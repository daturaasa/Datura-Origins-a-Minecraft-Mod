package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.EntityFlags;
import com.datura.origins.fabric.power.JsonParams;
import com.datura.origins.fabric.power.RacePower;
import com.datura.origins.fabric.power.ability.LegendKit;
import com.google.gson.JsonObject;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AquaticPower implements RacePower {
	public static final String WET = "sea_wet";
	public static final String DRY = "sea_dry";
	private static final Map<UUID, Integer> DRY_TICKS = new ConcurrentHashMap<>();

	private final int dryTicks;
	private final boolean nightVision;

	public AquaticPower(JsonObject json) {
		this.dryTicks = Math.round(JsonParams.num(json, "dry_seconds", 90.0F) * 20);
		this.nightVision = JsonParams.bool(json, "night_vision", false);
	}

	@Override
	public void tick(ServerPlayerEntity p) {
		boolean water = p.isTouchingWater();
		int dry = p.isTouchingWaterOrRain() ? 0 : DRY_TICKS.getOrDefault(p.getUuid(), 0) + 1;
		DRY_TICKS.put(p.getUuid(), dry);
		if (EntityFlags.has(p, WET) != water) {
			EntityFlags.set(p, WET, water);
			if (water) {
				LegendKit.snd(p, SoundEvents.ENTITY_DOLPHIN_SPLASH, 0.6F, 1.3F);
				p.getServerWorld().spawnParticles(ParticleTypes.BUBBLE_POP, p.getX(), p.getBodyY(0.4), p.getZ(), 16, 0.4, 0.4, 0.4, 0.05);
			}
		}
		if (water && p.age % 20 == 0) {
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 60, 0, true, false, true));
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.CONDUIT_POWER, 260, 0, true, false, true));
			if (nightVision && p.isSubmergedInWater()) {
				p.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 320, 0, true, false, false));
			}
		}
		boolean dried = dry > dryTicks;
		if (EntityFlags.has(p, DRY) != dried) {
			EntityFlags.set(p, DRY, dried);
			if (dried) LegendKit.snd(p, SoundEvents.ENTITY_DOLPHIN_HURT, 0.5F, 0.8F);
		}
		if (dried && p.age % 20 == 0) {
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0, true, false, true));
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 40, 0, true, false, true));
		}
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		DRY_TICKS.remove(p.getUuid());
		EntityFlags.set(p, WET, false);
		EntityFlags.set(p, DRY, false);
	}
}
