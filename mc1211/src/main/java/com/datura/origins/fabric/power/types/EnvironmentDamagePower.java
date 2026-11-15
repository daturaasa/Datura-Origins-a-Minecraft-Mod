package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.JsonParams;
import com.datura.origins.fabric.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

public final class EnvironmentDamagePower implements RacePower {
	private final String when;
	private final int interval;
	private final float damage;
	private final boolean ignite;

	public EnvironmentDamagePower(JsonObject json) {
		this.when = JsonParams.str(json, "when", "water");
		this.interval = Math.max(1, JsonParams.integer(json, "interval", 20));
		this.damage = JsonParams.num(json, "damage", 4.0F);
		this.ignite = JsonParams.bool(json, "ignite", false);
	}

	@Override
	public void tick(ServerPlayerEntity player) {
		if (player.age % interval != 0 || player.isSpectator() || player.isCreative()) return;
		boolean hit = switch (when) {
			case "nether" -> player.getWorld().getRegistryKey() == World.NETHER;
			default -> player.isTouchingWaterOrRain();
		};
		if (!hit) return;
		if (ignite) player.setOnFireFor(4.0F);
		else player.damage(player.getDamageSources().magic(), damage);
	}
}
