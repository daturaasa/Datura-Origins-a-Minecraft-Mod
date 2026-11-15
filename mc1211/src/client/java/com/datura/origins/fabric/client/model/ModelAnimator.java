package com.datura.origins.fabric.client.model;

import com.datura.origins.fabric.power.EntityFlags;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Map;

public final class ModelAnimator {
	public static final String MERMAID_FLAG = "mermaid";
	private static final float BLEND_RATE = 7.0F;
	private static final Map<Integer, Map<Identifier, State>> STATES = new HashMap<>();
	private static final Map<Integer, float[]> SITTING = new HashMap<>();

	private static final class State {
		final Map<String, float[]> clips = new HashMap<>();
		String target = "";
		float since;
		float last = -1;
	}

	private ModelAnimator() {
	}

	public static void forget(int entityId) {
		STATES.remove(entityId);
		SITTING.remove(entityId);
	}

	public static void clear() {
		STATES.clear();
		SITTING.clear();
	}

	public static boolean isMermaidSitting(PlayerEntity player) {
		return EntityFlags.clientHas(player, MERMAID_FLAG) && player.getPose() == EntityPose.STANDING && !player.isTouchingWater()
				&& !player.hasVehicle() && !player.isFallFlying() && !player.isSleeping();
	}

	public static boolean sitsInPreview(PlayerEntity player) {
		if (!RaceModels.inPreview(player)) return false;
		for (Identifier id : RaceModels.modelsOf(player)) {
			PartModel model = PartModel.get(id);
			if (model.hidesLegs() && model.hasAnimation("sit_idle")) return true;
		}
		return false;
	}

	public static float sitAmount(PlayerEntity player, float progress) {
		if (RaceModels.inPreview(player)) return sitsInPreview(player) ? 1.0F : 0.0F;
		float[] s = SITTING.computeIfAbsent(player.getId(), k -> new float[]{0, -1});
		float dt = s[1] < 0 ? 0 : MathHelper.clamp((progress - s[1]) / 20.0F, 0.0F, 0.25F);
		s[1] = progress;
		float goal = isMermaidSitting(player) ? 1.0F : 0.0F;
		s[0] += (goal - s[0]) * (1.0F - (float) Math.exp(-dt * 4.0F));
		return s[0];
	}

	public static void pose(Identifier id, PartModel model, PlayerEntity player, float progress, boolean preview) {
		State state = STATES.computeIfAbsent(player.getId(), k -> new HashMap<>()).computeIfAbsent(id, k -> new State());
		float dt = state.last < 0 ? 0 : MathHelper.clamp((progress - state.last) / 20.0F, 0.0F, 0.25F);
		state.last = progress;
		double speed = player.getPos().subtract(player.prevX, player.prevY, player.prevZ).length();

		String target = preview ? pick(model, model.hidesLegs() ? "sit_idle" : "idle", "idle", "swim_idle") : target(model, player, speed);
		if (!target.equals(state.target)) {
			state.target = target;
			state.since = 0;
		} else {
			state.since += dt;
		}
		String clip = target;
		if (target.equals("sit")) {
			clip = state.since < model.length("sit_down") || !model.hasAnimation("sit_idle") ? "sit_down" : "sit_idle";
		}
		float rate = rate(clip, speed);
		float blend = 1.0F - (float) Math.exp(-dt * BLEND_RATE);
		if (state.clips.isEmpty()) state.clips.put(clip, new float[]{1, 0});
		state.clips.computeIfAbsent(clip, k -> new float[]{0, 0});
		state.clips.entrySet().removeIf(e -> {
			float[] c = e.getValue();
			boolean active = e.getKey().equals(clip);
			c[0] += ((active ? 1 : 0) - c[0]) * blend;
			c[1] += dt * (active ? rate : 1.0F);
			if (e.getKey().equals("sit_down")) c[1] = Math.min(c[1], state.since);
			return !active && c[0] < 0.01F;
		});

		model.resetPose();
		for (Map.Entry<String, float[]> e : state.clips.entrySet()) {
			model.animate(e.getKey(), e.getValue()[1], e.getValue()[0]);
		}
		if (!preview && player.handSwinging && model.hasAnimation("balanco")) {
			model.animate("balanco", player.handSwingProgress * model.length("balanco"), 1.0F);
		}
	}

	private static String target(PartModel model, PlayerEntity player, double speed) {
		if (player.isFallFlying()) {
			return player.isSneaking() ? pick(model, "glide", "fly", "idle") : pick(model, "fly", "swim_fast", "swim", "idle");
		}
		if (player.isTouchingWater()) {
			if (speed > 0.2) return pick(model, "swim_fast", "swim", "swim_idle", "idle");
			if (speed > 0.03) return pick(model, "swim", "swim_idle", "idle");
			return pick(model, "swim_idle", "idle");
		}
		if (model.hasAnimation("crawl")) {
			if (isMermaidSitting(player) && model.hasAnimation("sit_down")) return "sit";
			if (speed > 0.01) return "crawl";
			return pick(model, "swim_idle", "idle");
		}
		if (player.isOnGround()) return pick(model, speed > 0.05 ? "walk" : "rest", "rest", "idle");
		return pick(model, "idle", "rest");
	}

	private static float rate(String clip, double speed) {
		return switch (clip) {
			case "swim", "swim_fast" -> MathHelper.clamp(0.7F + (float) speed * 3.0F, 0.7F, 1.6F);
			case "crawl" -> MathHelper.clamp(0.5F + (float) speed * 8.0F, 0.5F, 1.4F);
			case "walk" -> MathHelper.clamp(0.6F + (float) speed * 5.0F, 0.6F, 1.8F);
			default -> 1.0F;
		};
	}

	private static String pick(PartModel model, String... names) {
		for (String name : names) {
			if (model.hasAnimation(name)) return name;
		}
		return names[names.length - 1];
	}
}
