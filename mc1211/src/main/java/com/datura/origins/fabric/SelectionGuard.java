package com.datura.origins.fabric;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SelectionGuard {
	private static final Set<UUID> CHOOSING = ConcurrentHashMap.newKeySet();
	private static final double MOB_RANGE = 48.0;

	private SelectionGuard() {
	}

	static void init() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !CHOOSING.contains(entity.getUuid()));
		ServerTickEvents.END_SERVER_TICK.register(SelectionGuard::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> CHOOSING.remove(handler.getPlayer().getUuid()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> CHOOSING.clear());
	}

	public static void hold(ServerPlayerEntity player) {
		CHOOSING.add(player.getUuid());
		protect(player);
	}

	public static void release(ServerPlayerEntity player) {
		CHOOSING.remove(player.getUuid());
	}

	public static boolean isChoosing(ServerPlayerEntity player) {
		return CHOOSING.contains(player.getUuid());
	}

	private static void tick(MinecraftServer server) {
		if (CHOOSING.isEmpty()) {
			return;
		}
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (CHOOSING.contains(player.getUuid())) {
				protect(player);
			}
		}
	}

	private static void protect(ServerPlayerEntity player) {
		player.getHungerManager().setFoodLevel(20);
		player.getHungerManager().setSaturationLevel(5.0F);
		player.setAir(player.getMaxAir());
		player.extinguish();
		player.fallDistance = 0.0F;
		player.getServerWorld()
				.getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(MOB_RANGE), mob -> mob.getTarget() == player)
				.forEach(mob -> mob.setTarget(null));
	}
}
