package com.datura.origins.race;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class RaceRegistry {
	private static final Map<String, Race> RACES = new LinkedHashMap<>();
	private static final List<Consumer<Race>> LISTENERS = new CopyOnWriteArrayList<>();

	private RaceRegistry() {
	}

	public static synchronized Race register(Race race) {
		if (RACES.putIfAbsent(race.id(), race) != null) {
			throw new IllegalStateException("Race already registered: " + race.id());
		}
		LISTENERS.forEach(listener -> listener.accept(race));
		return race;
	}

	public static synchronized Optional<Race> get(String id) {
		return Optional.ofNullable(RACES.get(id));
	}

	public static synchronized List<Race> all() {
		return List.copyOf(new ArrayList<>(RACES.values()));
	}

	public static synchronized List<Race> pages() {
		return RACES.values().stream().filter(race -> !race.isVariant()).toList();
	}

	public static synchronized List<Race> variantsOf(String baseId) {
		return RACES.values().stream().filter(race -> baseId.equals(race.variantOf())).toList();
	}

	public static synchronized List<Race> forms(Race base) {
		List<Race> forms = new ArrayList<>();
		forms.add(base);
		forms.addAll(variantsOf(base.id()));
		return forms;
	}

	public static Race baseOf(Race race) {
		return race.isVariant() ? get(race.variantOf()).orElse(race) : race;
	}

	public static synchronized List<String> ids() {
		return List.copyOf(RACES.keySet());
	}

	public static synchronized int indexOf(Race race) {
		int i = 0;
		for (String id : RACES.keySet()) {
			if (id.equals(race.id())) {
				return i;
			}
			i++;
		}
		return -1;
	}

	public static Race defaultRace() {
		return Races.HUMANO;
	}

	public static void onRegister(Consumer<Race> listener) {
		LISTENERS.add(listener);
	}
}
