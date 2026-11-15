package com.datura.origins.fabric.client.preview;

import com.datura.origins.fabric.client.model.PartModel;
import com.datura.origins.fabric.power.types.AttributePower;
import com.datura.origins.fabric.race.RaceProfiles;
import com.datura.origins.race.Race;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public record PreviewFrame(float scale, float top, float bottom, float radius) {
	private static final float PIXEL = 0.9375F / 16.0F;
	private static final PreviewFrame PLAYER = new PreviewFrame(1.0F, 32.0F, 0.0F, 8.0F);
	private static final Map<String, PreviewFrame> CACHE = new ConcurrentHashMap<>();

	public static PreviewFrame of(Race race) {
		return race == null ? PLAYER : CACHE.computeIfAbsent(race.id(), id -> measure(race));
	}

	public static void clear() {
		CACHE.clear();
	}

	public float height() {
		return (top - bottom) * PIXEL * scale;
	}

	public float width() {
		return radius * 2.0F * PIXEL * scale;
	}

	public float middle() {
		return (top + bottom) * 0.5F * PIXEL;
	}

	private static PreviewFrame measure(Race race) {
		float scale = RaceProfiles.get(race.id())
				.map(profile -> profile.powers().stream()
						.filter(power -> power instanceof AttributePower)
						.mapToDouble(power -> ((AttributePower) power).previewScale())
						.reduce(1.0, (a, b) -> a * b))
				.orElse(1.0).floatValue();
		float[] box = {32.0F, 0.0F, 8.0F};
		for (String name : race.models()) {
			Identifier id = name == null || name.isEmpty() ? null : Identifier.tryParse(name);
			if (id == null) continue;
			PartModel model = PartModel.get(id);
			if (model.isEmpty()) continue;
			String clip = model.hidesLegs() && model.hasAnimation("sit_idle") ? "sit_idle" : model.hasAnimation("idle") ? "idle" : null;
			float length = clip == null ? 0.0F : Math.min(model.length(clip), 6.0F);
			for (float time = 0.0F; time <= length; time += 0.25F) {
				model.resetPose();
				if (clip != null) model.animate(clip, time, 1.0F);
				model.collectPoints(new MatrixStack(), point -> {
					box[0] = Math.max(box[0], point.y() + 1.5F);
					box[1] = Math.min(box[1], point.y() - 1.0F);
					box[2] = Math.max(box[2], (float) Math.sqrt(point.x() * point.x() + point.z() * point.z()) * 1.06F);
				});
			}
			model.resetPose();
		}
		return new PreviewFrame(scale, box[0], box[1], box[2]);
	}
}
