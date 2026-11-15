package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.JsonParams;
import com.datura.origins.fabric.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class DamageMultiplierPower implements RacePower {
	private static boolean reentry;
	private final TagKey<net.minecraft.entity.damage.DamageType> tag;
	private final float multiplier;
	private final java.util.List<String> types;

	public DamageMultiplierPower(JsonObject json) {
		this.tag = TagKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of(JsonParams.str(json, "damage_tag", "minecraft:is_fire")));
		this.multiplier = JsonParams.num(json, "multiplier", 2f);
		this.types = JsonParams.strings(json, "damage_types");
	}

	@Override
	public boolean allowDamage(ServerPlayerEntity player, DamageSource source, float amount) {
		if (reentry) return true;
		boolean match = types.isEmpty() ? source.isIn(tag)
				: source.getTypeRegistryEntry().getKey().map(k -> types.contains(k.getValue().toString())).orElse(false);
		if (!match) return true;
		reentry = true;
		try {
			player.damage(source, amount * multiplier);
		} finally {
			reentry = false;
		}
		return false;
	}
}
