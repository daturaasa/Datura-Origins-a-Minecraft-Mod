package com.datura.origins.fabric.power.ability;

import com.datura.origins.fabric.power.types.MarkerPower;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AreaZones {
	public static final String SPORE_FRIEND = "toxic_affinity";
	private static final DustParticleEffect SPORE = new DustParticleEffect(new Vector3f(0.62F, 0.22F, 0.86F), 1.6F);
	private static final DustParticleEffect SPORE_PALE = new DustParticleEffect(new Vector3f(0.86F, 0.55F, 0.95F), 1.1F);
	private static final List<Zone> ZONES = new ArrayList<>();

	private static final class Zone {
		final ServerWorld world;
		final Vec3d center;
		final float maxRadius;
		final int growTicks;
		final int life;
		final UUID owner;
		final float damage;
		final List<BlockPos> blocks;
		int age;

		Zone(ServerWorld world, Vec3d center, float maxRadius, int growTicks, int life, UUID owner, float damage, List<BlockPos> blocks) {
			this.world = world;
			this.center = center;
			this.maxRadius = maxRadius;
			this.growTicks = Math.max(1, growTicks);
			this.life = life;
			this.owner = owner;
			this.damage = damage;
			this.blocks = blocks;
		}

		float radius() {
			return maxRadius * Math.min(1.0F, (age + 1) / (float) growTicks);
		}
	}

	private AreaZones() {
	}

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(AreaZones::tick);
	}

	public static void create(ServerWorld world, Vec3d center, float radius, int growTicks, int life, Entity owner, float damage, List<BlockPos> blocks) {
		ZONES.add(new Zone(world, center, radius, growTicks, life, owner != null ? owner.getUuid() : null, damage, blocks));
	}

	public static DamageSource poison(ServerWorld world, Entity attacker) {
		return new DamageSource(world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(DamageTypes.MAGIC), attacker);
	}

	public static boolean friend(Entity entity) {
		return entity instanceof PlayerEntity player && MarkerPower.has(player, SPORE_FRIEND);
	}

	private static void tick(MinecraftServer server) {
		if (ZONES.isEmpty()) return;
		for (Zone zone : new ArrayList<>(ZONES)) {
			zone.age++;
			if (zone.age > zone.life || zone.world.getServer() != server) {
				end(zone);
				continue;
			}
			if (zone.age % 5 == 0) particles(zone);
			if (zone.age % 10 != 0) continue;
			float r = zone.radius();
			Entity owner = zone.owner != null ? zone.world.getEntity(zone.owner) : null;
			Box box = new Box(zone.center, zone.center).expand(r, 2.5, r);
			for (LivingEntity e : zone.world.getEntitiesByClass(LivingEntity.class, box, LivingEntity::isAlive)) {
				double dx = e.getX() - zone.center.x, dz = e.getZ() - zone.center.z;
				if (dx * dx + dz * dz > r * r) continue;
				affect(zone, e, owner);
			}
			if (zone.age % 40 == 0) {
				zone.world.playSound(null, zone.center.x, zone.center.y, zone.center.z, SoundEvents.BLOCK_BREWING_STAND_BREW,
						SoundCategory.PLAYERS, 0.6F, 0.6F + zone.world.random.nextFloat() * 0.3F);
			}
		}
	}

	private static void affect(Zone zone, LivingEntity e, Entity owner) {
		if (friend(e)) {
			if (e.getAbsorptionAmount() < 4.0F) e.setAbsorptionAmount(4.0F);
			if (zone.age % 20 == 0) e.heal(1.0F);
			return;
		}
		if (e == owner || (e instanceof Tameable pet && owner != null && owner.getUuid().equals(pet.getOwnerUuid()))) return;
		if (zone.age % 20 == 0 && zone.damage > 0) {
			e.timeUntilRegen = 0;
			e.damage(poison(zone.world, owner), zone.damage);
		}
		e.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 60, 1, true, true, true));
		e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 60, 0, true, false, true));
		e.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 100, 0, true, false, true));
		e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 40, 0, true, false, false));
	}

	private static void particles(Zone zone) {
		float r = zone.radius();
		ServerWorld w = zone.world;
		int count = MathHelper.clamp((int) (r * r * 0.6F), 4, 60);
		for (int i = 0; i < count; i++) {
			double a = w.random.nextDouble() * Math.PI * 2, d = Math.sqrt(w.random.nextDouble()) * r;
			double x = zone.center.x + Math.cos(a) * d, z = zone.center.z + Math.sin(a) * d;
			w.spawnParticles(i % 3 == 0 ? SPORE_PALE : SPORE, x, zone.center.y + 0.15 + w.random.nextDouble() * 0.6, z, 1, 0.1, 0.05, 0.1, 0);
		}
		w.spawnParticles(ParticleTypes.WITCH, zone.center.x, zone.center.y + 0.9, zone.center.z, 2, 0.2, 0.15, 0.2, 0.02);
	}

	private static void end(Zone zone) {
		ZONES.remove(zone);
		for (BlockPos pos : zone.blocks) TempBlocks.remove(zone.world, pos);
	}
}
