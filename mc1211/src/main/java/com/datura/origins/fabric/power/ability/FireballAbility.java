package com.datura.origins.fabric.power.ability;

import com.datura.origins.fabric.network.AnimPayload;
import com.datura.origins.fabric.network.FireballPayload;
import com.datura.origins.fabric.network.Networking;
import com.datura.origins.fabric.power.Ability;
import com.datura.origins.fabric.power.JsonParams;
import com.datura.origins.fabric.power.types.MaturityPower;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class FireballAbility implements Ability {
	private final double speed;
	private final int life;
	private final float explosionPower;
	private final float blockPower;
	private final int fireCount;
	private final int fireRadius;
	private final float[] maturityDamage;

	public FireballAbility(JsonObject json) {
		this.speed = JsonParams.num(json, "speed", 1.6f);
		this.life = JsonParams.integer(json, "life", 50);
		this.explosionPower = JsonParams.num(json, "explosion_power", 4f);
		this.blockPower = JsonParams.num(json, "block_power", 1.5f);
		this.fireCount = JsonParams.integer(json, "fire_count", 10);
		this.fireRadius = JsonParams.integer(json, "fire_radius", 3);
		this.maturityDamage = JsonParams.floats(json, "maturity_damage");
		this.green = "green".equals(JsonParams.str(json, "palette", "red"));
	}

	private static final class Shot {
		final int id;
		final FireballAbility config;
		final ServerPlayerEntity owner;
		final ServerWorld world;
		Vec3d pos;
		final Vec3d vel;
		int age;

		Shot(int id, FireballAbility config, ServerPlayerEntity owner, ServerWorld world, Vec3d pos, Vec3d vel) {
			this.id = id;
			this.config = config;
			this.owner = owner;
			this.world = world;
			this.pos = pos;
			this.vel = vel;
		}
	}

	private static final List<Shot> SHOTS = new ArrayList<>();
	private static int nextId = 1;
		private final boolean green;

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(FireballAbility::tickAll);
	}

	@Override
	public boolean activate(ServerPlayerEntity player) {
		if (BatFormState.isBat(player)) return false;
		if (!(player.getWorld() instanceof ServerWorld world)) return false;

		Vec3d look = player.getRotationVec(1f);
		Vec3d start = player.getEyePos().add(look.multiply(0.6 + player.getWidth() * 0.5));
		Vec3d vel = look.multiply(speed);
		Shot shot = new Shot(green ? -(nextId++) : nextId++, this, player, world, start, vel);
		SHOTS.add(shot);

		Networking.sendNear(world, start, 128,
				new FireballPayload(shot.id, start.x, start.y, start.z, vel.x, vel.y, vel.z, FireballPayload.FLYING));
		Networking.sendAround(new AnimPayload(player.getId(), "cast", 12), player);
		player.swingHand(Hand.MAIN_HAND, true);
		world.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.ENTITY_ENDER_DRAGON_SHOOT,
				SoundCategory.PLAYERS, 1.0f, 1.1f);
		world.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.ENTITY_BLAZE_SHOOT,
				SoundCategory.PLAYERS, 0.8f, 0.6f);
		return true;
	}

	private static void tickAll(MinecraftServer server) {
		if (SHOTS.isEmpty()) return;
		Iterator<Shot> it = SHOTS.iterator();
		while (it.hasNext()) {
			Shot s = it.next();
			if (!s.config.step(s)) it.remove();
		}
	}

		private boolean step(Shot s) {
		s.age++;
		BlockPos here = BlockPos.ofFloored(s.pos);
		if (!s.world.isChunkLoaded(here.getX() >> 4, here.getZ() >> 4)) return end(s, s.pos, false);

		Vec3d from = s.pos;
		Vec3d to = from.add(s.vel);
		BlockHitResult blockHit = s.world.raycast(new RaycastContext(from, to,
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, s.owner));
		Vec3d end = blockHit.getType() != HitResult.Type.MISS ? blockHit.getPos() : to;

		EntityHitResult entityHit = ProjectileUtil.getEntityCollision(s.world, s.owner, from, end,
				new Box(from, end).expand(1.0), e -> e != s.owner && e.isAlive() && e.canHit() && !e.isSpectator());

		if (entityHit != null) return end(s, entityHit.getPos(), true);
		if (blockHit.getType() != HitResult.Type.MISS) {
			boolean water = s.world.getFluidState(blockHit.getBlockPos()).isIn(FluidTags.WATER);
			Vec3d at = blockHit.getPos().subtract(s.vel.normalize().multiply(0.25));
			return end(s, at, !water);
		}
		s.pos = to;
		if (s.age >= life) return end(s, s.pos, false);
		return true;
	}

		private boolean end(Shot s, Vec3d at, boolean explode) {
		Networking.sendNear(s.world, at, 128, new FireballPayload(s.id, at.x, at.y, at.z, s.vel.x, s.vel.y, s.vel.z,
				explode ? FireballPayload.EXPLODED : FireballPayload.FIZZLED));
		if (explode) explode(s, at);
		else s.world.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.8f, 1.0f);
		return false;
	}

	private void explode(Shot s, Vec3d at) {
		ServerWorld world = s.world;
		ServerPlayerEntity owner = s.owner;

		float mult = MaturityPower.multiplier(owner, maturityDamage);
		world.createExplosion(owner, null, new OwnerSafeBehavior(owner, mult), at.x, at.y, at.z,
				explosionPower, false, World.ExplosionSourceType.NONE);
		if (blockPower > 0) {
			world.createExplosion(owner, null, new BlocksOnlyBehavior(), at.x, at.y, at.z,
					blockPower, false, World.ExplosionSourceType.MOB);
		}
		DragonFire.burnArea(world, at, explosionPower * 1.2, owner, 3f * mult, 6);
		spreadFire(world, BlockPos.ofFloored(at));
		double r = explosionPower * 1.2;
		for (net.minecraft.entity.LivingEntity e : world.getEntitiesByClass(net.minecraft.entity.LivingEntity.class,
				new Box(at, at).expand(r), x -> x.isAlive() && x != owner)) {
			double d = e.getPos().distanceTo(at);
			if (d > r) continue;
			double k = 1 - d / r;
			Vec3d dir = flat(e.getPos().subtract(at));
			e.addVelocity(dir.x * 0.75 * k, 0.12 + 0.22 * k, dir.z * 0.75 * k);
			e.velocityModified = true;
		}

		world.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 0.5f);
	}

	private static Vec3d flat(Vec3d v) {
		Vec3d f = new Vec3d(v.x, 0, v.z);
		return f.lengthSquared() < 1e-6 ? new Vec3d(0, 0, 1) : f.normalize();
	}

	private void spreadFire(ServerWorld world, BlockPos center) {
		int placed = 0;
		for (int attempt = 0; attempt < fireCount * 3 && placed < fireCount; attempt++) {
			int dx = world.random.nextInt(fireRadius * 2 + 1) - fireRadius;
			int dz = world.random.nextInt(fireRadius * 2 + 1) - fireRadius;
			for (int dy = 2; dy >= -3; dy--) {
				BlockPos pos = center.add(dx, dy, dz);
				if (world.getBlockState(pos).isAir() && AbstractFireBlock.canPlaceAt(world, pos, Direction.UP)) {
					world.setBlockState(pos, AbstractFireBlock.getState(world, pos));
					placed++;
					break;
				}
			}
		}
	}

		private static final class OwnerSafeBehavior extends ExplosionBehavior {
		private final Entity owner;
		private final float damageMultiplier;

		OwnerSafeBehavior(Entity owner, float damageMultiplier) {
			this.owner = owner;
			this.damageMultiplier = damageMultiplier;
		}

		@Override
		public boolean shouldDamage(Explosion explosion, Entity entity) {
			return entity != owner && super.shouldDamage(explosion, entity);
		}

		@Override
		public float calculateDamage(Explosion explosion, Entity entity) {
			return super.calculateDamage(explosion, entity) * damageMultiplier;
		}

		@Override
		public float getKnockbackModifier(Entity entity) {
			return 0f;
		}
	}

		private static final class BlocksOnlyBehavior extends ExplosionBehavior {
		@Override
		public boolean shouldDamage(Explosion explosion, Entity entity) {
			return false;
		}

		@Override
		public float getKnockbackModifier(Entity entity) {
			return 0f;
		}
	}
}
