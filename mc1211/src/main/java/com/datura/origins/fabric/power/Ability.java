package com.datura.origins.fabric.power;

import net.minecraft.server.network.ServerPlayerEntity;

public interface Ability {
		boolean activate(ServerPlayerEntity player);

		default void onRemove(ServerPlayerEntity player) {}

		default boolean chargeable() {
		return false;
	}

		default void holdStart(ServerPlayerEntity player) {}

		default boolean release(ServerPlayerEntity player, int heldTicks) {
		return activate(player);
	}

		default void scroll(ServerPlayerEntity player, int delta) {}

		default void attackKey(ServerPlayerEntity player) {}

		default void choose(ServerPlayerEntity player, int option) {}
}
