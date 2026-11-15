package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.JsonParams;
import com.datura.origins.fabric.power.RacePower;
import com.datura.origins.fabric.power.ability.LegendKit;
import com.datura.origins.fabric.race.RaceAccess;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;

import java.util.Optional;

public final class MountainLeapPower implements RacePower {
	private final int maxCharge;
	private final float maxHeight;
	private final float forward;

	public MountainLeapPower(JsonObject json) {
		this.maxCharge = Math.max(5, JsonParams.integer(json, "max_charge", 40));
		this.maxHeight = JsonParams.num(json, "max_height", 30.0F);
		this.forward = JsonParams.num(json, "forward", 0.6F);
	}

	public static Optional<MountainLeapPower> of(PlayerEntity player) {
		return RaceAccess.getRace(player).flatMap(r -> r.powers().stream()
				.filter(p -> p instanceof MountainLeapPower)
				.map(p -> (MountainLeapPower) p)
				.findFirst());
	}

	public int maxCharge() {
		return maxCharge;
	}

	public float maxHeight() {
		return maxHeight;
	}

	public float forward() {
		return forward;
	}

	public void charging(ServerPlayerEntity player, boolean start) {
		LegendKit.anim(player, "crouch", start ? maxCharge + 40 : 0);
	}

	public void leap(ServerPlayerEntity player, int charge) {
		if (charge < 5) {
			LegendKit.anim(player, "crouch", 0);
			return;
		}
		float c = Math.min(1.0F, charge / (float) maxCharge);
		player.fallDistance = 0;
		LegendKit.anim(player, "crouch", 0);
		player.getServerWorld().spawnParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.1, player.getZ(),
				6 + Math.round(c * 20), 0.4, 0.05, 0.4, 0.04);
		LegendKit.snd(player, SoundEvents.ENTITY_GOAT_LONG_JUMP, 0.9F, 1.1F - 0.25F * c);
		if (c > 0.7F) LegendKit.snd(player, SoundEvents.ENTITY_BREEZE_JUMP, 0.4F, 0.8F);
	}
}
