package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.JsonParams;
import com.datura.origins.fabric.power.RacePower;
import com.datura.origins.fabric.race.RaceAccess;
import com.google.gson.JsonObject;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public final class InvertedPotionPower implements RacePower {
	private final float damagePerLevel;

	public InvertedPotionPower(JsonObject json) {
		this.damagePerLevel = JsonParams.num(json, "damage_per_level", 2.0F);
	}

	public static boolean inverts(PlayerEntity player) {
		return RaceAccess.getRace(player).map(r -> r.powers().stream().anyMatch(p -> p instanceof InvertedPotionPower)).orElse(false);
	}

	@Override
	public void tick(ServerPlayerEntity player) {
		if (player.age % 10 != 0 || player.isCreative()) return;
		List<RegistryEntry<StatusEffect>> burn = new ArrayList<>();
		float damage = 0.0F;
		boolean cursed = false;
		for (StatusEffectInstance effect : player.getStatusEffects()) {
			if (effect.isAmbient()) continue;
			StatusEffectCategory category = effect.getEffectType().value().getCategory();
			if (category == StatusEffectCategory.BENEFICIAL) {
				burn.add(effect.getEffectType());
				damage += damagePerLevel * (effect.getAmplifier() + 1);
			} else if (category == StatusEffectCategory.HARMFUL) {
				cursed = true;
			}
		}
		for (RegistryEntry<StatusEffect> effect : burn) player.removeStatusEffect(effect);
		if (damage > 0.0F) player.damage(player.getDamageSources().magic(), damage);
		if (cursed) {
			StatusEffectInstance strength = player.getStatusEffect(StatusEffects.STRENGTH);
			if (strength == null || (strength.isAmbient() && strength.getDuration() < 30)) {
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 60, 0, true, false, true));
			}
		}
	}
}
