package com.datura.origins.fabric.client;

import com.datura.origins.fabric.race.RaceAccess;
import com.datura.origins.race.Race;
import com.datura.origins.race.RaceRegistry;

import java.util.Optional;

public final class ClientOrigin {
	private static String current = "";

	private ClientOrigin() {
	}

	public static Optional<Race> get() {
		return current.isEmpty() ? Optional.empty() : RaceRegistry.get(current);
	}

	public static boolean hasChosen() {
		return !current.isEmpty();
	}

	static void set(String id) {
		current = id == null ? "" : id;
		RaceAccess.clientRaceId = current;
	}
}
