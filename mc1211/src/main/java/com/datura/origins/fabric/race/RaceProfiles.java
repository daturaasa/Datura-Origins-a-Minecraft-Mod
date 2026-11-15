package com.datura.origins.fabric.race;

import com.datura.origins.race.RaceRegistry;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class RaceProfiles {
	private static final Map<String, RaceProfile> PROFILES = new ConcurrentHashMap<>();
	private static final Map<String, RaceProfile> NETHER = new ConcurrentHashMap<>();

	private RaceProfiles() {
	}

	public static void register(RaceProfile profile) {
		PROFILES.put(profile.id(), profile);
	}

	public static void registerNetherForm(RaceProfile profile) {
		NETHER.put(profile.id(), profile);
	}

	public static Optional<RaceProfile> netherForm(String id) {
		return id == null ? Optional.empty() : Optional.ofNullable(NETHER.get(id));
	}

	public static Optional<RaceProfile> get(String id) {
		if (id == null) {
			return Optional.empty();
		}
		RaceProfile profile = PROFILES.get(id);
		if (profile != null) {
			return Optional.of(profile);
		}
		return RaceRegistry.get(id).map(race -> PROFILES.computeIfAbsent(id, key -> RaceProfile.of(race)));
	}
}
