package com.datura.origins.fabric.race;

import com.datura.origins.fabric.OriginData;
import com.datura.origins.fabric.power.Control;
import com.datura.origins.fabric.power.EntityFlags;
import com.datura.origins.fabric.power.RacePower;
import com.datura.origins.fabric.power.ability.PhaseAbility;
import com.datura.origins.fabric.power.types.AttributePower;
import com.datura.origins.fabric.power.types.BatFormPower;
import com.datura.origins.fabric.power.types.FungalEmergencyPower;
import com.datura.origins.fabric.power.types.GoldenWardPower;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RaceManager {
	public static final String MODEL_FLAG = "model:";
	private static final Map<UUID, Long> READY = new ConcurrentHashMap<>();
	private static long serverTicks;

	private RaceManager() {
	}

	public static long ticks() {
		return serverTicks;
	}

	public static Optional<RaceProfile> getRace(ServerPlayerEntity player) {
		return RaceAccess.getRace(player);
	}

	public static void apply(ServerPlayerEntity player) {
		getRace(player).ifPresent(r -> {
			for (RacePower power : r.powers()) power.onApply(player);
			modelFlag(player, r, true);
		});
	}

	private static void modelFlag(ServerPlayerEntity player, RaceProfile race, boolean on) {
		for (String model : race.race().models()) EntityFlags.set(player, MODEL_FLAG + model, on);
	}

	public static void clear(ServerPlayerEntity player) {
		getRace(player).ifPresent(r -> {
			removeAbility(player, r);
			for (RacePower power : r.powers()) power.onRemove(player);
			modelFlag(player, r, false);
		});
		READY.remove(player.getUuid());
	}

	public static void onLeave(ServerPlayerEntity player) {
		getRace(player).ifPresent(r -> removeAbility(player, r));
		READY.remove(player.getUuid());
	}

	public static void onRespawn(ServerPlayerEntity player) {
		getRace(player).ifPresent(r -> {
			removeAbility(player, r);
			for (RacePower power : r.powers()) power.onApply(player);
		});
		EntityFlags.resend(player);
	}

	public static void tick(MinecraftServer server) {
		serverTicks++;
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (!player.isAlive()) continue;
			netherCheck(player);
			Optional<RaceProfile> race = getRace(player);
			if (race.isEmpty()) continue;
			for (RacePower power : race.get().powers()) power.tick(player);
		}
	}

	private static void netherCheck(ServerPlayerEntity player) {
		String id = player.getAttached(OriginData.ORIGIN);
		Optional<RaceProfile> weak = RaceProfiles.netherForm(id);
		boolean should = weak.isPresent() && player.getWorld().getRegistryKey() == World.NETHER;
		boolean now = RaceAccess.IN_NETHER.contains(player.getUuid());
		if (should == now) return;
		Optional<RaceProfile> full = RaceProfiles.get(id);
		if (should) {
			full.ifPresent(r -> {
				removeAbility(player, r);
				for (RacePower power : r.powers()) if (!weak.get().powers().contains(power)) power.onRemove(player);
			});
			RaceAccess.IN_NETHER.add(player.getUuid());
			player.sendMessage(Text.translatable("datura_origins.message.nether_weak").formatted(Formatting.DARK_RED), true);
		} else {
			RaceAccess.IN_NETHER.remove(player.getUuid());
			full.ifPresent(r -> {
				for (RacePower power : r.powers()) if (weak.isEmpty() || !weak.get().powers().contains(power)) power.onApply(player);
			});
			player.sendMessage(Text.translatable("datura_origins.message.nether_restored").formatted(Formatting.GREEN), true);
		}
	}

	private static void removeAbility(ServerPlayerEntity player, RaceProfile race) {
		if (race.ability() != null && race.ability().ability() != null) race.ability().ability().onRemove(player);
	}

	public static void useAbility(ServerPlayerEntity player) {
		if (!player.isAlive() || player.isSpectator() && !PhaseAbility.isPhased(player)) return;
		Optional<RaceProfile> race = getRace(player);
		if (race.isEmpty() || race.get().ability() == null) return;
		if (Control.isLocked(player)) {
			player.sendMessage(Text.translatable("datura_origins.message.locked").formatted(Formatting.GRAY), true);
			return;
		}
		RaceProfile.AbilityInfo info = race.get().ability();
		boolean free = OriginData.NO_COOLDOWN.contains(player.getUuid());
		long ready = free ? 0 : READY.getOrDefault(player.getUuid(), 0L);
		if (serverTicks < ready) {
			long secs = (ready - serverTicks + 19) / 20;
			player.sendMessage(Text.translatable("datura_origins.message.cooldown", info.nameText(), secs).formatted(Formatting.GRAY), true);
			return;
		}
		if (info.ability() == null) return;
		if (info.ability().activate(player) && !free) {
			READY.put(player.getUuid(), serverTicks + info.cooldownTicks());
		}
	}

	public static void choose(ServerPlayerEntity player, int option) {
		getRace(player).ifPresent(r -> {
			if (r.ability() != null && r.ability().ability() != null) r.ability().ability().choose(player, option);
		});
	}

	public static void resetCooldowns(ServerPlayerEntity player) {
		READY.remove(player.getUuid());
		BatFormPower.resetCooldown(player);
		FungalEmergencyPower.resetCooldown(player);
		getRace(player).ifPresent(r -> {
			if (r.powers().stream().anyMatch(p -> p instanceof GoldenWardPower)) EntityFlags.set(player, GoldenWardPower.FLAG, true);
		});
	}

	public static void snapAttributes(ServerPlayerEntity player) {
		getRace(player).ifPresent(r -> {
			for (RacePower power : r.powers()) if (power instanceof AttributePower ap) ap.snap(player);
		});
	}
}
