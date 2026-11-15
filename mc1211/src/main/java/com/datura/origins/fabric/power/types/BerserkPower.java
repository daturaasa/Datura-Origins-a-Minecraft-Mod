package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.EntityFlags;
import com.datura.origins.fabric.power.JsonParams;
import com.datura.origins.fabric.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public final class BerserkPower implements RacePower {
	public static final String FLAG = "berserk";

	private final float threshold;
	private final float release;
	private final int strength;
	private final int regen;

	public BerserkPower(JsonObject json) {
		this.threshold = JsonParams.num(json, "threshold", 8.0F);
		this.release = JsonParams.num(json, "release", 12.0F);
		this.strength = JsonParams.integer(json, "strength", 1);
		this.regen = JsonParams.integer(json, "regen", 1);
	}

	@Override
	public void tick(ServerPlayerEntity p) {
		if (p.age % 5 != 0 || !p.isAlive()) return;
		boolean on = EntityFlags.has(p, FLAG);
		if (!on && p.getHealth() <= threshold) {
			EntityFlags.set(p, FLAG, true);
			on = true;
			p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.PLAYERS, 0.55F, 1.15F);
			p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 1.0F, 0.5F);
		} else if (on && p.getHealth() >= release) {
			EntityFlags.set(p, FLAG, false);
			on = false;
		}
		if (!on) return;
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 30, strength, true, false, true));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 30, regen, true, false, true));
		double yaw = Math.toRadians(p.getYaw());
		double nx = p.getX() - Math.sin(yaw) * 0.45, nz = p.getZ() + Math.cos(yaw) * 0.45;
		p.getServerWorld().spawnParticles(ParticleTypes.CLOUD, nx, p.getEyeY() - 0.25, nz, 2, 0.05, 0.02, 0.05, 0.01);
		if (p.age % 40 == 0) {
			p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 0.7F, 0.45F);
		}
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		EntityFlags.set(p, FLAG, false);
	}
}
