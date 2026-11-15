package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.JsonParams;
import com.datura.origins.fabric.power.RacePower;
import com.datura.origins.fabric.race.RaceAccess;
import com.google.gson.JsonObject;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

public final class WingFlightPower implements RacePower {
	private final float speed;
	private final float exhaustion;

	public WingFlightPower(JsonObject json) {
		this.speed = JsonParams.num(json, "speed", 1.25F);
		this.exhaustion = JsonParams.num(json, "exhaustion", 0.015F);
	}

	public static Optional<WingFlightPower> of(PlayerEntity player) {
		return RaceAccess.getRace(player).flatMap(r -> r.powers().stream()
				.filter(p -> p instanceof WingFlightPower)
				.map(p -> (WingFlightPower) p)
				.findFirst());
	}

	public static boolean canKeepFlying(PlayerEntity player) {
		return !player.isOnGround() && !player.hasVehicle() && !player.isTouchingWater() && !player.isClimbing()
				&& !player.hasStatusEffect(StatusEffects.LEVITATION) && !player.getAbilities().flying;
	}

	public float speed() {
		return speed;
	}

	@Override
	public void tick(ServerPlayerEntity player) {
		if (player.isFallFlying() && !player.isCreative()) player.addExhaustion(exhaustion);
	}

	@Override
	public void onRemove(ServerPlayerEntity player) {
		if (player.isFallFlying()) player.stopFallFlying();
	}
}
