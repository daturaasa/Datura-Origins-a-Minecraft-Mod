package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.DaturaOriginsMod;
import com.datura.origins.fabric.network.MaturityPayload;
import com.datura.origins.fabric.power.JsonParams;
import com.datura.origins.fabric.power.RacePower;
import com.datura.origins.fabric.race.RaceAccess;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MaturityPower implements RacePower {
	public static final AttachmentType<Integer> POINTS = AttachmentRegistry.<Integer>create(
			DaturaOriginsMod.id("maturity"), b -> b.persistent(Codec.INT).copyOnDeath());
	public static final AttachmentType<Integer> STORED_XP = AttachmentRegistry.<Integer>create(
			DaturaOriginsMod.id("stored_xp"), b -> b.persistent(Codec.INT).copyOnDeath());

		private static boolean restoringXp = false;

	private final int[] thresholds;
	private final float pointsPerHealth;
	private final boolean onlyMonsters;
	private final int[] colors;
	private final String growMessage;
		private final int perKill;
		private final boolean fromXp;
		private final boolean fromKills;
		private final boolean keepXp;

	public MaturityPower(JsonObject json) {
		List<Integer> t = new ArrayList<>();
		if (json.has("thresholds")) for (JsonElement e : json.getAsJsonArray("thresholds")) t.add(e.getAsInt());
		if (t.isEmpty()) t = List.of(500, 1500);
		this.thresholds = t.stream().mapToInt(Integer::intValue).toArray();
		this.pointsPerHealth = JsonParams.num(json, "points_per_health", 0.5f);
		this.onlyMonsters = JsonParams.bool(json, "only_monsters", true);
		List<String> c = JsonParams.strings(json, "colors");
		if (c.isEmpty()) c = List.of("#7ED957", "#F2B33D", "#D9364A");
		this.growMessage = JsonParams.str(json, "grow_message", "datura_origins.message.maturity_grow");
		this.fromXp = JsonParams.bool(json, "from_xp", false);
		this.fromKills = JsonParams.bool(json, "from_kills", true);
		this.perKill = JsonParams.integer(json, "per_kill", 0);
		this.keepXp = JsonParams.bool(json, "keep_xp", false);
		this.colors = c.stream().mapToInt(s -> 0xFF000000 | Integer.parseInt(s.replace("#", ""), 16)).toArray();
	}

	public int maxPoints() {
		return thresholds[thresholds.length - 1];
	}

	public int stageCount() {
		return thresholds.length + 1;
	}

		public int stageOf(int points) {
		int s = 0;
		for (int th : thresholds) if (points >= th) s++;
		return s;
	}

		public float totalProgress(int points) {
		return Math.min(1f, points / (float) maxPoints());
	}

		public float stageProgress(int points) {
		int s = stageOf(points);
		if (s >= thresholds.length) return 1f;
		int start = s == 0 ? 0 : thresholds[s - 1];
		return (points - start) / (float) (thresholds[s] - start);
	}

		public int barColor(int points) {
		float f = totalProgress(points) * (colors.length - 1);
		int i = Math.min(colors.length - 2, (int) f);
		return mix(colors[i], colors[i + 1], f - i);
	}

		public static volatile int clientPoints = 0;

	public static int mix(int a, int b, float t) {
		if (t <= 0f) return a;
		if (t >= 1f) return b;
		int r = (int) (((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
		int g = (int) (((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
		int bl = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
		return 0xFF000000 | (r << 16) | (g << 8) | bl;
	}

		public static void init() {}

	public static int points(PlayerEntity player) {
		if (player.getWorld().isClient) return clientPoints;
		Integer p = player.getAttached(POINTS);
		return p == null ? 0 : p;
	}

		public static Optional<MaturityPower> of(PlayerEntity player) {
		return RaceAccess.getRace(player).flatMap(r -> r.powers().stream()
				.filter(p -> p instanceof MaturityPower).map(p -> (MaturityPower) p).findFirst());
	}

		public static int stage(PlayerEntity player) {
		return of(player).map(m -> m.stageOf(points(player))).orElse(0);
	}

		public static float multiplier(PlayerEntity player, float[] values) {
		if (values == null || values.length == 0) return 1f;
		return values[Math.min(Math.max(0, stage(player)), values.length - 1)];
	}

		public static float progress(PlayerEntity player) {
		return of(player).map(m -> m.totalProgress(points(player))).orElse(0f);
	}

		public static boolean blocksExperience(PlayerEntity player) {
		return !restoringXp && of(player).filter(m -> !m.keepXp).isPresent();
	}

		public static boolean isMax(PlayerEntity player) {
		return of(player).map(m -> m.stageOf(points(player)) >= m.stageCount() - 1).orElse(false);
	}

	@Override
	public void onApply(ServerPlayerEntity player) {
		if (!keepXp && player.getAttached(STORED_XP) == null) {
			player.setAttached(STORED_XP, player.totalExperience);
			player.setExperienceLevel(0);
			player.setExperiencePoints(0);
			player.totalExperience = 0;
		}
		if (player.getAttached(POINTS) == null) player.setAttached(POINTS, 0);
		sync(player);
		flags(player);
	}

		private void flags(ServerPlayerEntity player) {
		int st = stageOf(points(player));
		com.datura.origins.fabric.power.EntityFlags.set(player, "mat_1", st >= 1);
		com.datura.origins.fabric.power.EntityFlags.set(player, "mat_2", st >= 2);
	}

	@Override
	public void tick(ServerPlayerEntity player) {
		if (player.age % 100 == 0) flags(player);
	}

		public static void gainXp(PlayerEntity player, int amount) {
		if (amount <= 0 || !(player instanceof ServerPlayerEntity sp)) return;
		of(player).filter(m -> m.fromXp).ifPresent(m -> m.addPoints(sp, amount));
	}

		public static boolean maxOut(ServerPlayerEntity player) {
		var m = of(player);
		if (m.isEmpty()) return false;
		int before = points(player);
		if (before < m.get().maxPoints()) m.get().addPoints(player, m.get().maxPoints() - before);
		return true;
	}

		public static void grant(ServerPlayerEntity player, int gain) {
		if (gain > 0) of(player).ifPresent(m -> m.addPoints(player, gain));
	}

	private void addPoints(ServerPlayerEntity player, int gain) {
		int before = points(player);
		if (before >= maxPoints()) return;
		int after = Math.min(maxPoints(), before + gain);
		player.setAttached(POINTS, after);
		sync(player);
		if (stageOf(after) > stageOf(before)) {
			celebrate(player);
			flags(player);
		}
	}

	@Override
	public void onRemove(ServerPlayerEntity player) {
		Integer stored = player.getAttached(STORED_XP);
		player.removeAttached(STORED_XP);
		player.removeAttached(POINTS);
		com.datura.origins.fabric.power.EntityFlags.set(player, "mat_1", false);
		com.datura.origins.fabric.power.EntityFlags.set(player, "mat_2", false);
		if (stored != null && stored > 0) {
			restoringXp = true;
			try {
				player.addExperience(stored);
			} finally {
				restoringXp = false;
			}
		}
		sync(player);
	}

	@Override
	public void onKill(ServerPlayerEntity player, LivingEntity victim) {
		if (!fromKills || (onlyMonsters && !(victim instanceof Monster))) return;
		int gain = perKill > 0 ? perKill : Math.max(1, Math.round(victim.getMaxHealth() * pointsPerHealth));
		addPoints(player, gain);
	}

	private void celebrate(ServerPlayerEntity player) {
		player.sendMessage(Text.translatable(growMessage).formatted(Formatting.GOLD), true);
		player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 0.7f, 1.3f);
		if (player.getWorld() instanceof ServerWorld sw) {
			sw.spawnParticles(ParticleTypes.DRAGON_BREATH, player.getX(), player.getBodyY(0.5), player.getZ(),
					40, player.getWidth() * 0.6, player.getHeight() * 0.4, player.getWidth() * 0.6, 0.02);
			sw.spawnParticles(ParticleTypes.FLAME, player.getX(), player.getBodyY(0.3), player.getZ(),
					20, player.getWidth() * 0.5, 0.2, player.getWidth() * 0.5, 0.05);
		}
	}

	public static void sync(ServerPlayerEntity player) {
		if (ServerPlayNetworking.canSend(player, MaturityPayload.ID)) {
			ServerPlayNetworking.send(player, new MaturityPayload(points(player)));
		}
	}
}
