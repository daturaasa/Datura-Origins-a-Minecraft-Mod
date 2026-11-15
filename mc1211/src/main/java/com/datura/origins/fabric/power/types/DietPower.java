package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.JsonParams;
import com.datura.origins.fabric.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class DietPower implements RacePower {
	private final List<Identifier> allowedItems = new ArrayList<>();
	private final List<TagKey<Item>> allowedTags = new ArrayList<>();
		private final List<Identifier> forbiddenItems = new ArrayList<>();
	private final List<TagKey<Item>> forbiddenTags = new ArrayList<>();
	private final Text message;

	public DietPower(JsonObject json) {
		for (String s : JsonParams.strings(json, "allowed")) {
			if (s.startsWith("#")) allowedTags.add(TagKey.of(RegistryKeys.ITEM, Identifier.of(s.substring(1))));
			else allowedItems.add(Identifier.of(s));
		}
		for (String s : JsonParams.strings(json, "forbidden")) {
			if (s.startsWith("#")) forbiddenTags.add(TagKey.of(RegistryKeys.ITEM, Identifier.of(s.substring(1))));
			else forbiddenItems.add(Identifier.of(s));
		}
		this.message = JsonParams.message(json, "message", "datura_origins.message.diet_default")
				.formatted(Formatting.DARK_RED);
	}

	@Override
	public Text consumeRejection(PlayerEntity player, ItemStack stack) {
		if (!forbiddenItems.isEmpty() || !forbiddenTags.isEmpty()) {
			boolean bad = forbiddenItems.contains(Registries.ITEM.getId(stack.getItem()));
			for (TagKey<Item> tag : forbiddenTags) bad |= stack.isIn(tag);
			return bad ? message : null;
		}
		if (allowedItems.contains(Registries.ITEM.getId(stack.getItem()))) return null;
		for (TagKey<Item> tag : allowedTags) if (stack.isIn(tag)) return null;
		return message;
	}
}
