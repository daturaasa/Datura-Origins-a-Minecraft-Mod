package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.server.network.ServerPlayerEntity;

public final class NoHungerPower implements RacePower {
	public NoHungerPower(JsonObject json) {
	}

	@Override
	public void tick(ServerPlayerEntity player) {
		if (player.age % 10 != 0) return;
		HungerManager hunger = player.getHungerManager();
		if (hunger.getFoodLevel() < 18) hunger.setFoodLevel(18);
		hunger.setExhaustion(0.0F);
	}
}
