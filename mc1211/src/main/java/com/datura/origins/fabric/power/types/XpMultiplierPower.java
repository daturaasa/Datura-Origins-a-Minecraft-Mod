package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.JsonParams;
import com.datura.origins.fabric.power.RacePower;
import com.datura.origins.fabric.race.RaceAccess;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;

public final class XpMultiplierPower implements RacePower {
	private final float factor;

	public XpMultiplierPower(JsonObject json) {
		this.factor = JsonParams.num(json, "factor", 1.5f);
	}

	public static float factor(PlayerEntity p) {
		float f = RaceAccess.getRace(p).flatMap(r -> r.powers().stream().filter(x -> x instanceof XpMultiplierPower)
				.map(x -> ((XpMultiplierPower) x).factor).findFirst()).orElse(1f);
		return f;
	}
}
