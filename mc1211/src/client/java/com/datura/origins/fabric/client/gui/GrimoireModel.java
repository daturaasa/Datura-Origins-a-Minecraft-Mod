package com.datura.origins.fabric.client.gui;

import com.datura.origins.DaturaOrigins;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class GrimoireModel {
	static final float HINGE_X = 4.25F;
	static final float HINGE_Y = 9.5F;

	private static final Identifier MODEL = Identifier.of(DaturaOrigins.MOD_ID, "models/grimorio.json");
	private static final Identifier TEXTURE = Identifier.of(DaturaOrigins.MOD_ID, "textures/gui/grimorio.png");
	private static final Identifier AURA = Identifier.of(DaturaOrigins.MOD_ID, "textures/gui/grimorio_aura.png");
	private static final int LIGHT = LightmapTextureManager.MAX_LIGHT_COORDINATE;
	private static final Logger LOGGER = LoggerFactory.getLogger(DaturaOrigins.MOD_NAME);
	private static final String[] FACES = {"down", "up", "north", "south", "west", "east"};

	private static GrimoireModel cached;

	private final List<Cube> cubes;
	private final float uvWidth;
	private final float uvHeight;
	private final int frames;
	private final int frameTicks;

	private GrimoireModel(List<Cube> cubes, float uvWidth, float uvHeight, int frames, int frameTicks) {
		this.cubes = cubes;
		this.uvWidth = uvWidth;
		this.uvHeight = uvHeight;
		this.frames = Math.max(1, frames);
		this.frameTicks = Math.max(1, frameTicks);
	}

	static GrimoireModel get() {
		if (cached == null) {
			cached = load();
		}
		return cached;
	}

	private static GrimoireModel load() {
		Optional<Resource> resource = MinecraftClient.getInstance().getResourceManager().getResource(MODEL);
		if (resource.isEmpty()) {
			return new GrimoireModel(List.of(), 1, 1, 1, 1);
		}
		try (Reader reader = resource.get().getReader()) {
			JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
			JsonArray uv = json.getAsJsonArray("uv_size");
			List<Cube> cubes = new ArrayList<>();
			for (JsonElement element : json.getAsJsonArray("cubes")) {
				cubes.add(Cube.parse(element.getAsJsonObject()));
			}
			return new GrimoireModel(cubes, uv.get(0).getAsFloat(), uv.get(1).getAsFloat(),
					json.get("frames").getAsInt(), json.get("frame_ticks").getAsInt());
		} catch (Exception e) {
			LOGGER.error("Failed to load grimoire model", e);
			return new GrimoireModel(List.of(), 1, 1, 1, 1);
		}
	}

	void render(MatrixStack matrices, VertexConsumerProvider buffers, float ticks, float coverAngle, float strapAlpha, float strapLift, float glow) {
		int frame = (int) (ticks / frameTicks) % frames;
		VertexConsumer solid = buffers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
		for (Cube cube : cubes) {
			if (cube.group != Group.STRAP) {
				drawCube(matrices, solid, cube, frame, coverAngle, 0.0F, 0xFFFFFFFF);
			}
		}
		if (strapAlpha > 0.01F) {
			int color = (Math.round(strapAlpha * 255.0F) << 24) | 0xFFFFFF;
			VertexConsumer strap = buffers.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE));
			for (Cube cube : cubes) {
				if (cube.group == Group.STRAP) {
					drawCube(matrices, strap, cube, frame, coverAngle, strapLift, color);
				}
			}
		}
		if (glow > 0.01F) {
			int value = Math.round(Math.min(glow, 1.0F) * 255.0F);
			int color = 0xFF000000 | value << 16 | value << 8 | value;
			VertexConsumer aura = buffers.getBuffer(RenderLayer.getEyes(AURA));
			for (Cube cube : cubes) {
				if (cube.glow && (cube.group != Group.STRAP || strapAlpha > 0.5F)) {
					drawCube(matrices, aura, cube, frame, coverAngle, cube.group == Group.STRAP ? strapLift : 0.0F, color);
				}
			}
		}
	}

	private void drawCube(MatrixStack matrices, VertexConsumer buffer, Cube cube, int frame, float coverAngle, float lift, int color) {
		matrices.push();
		if (cube.group == Group.COVER && coverAngle != 0.0F) {
			matrices.translate(HINGE_X, HINGE_Y, 0.0F);
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(coverAngle));
			matrices.translate(-HINGE_X, -HINGE_Y, 0.0F);
		}
		if (lift != 0.0F) {
			matrices.translate(0.0F, 0.0F, lift);
		}
		if (cube.rotated) {
			matrices.translate(cube.origin[0], cube.origin[1], cube.origin[2]);
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(cube.rotation[2]));
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(cube.rotation[1]));
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cube.rotation[0]));
			matrices.translate(-cube.origin[0], -cube.origin[1], -cube.origin[2]);
		}
		MatrixStack.Entry entry = matrices.peek();
		Matrix4f matrix = entry.getPositionMatrix();
		float fx = cube.from[0], fy = cube.from[1], fz = cube.from[2];
		float tx = cube.to[0], ty = cube.to[1], tz = cube.to[2];
		int a = color >>> 24, r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
		float frameOffset = frame * uvHeight;
		float vScale = uvHeight * frames;

		for (int face = 0; face < FACES.length; face++) {
			float[] uv = cube.faces[face];
			if (uv == null) {
				continue;
			}
			float u0 = uv[0] / uvWidth, u1 = uv[2] / uvWidth;
			float v0 = (uv[1] + frameOffset) / vScale, v1 = (uv[3] + frameOffset) / vScale;
			float[][] p = corners(face, fx, fy, fz, tx, ty, tz);
			float[] n = NORMALS[face];
			float[][] uvs = {{u0, v0}, {u0, v1}, {u1, v1}, {u1, v0}};
			for (int i = 0; i < 4; i++) {
				buffer.vertex(matrix, p[i][0], p[i][1], p[i][2])
						.color(r, g, b, a)
						.texture(uvs[i][0], uvs[i][1])
						.overlay(OverlayTexture.DEFAULT_UV)
						.light(LIGHT)
						.normal(entry, n[0], n[1], n[2]);
			}
		}
		matrices.pop();
	}

	private static final float[][] NORMALS = {
			{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}
	};

	private static float[][] corners(int face, float fx, float fy, float fz, float tx, float ty, float tz) {
		return switch (face) {
			case 0 -> new float[][]{{fx, fy, tz}, {fx, fy, fz}, {tx, fy, fz}, {tx, fy, tz}};
			case 1 -> new float[][]{{fx, ty, fz}, {fx, ty, tz}, {tx, ty, tz}, {tx, ty, fz}};
			case 2 -> new float[][]{{tx, ty, fz}, {tx, fy, fz}, {fx, fy, fz}, {fx, ty, fz}};
			case 3 -> new float[][]{{fx, ty, tz}, {fx, fy, tz}, {tx, fy, tz}, {tx, ty, tz}};
			case 4 -> new float[][]{{fx, ty, fz}, {fx, fy, fz}, {fx, fy, tz}, {fx, ty, tz}};
			default -> new float[][]{{tx, ty, tz}, {tx, fy, tz}, {tx, fy, fz}, {tx, ty, fz}};
		};
	}

	private enum Group {
		BODY, COVER, STRAP
	}

	private record Cube(Group group, boolean glow, float[] from, float[] to, float[] origin, float[] rotation, boolean rotated, float[][] faces) {
		static Cube parse(JsonObject json) {
			Group group = switch (json.get("group").getAsString()) {
				case "cover" -> Group.COVER;
				case "strap" -> Group.STRAP;
				default -> Group.BODY;
			};
			float[] rotation = vector(json, "rotation");
			JsonObject faceJson = json.getAsJsonObject("faces");
			float[][] faces = new float[FACES.length][];
			for (Map.Entry<String, JsonElement> entry : faceJson.entrySet()) {
				for (int i = 0; i < FACES.length; i++) {
					if (FACES[i].equals(entry.getKey())) {
						JsonArray array = entry.getValue().getAsJsonArray();
						faces[i] = new float[]{array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat(), array.get(3).getAsFloat()};
					}
				}
			}
			boolean rotated = rotation[0] != 0.0F || rotation[1] != 0.0F || rotation[2] != 0.0F;
			return new Cube(group, json.has("glow") && json.get("glow").getAsBoolean(),
					vector(json, "from"), vector(json, "to"), vector(json, "origin"), rotation, rotated, faces);
		}

		private static float[] vector(JsonObject json, String key) {
			if (!json.has(key)) {
				return new float[3];
			}
			JsonArray array = json.getAsJsonArray(key);
			return new float[]{array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()};
		}
	}
}
