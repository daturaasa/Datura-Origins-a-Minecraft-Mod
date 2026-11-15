package com.datura.origins.fabric.race;

import com.datura.origins.fabric.DaturaOriginsMod;
import com.datura.origins.fabric.power.Ability;
import com.datura.origins.fabric.power.JsonParams;
import com.datura.origins.fabric.power.PowerRegistry;
import com.datura.origins.fabric.power.RacePower;
import com.datura.origins.race.Race;
import com.datura.origins.race.RaceRegistry;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.util.Identifier;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public final class RaceLoader {
	public static final String FOLDER = "datura_races";
	private static final Set<String> NETHER_KEEP = Set.of("maturity", "restricted_diet", "damage_multiplier", "environment_damage",
			"inverted_potions", "nature_oath", "sunlight_damage", "translucent");

	private record Entry(int order, String modId, JsonObject json) {
	}

	private RaceLoader() {
	}

	public static void load() {
		List<Entry> entries = new ArrayList<>();
		for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
			Optional<Path> dir = mod.findPath(FOLDER);
			if (dir.isEmpty() || !Files.isDirectory(dir.get())) continue;
			String modId = mod.getMetadata().getId();
			try (Stream<Path> files = Files.list(dir.get())) {
				for (Path file : files.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
					try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
						JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
						entries.add(new Entry(JsonParams.integer(json, "order", 99), modId, json));
					} catch (Exception e) {
						DaturaOriginsMod.LOGGER.error("Failed to read race {} ({}): {}", file.getFileName(), modId, e.getMessage());
					}
				}
			} catch (Exception e) {
				DaturaOriginsMod.LOGGER.error("Failed to list races of mod {}", modId, e);
			}
		}
		entries.sort(Comparator.comparingInt(Entry::order));
		for (Entry entry : entries) {
			try {
				register(entry.modId, entry.json);
			} catch (Exception e) {
				DaturaOriginsMod.LOGGER.error("Failed to register race from {}: {}", entry.modId, e.getMessage());
			}
		}
	}

	private static String fullId(String modId, String id) {
		id = id.toLowerCase(Locale.ROOT);
		return id.indexOf(':') > 0 ? id : modId + ":" + id;
	}

	private static void register(String modId, JsonObject o) {
		String id = fullId(modId, o.get("id").getAsString());
		Race.Builder builder = Race.builder(id)
				.name(o.get("name").getAsString())
				.description(JsonParams.str(o, "description", ""));
		for (String model : JsonParams.strings(o, "model")) {
			builder.model(fullId(modId, model));
		}
		if (o.has("color")) {
			builder.color(Integer.parseInt(o.get("color").getAsString().replace("#", ""), 16));
		}
		JsonParams.strings(o, "advantages").forEach(builder::advantage);
		JsonParams.strings(o, "disadvantages").forEach(builder::disadvantage);
		JsonParams.strings(o, "passives").forEach(builder::passive);
		if (o.has("variant_of")) {
			builder.variantOf(fullId(modId, o.get("variant_of").getAsString()), JsonParams.str(o, "variant_name", ""));
		} else if (o.has("variant_name")) {
			builder.variantName(o.get("variant_name").getAsString());
		}

		List<RacePower> powers = new ArrayList<>();
		List<RacePower> kept = new ArrayList<>();
		if (o.has("powers")) {
			for (JsonElement e : o.getAsJsonArray("powers")) {
				JsonObject pj = e.getAsJsonObject();
				Identifier type = Identifier.of(pj.get("type").getAsString());
				PowerRegistry.PowerFactory factory = PowerRegistry.powerType(type);
				if (factory == null) {
					DaturaOriginsMod.LOGGER.error("Race {}: unknown power type '{}'", id, type);
					continue;
				}
				RacePower power = factory.create(pj);
				powers.add(power);
				if (keepsInNether(type.getPath(), pj)) kept.add(power);
			}
		}

		RaceProfile.AbilityInfo info = null;
		if (o.has("ability")) {
			JsonObject a = o.getAsJsonObject("ability");
			String name = a.get("name").getAsString();
			builder.ability(name, JsonParams.str(a, "description", ""));
			Ability ability = null;
			String type = JsonParams.str(a, "type", "");
			if (!type.isEmpty()) {
				PowerRegistry.AbilityFactory factory = PowerRegistry.abilityType(Identifier.of(type));
				if (factory == null) DaturaOriginsMod.LOGGER.error("Race {}: unknown ability type '{}'", id, type);
				else ability = factory.create(a);
			}
			info = new RaceProfile.AbilityInfo(name, JsonParams.integer(a, "cooldown", 200), ability);
		}

		Race race = RaceRegistry.register(builder.build());
		RaceProfiles.register(new RaceProfile(race, List.copyOf(powers), info));
		if (JsonParams.bool(o, "nether_suppressed", false)) {
			RaceProfiles.registerNetherForm(new RaceProfile(race, List.copyOf(kept), null));
		}
	}

	private static boolean keepsInNether(String type, JsonObject json) {
		if (NETHER_KEEP.contains(type) || "keep".equals(JsonParams.str(json, "nether", ""))) return true;
		return type.equals("conditional_attribute") && JsonParams.num(json, "amount", 0.0F) < 0;
	}
}
