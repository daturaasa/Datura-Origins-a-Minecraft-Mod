package com.datura.origins.fabric.race;

import com.datura.origins.fabric.OriginData;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RaceAccess {
	public static volatile String clientRaceId = "";
	public static volatile UUID clientPlayerId;
	public static final Set<UUID> IN_NETHER = ConcurrentHashMap.newKeySet();

	private RaceAccess() {
	}

	public static Optional<RaceProfile> getRace(PlayerEntity player) {
		if (player.getWorld().isClient) {
			return player.getUuid().equals(clientPlayerId) ? RaceProfiles.get(clientRaceId) : Optional.empty();
		}
		String id = player.getAttached(OriginData.ORIGIN);
		if (IN_NETHER.contains(player.getUuid())) {
			Optional<RaceProfile> weak = RaceProfiles.netherForm(id);
			if (weak.isPresent()) return weak;
		}
		return RaceProfiles.get(id);
	}

	public static Optional<RaceProfile> baseRace(PlayerEntity player) {
		return RaceProfiles.get(player.getAttached(OriginData.ORIGIN));
	}
}
