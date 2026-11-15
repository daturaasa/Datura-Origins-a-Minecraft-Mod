package com.datura.origins.fabric.power;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public interface RacePower {
		default void onApply(ServerPlayerEntity player) {}

		default void onRemove(ServerPlayerEntity player) {}

		default void tick(ServerPlayerEntity player) {}

		default void onKill(ServerPlayerEntity player, LivingEntity victim) {}

		default boolean allowDamage(ServerPlayerEntity player, DamageSource source, float amount) {
		return true;
	}

		@Nullable
	default Text consumeRejection(PlayerEntity player, ItemStack stack) {
		return null;
	}

		@Nullable
	default Text useRejection(PlayerEntity player, ItemStack stack) {
		return null;
	}
}
