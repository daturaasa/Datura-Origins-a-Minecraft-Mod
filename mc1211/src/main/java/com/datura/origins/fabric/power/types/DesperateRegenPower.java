package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.JsonParams;
import com.datura.origins.fabric.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;

public final class DesperateRegenPower implements RacePower {
	private final float max;
	private final int interval;

	public DesperateRegenPower(JsonObject json) {
		this.max = JsonParams.num(json, "max", 1.5f);
		this.interval = Math.max(1, JsonParams.integer(json, "interval", 20));
	}

	@Override
	public void tick(ServerPlayerEntity p) {
		if (p.age % interval != 0 || !p.isAlive() || p.getHealth() >= p.getMaxHealth()) return;
		float missing = 1f - p.getHealth() / p.getMaxHealth();
		float heal = Math.min(max, max * 1.4f * (float) Math.pow(missing, 1.6));
		if (heal >= 0.05f) p.heal(heal);
		if (missing > 0.6f && p.age % (interval * 3) == 0) p.getServerWorld().spawnParticles(
				ParticleTypes.CRIMSON_SPORE, p.getX(), p.getBodyY(0.5), p.getZ(), 6, 0.3, 0.5, 0.3, 0.0);
	}
}
