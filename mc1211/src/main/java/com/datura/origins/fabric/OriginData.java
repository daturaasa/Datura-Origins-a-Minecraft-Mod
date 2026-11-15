package com.datura.origins.fabric;

import com.datura.origins.DaturaOrigins;
import com.datura.origins.fabric.network.OpenSelectionPayload;
import com.datura.origins.fabric.network.SyncOriginPayload;
import com.datura.origins.fabric.race.RaceManager;
import com.datura.origins.race.Race;
import com.datura.origins.race.RaceRegistry;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OriginData {
	public static final AttachmentType<String> ORIGIN = AttachmentRegistry.<String>builder()
			.persistent(Codec.STRING)
			.copyOnDeath()
			.buildAndRegister(Identifier.of(DaturaOrigins.MOD_ID, "origin"));

	public static final Set<UUID> NO_COOLDOWN = ConcurrentHashMap.newKeySet();

	private OriginData() {
	}

	static void init() {
	}

	public static boolean hasChosen(PlayerEntity player) {
		return player.hasAttached(ORIGIN);
	}

	public static Optional<Race> get(PlayerEntity player) {
		String id = player.getAttached(ORIGIN);
		return id == null ? Optional.empty() : RaceRegistry.get(id);
	}

	public static void set(ServerPlayerEntity player, Race race) {
		RaceManager.clear(player);
		player.setAttached(ORIGIN, race.id());
		RaceManager.apply(player);
		SelectionGuard.release(player);
		sync(player);
		player.sendMessage(Text.translatable("datura_origins.message.awakened",
				Text.translatableWithFallback(race.name(), race.name()).formatted(Formatting.GOLD)).formatted(Formatting.GRAY), false);
	}

	public static void restartSelection(ServerPlayerEntity player) {
		RaceManager.clear(player);
		player.removeAttached(ORIGIN);
		sync(player);
		requestSelection(player);
	}

	public static void requestSelection(ServerPlayerEntity player) {
		if (ServerPlayNetworking.canSend(player, OpenSelectionPayload.ID)) {
			SelectionGuard.hold(player);
			ServerPlayNetworking.send(player, OpenSelectionPayload.INSTANCE);
		} else {
			player.setAttached(ORIGIN, RaceRegistry.defaultRace().id());
			RaceManager.apply(player);
		}
	}

	public static void sync(ServerPlayerEntity player) {
		if (ServerPlayNetworking.canSend(player, SyncOriginPayload.ID)) {
			String id = player.getAttached(ORIGIN);
			ServerPlayNetworking.send(player, new SyncOriginPayload(id == null ? "" : id));
		}
	}
}
