package com.datura.origins.fabric.power.ability;

import net.minecraft.entity.player.PlayerEntity;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BatFormState {
		public static final Set<UUID> SERVER = ConcurrentHashMap.newKeySet();
		public static final Set<Integer> CLIENT = ConcurrentHashMap.newKeySet();

	private BatFormState() {}

	public static boolean isBat(PlayerEntity player) {
		return player.getWorld().isClient ? CLIENT.contains(player.getId()) : SERVER.contains(player.getUuid());
	}
}
