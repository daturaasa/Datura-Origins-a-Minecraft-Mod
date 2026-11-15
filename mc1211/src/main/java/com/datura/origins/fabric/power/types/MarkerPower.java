package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.RacePower;
import com.datura.origins.fabric.race.RaceAccess;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;

public final class MarkerPower implements RacePower {
	private final String kind;

	public MarkerPower(String kind) {
		this.kind = kind;
	}

	public static MarkerPower noSwim(JsonObject json) {
		return new MarkerPower("no_swim");
	}

		public static MarkerPower generic(JsonObject json) {
		return new MarkerPower(com.datura.origins.fabric.power.JsonParams.str(json, "kind", ""));
	}

	public static MarkerPower snowWalker(JsonObject json) {
		return new MarkerPower("powder_snow_walker");
	}

	public static boolean has(PlayerEntity p, String kind) {
		return RaceAccess.getRace(p).map(r -> r.powers().stream()
				.anyMatch(x -> x instanceof MarkerPower m && m.kind.equals(kind))).orElse(false);
	}
}
