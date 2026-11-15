package com.datura.origins.fabric.client.model;

import com.datura.origins.fabric.DaturaOriginsMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class PartModel {
	private static final Map<Identifier, PartModel> CACHE = new ConcurrentHashMap<>();
	private static final PartModel EMPTY = new PartModel(List.of(), 16, 16, null, Map.of(), false, false, false);
	private static final String[] FACES = {"down", "up", "north", "south", "west", "east"};
	private static final float[][] NORMALS = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};

	private final List<Bone> roots;
	private final Map<String, Bone> bones = new HashMap<>();
	private final float textureWidth;
	private final float textureHeight;
	private final Bone liftBone;
	private final Map<String, Animation> animations;
	private final boolean onHead;
	private final boolean physics;
	private final boolean hidesLegs;
	private final boolean glows;

	private PartModel(List<Bone> roots, float textureWidth, float textureHeight, String liftBone, Map<String, Animation> animations,
			boolean onHead, boolean physics, boolean hidesLegs) {
		this.roots = roots;
		this.onHead = onHead;
		this.physics = physics;
		this.hidesLegs = hidesLegs;
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
		this.animations = animations;
		for (Bone root : roots) index(root);
		this.liftBone = liftBone == null ? null : bones.get(liftBone);
		this.glows = bones.values().stream().anyMatch(b -> b.glow && !b.cubes.isEmpty());
	}

	private void index(Bone bone) {
		bones.put(bone.name, bone);
		for (Bone child : bone.children) index(child);
	}

	public static PartModel get(Identifier id) {
		return CACHE.computeIfAbsent(id, PartModel::load);
	}

	public static void clearCache() {
		CACHE.clear();
	}

	public boolean isEmpty() {
		return roots.isEmpty();
	}

	public boolean onHead() {
		return onHead;
	}

	public boolean physics() {
		return physics;
	}

	public boolean hidesLegs() {
		return hidesLegs;
	}

	public boolean glows() {
		return glows;
	}

	public boolean hasAnimation(String name) {
		return animations.containsKey(name);
	}

	public float length(String name) {
		Animation animation = animations.get(name);
		return animation == null ? 1.0F : animation.length;
	}

	public Bone liftBone() {
		return liftBone;
	}

	public Bone bone(String name) {
		return bones.get(name);
	}

	public List<Bone> chain() {
		List<Bone> chain = new ArrayList<>();
		for (Bone b = liftBone; b != null; ) {
			chain.add(b);
			Bone next = null;
			for (Bone child : b.children) {
				if (next == null || child.depth() > next.depth()) next = child;
			}
			b = next;
		}
		return chain;
	}

	public void resetPose() {
		for (Bone bone : bones.values()) bone.reset();
	}

	public void animate(String name, float seconds, float weight) {
		Animation animation = animations.get(name);
		if (animation == null || weight <= 0) return;
		float time = animation.loop ? seconds % animation.length : Math.min(seconds, animation.length);
		float[] out = new float[3];
		for (Map.Entry<String, Track> entry : animation.tracks.entrySet()) {
			Bone bone = bones.get(entry.getKey());
			if (bone == null) continue;
			Track track = entry.getValue();
			if (track.rotation != null) {
				sample(track.rotation, time, out);
				bone.add(out[0] * weight, out[1] * weight, out[2] * weight);
			}
			if (track.position != null) {
				sample(track.position, time, out);
				for (int i = 0; i < 3; i++) bone.offset[i] += out[i] * weight;
			}
			if (track.scale != null) {
				sample(track.scale, time, out);
				for (int i = 0; i < 3; i++) bone.scale[i] += (out[i] - 1.0F) * weight;
			}
		}
	}

	private static void sample(float[][] frames, float time, float[] out) {
		int n = frames.length;
		if (n == 1 || time <= frames[0][0]) {
			System.arraycopy(frames[0], 1, out, 0, 3);
			return;
		}
		if (time >= frames[n - 1][0]) {
			System.arraycopy(frames[n - 1], 1, out, 0, 3);
			return;
		}
		int i = 0;
		while (i < n - 2 && frames[i + 1][0] <= time) i++;
		float mode = frames[i].length > 4 ? frames[i][4] : 0;
		if (mode == 2) {
			System.arraycopy(frames[i], 1, out, 0, 3);
			return;
		}
		if (mode == 1) {
			float k = (time - frames[i][0]) / Math.max(1.0e-4F, frames[i + 1][0] - frames[i][0]);
			for (int c = 1; c <= 3; c++) out[c - 1] = frames[i][c] + (frames[i + 1][c] - frames[i][c]) * k;
			return;
		}
		float[] p0 = frames[Math.max(0, i - 1)], p1 = frames[i], p2 = frames[i + 1], p3 = frames[Math.min(n - 1, i + 2)];
		float u = (time - p1[0]) / Math.max(1.0e-4F, p2[0] - p1[0]);
		float u2 = u * u, u3 = u2 * u;
		for (int c = 1; c <= 3; c++) {
			out[c - 1] = 0.5F * ((2 * p1[c]) + (-p0[c] + p2[c]) * u + (2 * p0[c] - 5 * p1[c] + 4 * p2[c] - p3[c]) * u2
					+ (-p0[c] + 3 * p1[c] - 3 * p2[c] + p3[c]) * u3);
		}
	}

	public static void toModelSpace(MatrixStack matrices) {
		matrices.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
		matrices.translate(0.0F, -24.0F, 0.0F);
	}

	public void render(MatrixStack matrices, VertexConsumer buffer, int light, int overlay, int color) {
		for (Bone root : roots) renderBone(matrices, buffer, light, overlay, color, root, false);
	}

	public void renderGlow(MatrixStack matrices, VertexConsumer buffer, int overlay, int color) {
		for (Bone root : roots) renderBone(matrices, buffer, 0xF000F0, overlay, color, root, true);
	}

	public void collectPoints(MatrixStack matrices, Consumer<Vector3f> sink) {
		for (Bone root : roots) collectBone(matrices, sink, root);
	}

	private void renderBone(MatrixStack matrices, VertexConsumer buffer, int light, int overlay, int color, Bone bone, boolean glowPass) {
		matrices.push();
		bone.apply(matrices);
		if (bone.glow == glowPass) {
			for (Cube cube : bone.cubes) cube.render(matrices, buffer, light, overlay, color, textureWidth, textureHeight);
		}
		for (Bone child : bone.children) renderBone(matrices, buffer, light, overlay, color, child, glowPass);
		matrices.pop();
	}

	private void collectBone(MatrixStack matrices, Consumer<Vector3f> sink, Bone bone) {
		matrices.push();
		bone.apply(matrices);
		for (Cube cube : bone.cubes) cube.corners(matrices, sink);
		for (Bone child : bone.children) collectBone(matrices, sink, child);
		matrices.pop();
	}

	private static void rotate(MatrixStack matrices, float[] origin, float x, float y, float z) {
		if (x == 0 && y == 0 && z == 0) return;
		matrices.translate(origin[0], origin[1], origin[2]);
		if (z != 0) matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(z));
		if (y != 0) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(y));
		if (x != 0) matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(x));
		matrices.translate(-origin[0], -origin[1], -origin[2]);
	}

	public static final class Bone {
		final String name;
		final float[] origin;
		final float[] rotation;
		final boolean glow;
		final float[] pose = new float[3];
		final float[] offset = new float[3];
		final float[] scale = {1, 1, 1};
		final List<Cube> cubes = new ArrayList<>();
		final List<Bone> children = new ArrayList<>();

		Bone(String name, float[] origin, float[] rotation, boolean glow) {
			this.name = name;
			this.origin = origin;
			this.rotation = rotation;
			this.glow = glow;
		}

		void reset() {
			for (int i = 0; i < 3; i++) {
				pose[i] = 0;
				offset[i] = 0;
				scale[i] = 1;
			}
		}

		public void add(float x, float y, float z) {
			pose[0] += x;
			pose[1] += y;
			pose[2] += z;
		}

		int depth() {
			int deepest = 0;
			for (Bone child : children) deepest = Math.max(deepest, child.depth());
			return deepest + 1;
		}

		void apply(MatrixStack matrices) {
			if (offset[0] != 0 || offset[1] != 0 || offset[2] != 0) matrices.translate(offset[0], offset[1], offset[2]);
			rotate(matrices, origin, rotation[0] + pose[0], rotation[1] + pose[1], rotation[2] + pose[2]);
			if (scale[0] != 1 || scale[1] != 1 || scale[2] != 1) {
				matrices.translate(origin[0], origin[1], origin[2]);
				matrices.scale(Math.max(0.0F, scale[0]), Math.max(0.0F, scale[1]), Math.max(0.0F, scale[2]));
				matrices.translate(-origin[0], -origin[1], -origin[2]);
			}
		}
	}

	private record Cube(float[] from, float[] to, float[] origin, float[] rotation, float[][] faces) {
		void corners(MatrixStack matrices, Consumer<Vector3f> sink) {
			matrices.push();
			rotate(matrices, origin, rotation[0], rotation[1], rotation[2]);
			Matrix4f matrix = matrices.peek().getPositionMatrix();
			for (int i = 0; i < 8; i++) {
				Vector3f v = new Vector3f((i & 1) == 0 ? from[0] : to[0], (i & 2) == 0 ? from[1] : to[1], (i & 4) == 0 ? from[2] : to[2]);
				sink.accept(matrix.transformPosition(v));
			}
			matrices.pop();
		}

		void render(MatrixStack matrices, VertexConsumer buffer, int light, int overlay, int color, float tw, float th) {
			matrices.push();
			rotate(matrices, origin, rotation[0], rotation[1], rotation[2]);
			MatrixStack.Entry entry = matrices.peek();
			Matrix4f matrix = entry.getPositionMatrix();
			float fx = from[0], fy = from[1], fz = from[2], tx = to[0], ty = to[1], tz = to[2];
			for (int face = 0; face < 6; face++) {
				float[] uv = faces[face];
				if (uv == null) continue;
				float[][] p = switch (face) {
					case 0 -> new float[][]{{fx, fy, tz}, {fx, fy, fz}, {tx, fy, fz}, {tx, fy, tz}};
					case 1 -> new float[][]{{fx, ty, fz}, {fx, ty, tz}, {tx, ty, tz}, {tx, ty, fz}};
					case 2 -> new float[][]{{tx, ty, fz}, {tx, fy, fz}, {fx, fy, fz}, {fx, ty, fz}};
					case 3 -> new float[][]{{fx, ty, tz}, {fx, fy, tz}, {tx, fy, tz}, {tx, ty, tz}};
					case 4 -> new float[][]{{fx, ty, fz}, {fx, fy, fz}, {fx, fy, tz}, {fx, ty, tz}};
					default -> new float[][]{{tx, ty, tz}, {tx, fy, tz}, {tx, fy, fz}, {tx, ty, fz}};
				};
				int shift = Math.floorMod(Math.round(uv[4] / 90.0F), 4);
				float[] n = NORMALS[face];
				for (int i = 0; i < 4; i++) {
					int corner = (i + shift) % 4;
					float u = (corner == 0 || corner == 1 ? uv[0] : uv[2]) / tw;
					float v = (corner == 0 || corner == 3 ? uv[1] : uv[3]) / th;
					buffer.vertex(matrix, p[i][0], p[i][1], p[i][2])
							.color(color)
							.texture(u, v)
							.overlay(overlay)
							.light(light)
							.normal(entry, n[0], n[1], n[2]);
				}
			}
			matrices.pop();
		}
	}

	private record Track(float[][] rotation, float[][] position, float[][] scale) {
	}

	private record Animation(float length, boolean loop, Map<String, Track> tracks) {
	}

	private static PartModel load(Identifier id) {
		Identifier file = Identifier.of(id.getNamespace(), "models/part/" + id.getPath() + ".json");
		Optional<Resource> resource = MinecraftClient.getInstance().getResourceManager().getResource(file);
		if (resource.isEmpty()) {
			DaturaOriginsMod.LOGGER.error("Missing part model {}", file);
			return EMPTY;
		}
		try (Reader reader = resource.get().getReader()) {
			JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
			JsonArray size = json.getAsJsonArray("texture_size");
			Map<String, Bone> byName = new HashMap<>();
			List<Bone> roots = new ArrayList<>();
			for (JsonElement element : json.getAsJsonArray("bones")) {
				JsonObject b = element.getAsJsonObject();
				boolean glow = b.has("glow") && b.get("glow").getAsBoolean();
				Bone bone = new Bone(b.get("name").getAsString(), vec(b, "origin"), vec(b, "rotation"), glow);
				for (JsonElement c : b.getAsJsonArray("cubes")) bone.cubes.add(cube(c.getAsJsonObject()));
				byName.put(bone.name, bone);
				Bone parent = b.has("parent") && !b.get("parent").isJsonNull() ? byName.get(b.get("parent").getAsString()) : null;
				if (parent != null) parent.children.add(bone);
				else roots.add(bone);
			}
			Map<String, Animation> animations = new HashMap<>();
			JsonObject anims = json.getAsJsonObject("animations");
			if (anims != null) {
				for (Map.Entry<String, JsonElement> a : anims.entrySet()) {
					JsonObject o = a.getValue().getAsJsonObject();
					Map<String, Track> tracks = new HashMap<>();
					for (Map.Entry<String, JsonElement> t : o.getAsJsonObject("bones").entrySet()) {
						JsonObject channels = t.getValue().getAsJsonObject();
						tracks.put(t.getKey(), new Track(frames(channels, "rotation"), frames(channels, "position"), frames(channels, "scale")));
					}
					animations.put(a.getKey(), new Animation(Math.max(0.05F, o.get("length").getAsFloat()), o.get("loop").getAsBoolean(), tracks));
				}
			}
			String lift = json.has("lift_bone") ? json.get("lift_bone").getAsString() : null;
			boolean head = json.has("attach") && "head".equals(json.get("attach").getAsString());
			boolean physics = !json.has("physics") || json.get("physics").getAsBoolean();
			boolean hidesLegs = false;
			if (json.has("hide")) {
				for (JsonElement part : json.getAsJsonArray("hide")) hidesLegs |= "legs".equals(part.getAsString());
			}
			return new PartModel(roots, size.get(0).getAsFloat(), size.get(1).getAsFloat(), lift, animations, head, physics && !head, hidesLegs);
		} catch (Exception e) {
			DaturaOriginsMod.LOGGER.error("Failed to load part model {}", file, e);
			return EMPTY;
		}
	}

	private static float[][] frames(JsonObject channels, String key) {
		if (!channels.has(key)) return null;
		JsonArray list = channels.getAsJsonArray(key);
		if (list.isEmpty()) return null;
		float[][] data = new float[list.size()][5];
		for (int i = 0; i < data.length; i++) {
			JsonArray f = list.get(i).getAsJsonArray();
			for (int k = 0; k < Math.min(5, f.size()); k++) data[i][k] = f.get(k).getAsFloat();
		}
		return data;
	}

	private static Cube cube(JsonObject c) {
		JsonObject faceJson = c.getAsJsonObject("faces");
		float[][] faces = new float[6][];
		for (int i = 0; i < FACES.length; i++) {
			if (!faceJson.has(FACES[i])) continue;
			JsonArray a = faceJson.getAsJsonArray(FACES[i]);
			faces[i] = new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat(), a.get(3).getAsFloat(),
					a.size() > 4 ? a.get(4).getAsFloat() : 0};
		}
		float inflate = c.has("inflate") ? c.get("inflate").getAsFloat() : 0;
		float[] from = vec(c, "from"), to = vec(c, "to");
		for (int i = 0; i < 3; i++) {
			from[i] -= inflate;
			to[i] += inflate;
		}
		return new Cube(from, to, vec(c, "origin"), vec(c, "rotation"), faces);
	}

	private static float[] vec(JsonObject o, String key) {
		if (!o.has(key)) return new float[3];
		JsonArray a = o.getAsJsonArray(key);
		return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
	}
}
