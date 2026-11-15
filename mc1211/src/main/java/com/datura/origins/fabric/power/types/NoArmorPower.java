package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.JsonParams;
import com.datura.origins.fabric.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class NoArmorPower implements RacePower {
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private final Text message;
	private boolean heavyOnly;
	private final java.util.EnumSet<EquipmentSlot> slots = java.util.EnumSet.noneOf(EquipmentSlot.class);

	public NoArmorPower(JsonObject json) {
		this.message = JsonParams.message(json, "message", "datura_origins.message.no_armor")
				.formatted(Formatting.DARK_RED);
		for (String s : JsonParams.strings(json, "slots")) {
			switch (s) {
				case "head" -> slots.add(EquipmentSlot.HEAD);
				case "chest" -> slots.add(EquipmentSlot.CHEST);
				case "legs" -> slots.add(EquipmentSlot.LEGS);
				case "feet" -> slots.add(EquipmentSlot.FEET);
				default -> {}
			}
		}
		if (slots.isEmpty()) slots.addAll(java.util.List.of(SLOTS));
		this.heavyOnly = JsonParams.bool(json, "heavy_only", false);
	}

	@Override
	public Text useRejection(PlayerEntity player, ItemStack stack) {
		return stack.getItem() instanceof ArmorItem a && slots.contains(a.getSlotType()) && (!heavyOnly || heavy(a)) ? message : null;
	}

		private static boolean heavy(ArmorItem a) {
		var m = a.getMaterial();
		return m != net.minecraft.item.ArmorMaterials.LEATHER && m != net.minecraft.item.ArmorMaterials.CHAIN
				&& m != net.minecraft.item.ArmorMaterials.GOLD && m != net.minecraft.item.ArmorMaterials.TURTLE;
	}

	@Override
	public void onApply(ServerPlayerEntity player) {
		strip(player);
	}

	@Override
	public void tick(ServerPlayerEntity player) {
		if (player.age % 10 == 0) strip(player);
	}

	private void strip(ServerPlayerEntity player) {
		if (player.isCreative() || player.isSpectator()) return;
		boolean removed = false;
		for (EquipmentSlot slot : slots) {
			ItemStack stack = player.getEquippedStack(slot);
			if (!stack.isEmpty() && stack.getItem() instanceof ArmorItem a && (!heavyOnly || heavy(a))) {
				player.equipStack(slot, ItemStack.EMPTY);
				player.getInventory().offerOrDrop(stack);
				removed = true;
			}
		}
		if (removed) player.sendMessage(message, true);
	}
}
