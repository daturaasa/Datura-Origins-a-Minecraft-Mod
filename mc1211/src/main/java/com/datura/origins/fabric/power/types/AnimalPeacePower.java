package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.RacePower;
import com.datura.origins.fabric.race.RaceAccess;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;

public final class AnimalPeacePower implements RacePower {
	public AnimalPeacePower(JsonObject json) {
	}

	public static void bootstrap() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim, source, amount) ->
				!(source.getAttacker() instanceof AnimalEntity && has(victim)));
	}

	public static boolean has(LivingEntity entity) {
		return entity instanceof PlayerEntity player
				&& RaceAccess.getRace(player).map(r -> r.powers().stream().anyMatch(p -> p instanceof AnimalPeacePower)).orElse(false);
	}

	public static boolean blocks(LivingEntity attacker, LivingEntity target) {
		return attacker instanceof AnimalEntity && has(target);
	}
}
