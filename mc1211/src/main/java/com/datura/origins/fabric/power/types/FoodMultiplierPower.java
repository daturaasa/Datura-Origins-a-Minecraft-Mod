package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.JsonParams;
import com.datura.origins.fabric.power.RacePower;
import com.datura.origins.fabric.race.RaceAccess;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;

public final class FoodMultiplierPower implements RacePower {
	private final float factor;

	public FoodMultiplierPower(JsonObject json) {
		this.factor = JsonParams.num(json, "factor", 0.6F);
	}

	public static float factor(PlayerEntity player) {
		return RaceAccess.getRace(player).flatMap(r -> r.powers().stream()
				.filter(p -> p instanceof FoodMultiplierPower)
				.map(p -> ((FoodMultiplierPower) p).factor)
				.findFirst()).orElse(1.0F);
	}
}
