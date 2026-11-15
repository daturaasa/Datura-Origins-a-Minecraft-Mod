package com.datura.origins.fabric.client.preview;

import com.datura.origins.fabric.client.model.RaceModels;
import com.datura.origins.race.Race;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RacePreviews {
	public static final RacePreview PLAYER = RacePreviews::drawPlayer;
	private static final Map<String, RacePreview> PREVIEWS = new ConcurrentHashMap<>();

	private RacePreviews() {
	}

	public static void register(String raceId, RacePreview preview) {
		PREVIEWS.put(raceId, preview);
	}

	public static RacePreview get(Race race) {
		return race == null ? PLAYER : PREVIEWS.getOrDefault(race.id(), PLAYER);
	}

	private static void drawPlayer(DrawContext context, LivingEntity entity, float x, float y, float size, float yaw, float pitch, float delta) {
		float bodyYaw = entity.bodyYaw;
		float prevBodyYaw = entity.prevBodyYaw;
		float entityYaw = entity.getYaw();
		float entityPitch = entity.getPitch();
		float headYaw = entity.headYaw;
		float prevHeadYaw = entity.prevHeadYaw;

		float facing = 180.0F + yaw;
		entity.bodyYaw = facing;
		entity.prevBodyYaw = facing;
		entity.setYaw(facing);
		entity.setPitch(0.0F);
		entity.headYaw = facing;
		entity.prevHeadYaw = facing;

		Quaternionf tilt = new Quaternionf().rotateX(pitch * ((float) Math.PI / 180.0F));
		Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI).mul(tilt);
		float scale = entity.getScale();
		PreviewFrame frame = PreviewFrame.of(RaceModels.previewRace());
		Vector3f offset = new Vector3f(0.0F, frame.middle() * scale, 0.0F);
		InventoryScreen.drawEntity(context, x, y, size * frame.scale() / scale, offset, rotation, tilt, entity);

		entity.bodyYaw = bodyYaw;
		entity.prevBodyYaw = prevBodyYaw;
		entity.setYaw(entityYaw);
		entity.setPitch(entityPitch);
		entity.headYaw = headYaw;
		entity.prevHeadYaw = prevHeadYaw;
	}
}
