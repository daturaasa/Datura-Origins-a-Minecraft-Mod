package com.datura.origins.fabric.power;

import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;

public abstract class ConditionalPower implements RacePower {
	protected final boolean requiresFood;

	protected ConditionalPower(JsonObject json) {
		this.requiresFood = JsonParams.bool(json, "requires_food", false);
	}

	protected boolean isActive(PlayerEntity player) {
		return !requiresFood || player.getHungerManager().getFoodLevel() > 0;
	}
}
