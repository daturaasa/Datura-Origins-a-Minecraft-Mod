package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.JsonParams;
import com.datura.origins.fabric.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

public final class PhotosynthesisPower implements RacePower {
	private final float heal;
	private final int healInterval;
	private final int foodInterval;

	public PhotosynthesisPower(JsonObject json) {
		this.heal = JsonParams.num(json, "heal", 1.0F);
		this.healInterval = Math.max(1, JsonParams.integer(json, "heal_interval", 40));
		this.foodInterval = Math.max(1, JsonParams.integer(json, "food_interval", 60));
	}

	public static boolean inSun(ServerPlayerEntity player) {
		World world = player.getWorld();
		return world.isDay() && !world.isRaining() && world.isSkyVisible(player.getBlockPos().up());
	}

	@Override
	public void tick(ServerPlayerEntity player) {
		if (!inSun(player)) return;
		if (player.age % healInterval == 0 && player.getHealth() < player.getMaxHealth()) player.heal(heal);
		if (player.age % foodInterval == 0) player.getHungerManager().add(1, 0.5F);
	}
}
