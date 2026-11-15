package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.JsonParams;
import com.datura.origins.fabric.power.RacePower;
import com.datura.origins.fabric.race.RaceAccess;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class SleepAltitudePower implements RacePower {
	private final int minY;

	public SleepAltitudePower(JsonObject json) {
		this.minY = JsonParams.integer(json, "min_y", 100);
	}

	public static void bootstrap() {
		EntitySleepEvents.ALLOW_SLEEPING.register((player, pos) -> {
			if (player.getWorld().isClient) return null;
			SleepAltitudePower power = RaceAccess.getRace(player).flatMap(r -> r.powers().stream()
					.filter(p -> p instanceof SleepAltitudePower)
					.map(p -> (SleepAltitudePower) p)
					.findFirst()).orElse(null);
			if (power == null || pos.getY() >= power.minY) return null;
			player.sendMessage(Text.translatable("datura_origins.message.sleep_high", power.minY).formatted(Formatting.GRAY), true);
			return PlayerEntity.SleepFailureReason.OTHER_PROBLEM;
		});
	}
}
