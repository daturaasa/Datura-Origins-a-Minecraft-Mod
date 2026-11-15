package com.datura.origins.fabric.power.ability;

import com.datura.origins.fabric.power.Ability;
import com.datura.origins.fabric.power.JsonParams;
import com.datura.origins.fabric.power.types.MaturityPower;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class StompAbility implements Ability {
	private record Pending(StompAbility ability, ServerPlayerEntity player, long fireAt) {
	}

	private static final Map<UUID, Pending> PENDING = new HashMap<>();
	private static long serverTicks;

	private final double radius;
	private final float damage;
	private final double launch;
	private final int windup;
	private final float[] maturityDamage;

	public StompAbility(JsonObject json) {
		this.radius = JsonParams.num(json, "radius", 4.0F);
		this.damage = JsonParams.num(json, "damage", 8.0F);
		this.launch = JsonParams.num(json, "launch", 0.9F);
		this.windup = JsonParams.integer(json, "windup", 9);
		this.maturityDamage = JsonParams.floats(json, "maturity_damage");
	}

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(StompAbility::tickAll);
	}

	@Override
	public boolean activate(ServerPlayerEntity player) {
		if (PENDING.containsKey(player.getUuid())) return false;
		PENDING.put(player.getUuid(), new Pending(this, player, serverTicks + windup));
		LegendKit.anim(player, "stomp", windup + 8);
		LegendKit.snd(player, SoundEvents.ENTITY_RAVAGER_STEP, 0.9F, 0.6F);
		return true;
	}

	@Override
	public void onRemove(ServerPlayerEntity player) {
		PENDING.remove(player.getUuid());
	}

	private static void tickAll(MinecraftServer server) {
		serverTicks++;
		if (PENDING.isEmpty()) return;
		Iterator<Pending> it = PENDING.values().iterator();
		while (it.hasNext()) {
			Pending p = it.next();
			if (p.player.isRemoved() || !p.player.isAlive()) {
				it.remove();
			} else if (serverTicks >= p.fireAt) {
				it.remove();
				p.ability.impact(p.player);
			}
		}
	}

	private void impact(ServerPlayerEntity owner) {
		ServerWorld world = owner.getServerWorld();
		Vec3d at = owner.getPos();
		double r = radius * Math.max(1.0, owner.getScale() * 0.4);
		float hit = damage * MaturityPower.multiplier(owner, maturityDamage);
		for (Entity e : world.getOtherEntities(owner, new Box(at, at).expand(r, 2.5, r))) {
			if (!(e instanceof LivingEntity target) || !target.isAlive()) continue;
			double d = Math.sqrt(e.squaredDistanceTo(at));
			if (d > r) continue;
			float falloff = (float) (1.0 - 0.5 * d / r);
			target.damage(owner.getDamageSources().playerAttack(owner), hit * falloff);
			Vec3d push = e.getPos().subtract(at).multiply(1, 0, 1);
			push = push.lengthSquared() < 1.0E-4 ? Vec3d.ZERO : push.normalize().multiply(0.35 * falloff);
			double resist = 1.0 - target.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE) * 0.5;
			target.addVelocity(push.x * resist, launch * falloff * resist, push.z * resist);
			target.velocityModified = true;
		}
		debris(world, owner, at, r);
		LegendKit.sndAt(world, at, SoundEvents.ENTITY_GENERIC_EXPLODE, 0.6F, 1.3F);
		LegendKit.sndAt(world, at, SoundEvents.BLOCK_ANVIL_LAND, 0.35F, 0.5F);
		LegendKit.sndAt(world, at, SoundEvents.ENTITY_IRON_GOLEM_ATTACK, 0.9F, 0.6F);
	}

	private static void debris(ServerWorld world, ServerPlayerEntity owner, Vec3d at, double r) {
		BlockState ground = world.getBlockState(BlockPos.ofFloored(at.x, at.y - 0.2, at.z));
		if (!ground.isAir()) {
			world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, ground), at.x, at.y + 0.2, at.z,
					(int) Math.round(r * 18), r * 0.45, 0.15, r * 0.45, 0.25);
		}
		int ring = (int) Math.round(r * 8);
		for (int i = 0; i < ring; i++) {
			double a = i * Math.PI * 2 / ring;
			world.spawnParticles(ParticleTypes.POOF, at.x + Math.cos(a) * r * 0.8, at.y + 0.1, at.z + Math.sin(a) * r * 0.8,
					1, 0.05, 0.05, 0.05, 0.03);
		}
		world.spawnParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.3, at.z, 2, 0.5, 0.1, 0.5, 0);
	}
}
