package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.JsonParams;
import com.datura.origins.fabric.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class EffectImmunityPower implements RacePower {
	private final List<RegistryEntry<StatusEffect>> effects = new ArrayList<>();
		private final boolean all;

	public EffectImmunityPower(JsonObject json) {
		this.all = JsonParams.bool(json, "all", false);
		for (String s : JsonParams.strings(json, "effects")) {
			Registries.STATUS_EFFECT.getEntry(Identifier.of(s)).ifPresent(effects::add);
		}
	}

	public static boolean blocks(PlayerEntity player, StatusEffectInstance effect) {
		return com.datura.origins.fabric.race.RaceAccess.getRace(player).map(r -> r.powers().stream()
				.anyMatch(p -> p instanceof EffectImmunityPower e
						&& (e.all ? !effect.isAmbient() : e.effects.contains(effect.getEffectType())))).orElse(false);
	}
}
