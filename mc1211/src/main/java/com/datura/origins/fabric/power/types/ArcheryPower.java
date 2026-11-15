package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.JsonParams;
import com.datura.origins.fabric.power.RacePower;
import com.datura.origins.fabric.race.RaceAccess;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.DamageTypeTags;

public final class ArcheryPower implements RacePower {
	private static boolean reentry;
	private final float multiplier;

	public ArcheryPower(JsonObject json) {
		this.multiplier = JsonParams.num(json, "multiplier", 1.5f);
	}

	public static void bootstrap() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((target, source, amount) -> {
			if (reentry || !source.isIn(DamageTypeTags.IS_PROJECTILE) || !(source.getAttacker() instanceof PlayerEntity p)) return true;
			float m = RaceAccess.getRace(p).flatMap(r -> r.powers().stream().filter(x -> x instanceof ArcheryPower)
					.map(x -> ((ArcheryPower) x).multiplier).findFirst()).orElse(1f);
			if (m == 1f) return true;
			reentry = true;
			try {
				target.damage(source, amount * m);
			} finally {
				reentry = false;
			}
			return false;
		});
	}
}
