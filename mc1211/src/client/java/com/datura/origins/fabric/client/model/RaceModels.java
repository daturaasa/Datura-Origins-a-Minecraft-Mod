package com.datura.origins.fabric.client.model;

import com.datura.origins.fabric.client.ClientOrigin;
import com.datura.origins.fabric.power.EntityFlags;
import com.datura.origins.fabric.power.types.TranslucentPower;
import com.datura.origins.fabric.race.RaceProfiles;
import com.datura.origins.race.Race;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class RaceModels {
	public static final String FLAG_PREFIX = "model:";

	private static Race preview;

	private RaceModels() {
	}

	public static void beginPreview(Race race) {
		preview = race;
	}

	public static void endPreview() {
		preview = null;
	}

	public static boolean inPreview(PlayerEntity player) {
		return preview != null && player == MinecraftClient.getInstance().player;
	}

	public static List<Identifier> modelsOf(PlayerEntity player) {
		List<Identifier> out = new ArrayList<>();
		MinecraftClient client = MinecraftClient.getInstance();
		if (player == client.player) {
			Race race = preview != null ? preview : ClientOrigin.get().orElse(null);
			if (race != null) for (String model : race.models()) add(out, model);
			return out;
		}
		Set<String> flags = EntityFlags.CLIENT.get(player.getId());
		if (flags == null) return out;
		for (String flag : flags) {
			if (flag.startsWith(FLAG_PREFIX)) add(out, flag.substring(FLAG_PREFIX.length()));
		}
		return out;
	}

	private static void add(List<Identifier> out, String id) {
		Identifier parsed = parse(id);
		if (parsed != null) out.add(parsed);
	}

	public static boolean isGhost(PlayerEntity player) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (player == client.player) {
			Race race = preview != null ? preview : ClientOrigin.get().orElse(null);
			return race != null && RaceProfiles.get(race.id())
					.map(r -> r.powers().stream().anyMatch(p -> p instanceof TranslucentPower)).orElse(false);
		}
		return EntityFlags.clientHas(player, TranslucentPower.FLAG);
	}

	private static Identifier parse(String id) {
		return id == null || id.isEmpty() ? null : Identifier.tryParse(id);
	}
}
