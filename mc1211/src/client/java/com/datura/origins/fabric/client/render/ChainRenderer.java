package com.datura.origins.fabric.client.render;

import com.datura.origins.fabric.DaturaOriginsMod;
import com.datura.origins.fabric.power.EntityFlags;
import com.datura.origins.fabric.power.ability.DarkCometAbility;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;

import java.util.Set;

public final class ChainRenderer {
	private static final Identifier TEXTURE = DaturaOriginsMod.id("textures/entity/blood_chain.png");
	private static final int GLOW = 0xC0101E;

	private ChainRenderer() {
	}

	public static void render(WorldRenderContext context) {
		ClientWorld world = context.world();
		MatrixStack matrices = context.matrixStack();
		VertexConsumerProvider consumers = context.consumers();
		if (world == null || matrices == null || consumers == null || EntityFlags.CLIENT.isEmpty()) return;
		Vec3d camera = context.camera().getPos();
		float tickDelta = context.tickCounter().getTickDelta(true);
		for (PlayerEntity player : world.getPlayers()) {
			Set<String> flags = EntityFlags.CLIENT.get(player.getId());
			if (flags == null) continue;
			for (String flag : flags) {
				if (!flag.startsWith(DarkCometAbility.CHAIN_PREFIX)) continue;
				Entity target;
				try {
					target = world.getEntityById(Integer.parseInt(flag.substring(DarkCometAbility.CHAIN_PREFIX.length())));
				} catch (NumberFormatException e) {
					continue;
				}
				if (target == null) continue;
				Vec3d from = player.getLerpedPos(tickDelta).add(0, player.getHeight() * 0.55, 0);
				Vec3d to = target.getLerpedPos(tickDelta).add(0, target.getHeight() * 0.5, 0);
				chain(matrices, consumers, world, camera, from, to, player.age + tickDelta);
			}
		}
	}

	private static void chain(MatrixStack matrices, VertexConsumerProvider consumers, ClientWorld world, Vec3d camera, Vec3d a, Vec3d b, float t) {
		a = aboveGround(world, a);
		b = aboveGround(world, b);
		double length = a.distanceTo(b);
		if (length < 0.3) return;
		int links = Math.max(3, (int) Math.round(length / 0.3));
		VertexConsumer buffer = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));
		VertexConsumer glow = consumers.getBuffer(RenderLayer.getLightning());
		int light = WorldRenderer.getLightmapCoordinates(world, BlockPos.ofFloored(a.lerp(b, 0.5)));
		Vec3d prev = a;
		for (int i = 1; i <= links; i++) {
			double u = i / (double) links;
			Vec3d p = a.lerp(b, u);
			Vec3d c = prev.lerp(p, 0.5), d = p.subtract(prev);
			float yaw = (float) Math.toDegrees(Math.atan2(d.x, d.z));
			float pitch = (float) Math.toDegrees(-Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
			matrices.push();
			matrices.translate(c.x - camera.x, c.y - camera.y, c.z - camera.z);
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i % 2 == 0 ? 0 : 90));
			link(matrices.peek(), buffer, light);
			float wave = MathHelper.sin((float) (u * 12 - t * 0.35));
			if (wave > 0.6F) dot(matrices.peek().getPositionMatrix(), glow, 0.09F, (wave - 0.6F) * 1.6F);
			matrices.pop();
			prev = p;
		}
	}

	private static Vec3d aboveGround(ClientWorld world, Vec3d p) {
		BlockPos pos = BlockPos.ofFloored(p);
		VoxelShape shape = world.getBlockState(pos).getCollisionShape(world, pos);
		if (shape.isEmpty()) return p;
		double top = pos.getY() + shape.getMax(Direction.Axis.Y);
		return p.y < top + 0.35 ? new Vec3d(p.x, top + 0.35, p.z) : p;
	}

	private static void link(MatrixStack.Entry entry, VertexConsumer buffer, int light) {
		float l = 0.2F, w = 0.1F, t = 0.035F;
		box(entry, buffer, light, -w, -t, -l, -w + 2 * t, t, l);
		box(entry, buffer, light, w - 2 * t, -t, -l, w, t, l);
		box(entry, buffer, light, -w, -t, -l, w, t, -l + 2 * t);
		box(entry, buffer, light, -w, -t, l - 2 * t, w, t, l);
	}

	private static void box(MatrixStack.Entry entry, VertexConsumer buffer, int light, float x0, float y0, float z0, float x1, float y1, float z1) {
		Matrix4f m = entry.getPositionMatrix();
		float[][] faces = {
				{x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, 0, 0, -1}, {x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1, 0, 0, 1},
				{x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, -1, 0, 0}, {x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, 1, 0, 0},
				{x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0, 1, 0}, {x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, 0, -1, 0}};
		float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
		for (float[] q : faces) {
			for (int k = 0; k < 4; k++) {
				buffer.vertex(m, q[k * 3], q[k * 3 + 1], q[k * 3 + 2]).color(255, 255, 255, 255).texture(uv[k][0], uv[k][1])
						.overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, q[12], q[13], q[14]);
			}
		}
	}

	private static void dot(Matrix4f m, VertexConsumer buffer, float h, float alpha) {
		int r = GLOW >> 16 & 255, g = GLOW >> 8 & 255, b = GLOW & 255, a = (int) (MathHelper.clamp(alpha, 0, 1) * 255);
		float[][] faces = {
				{-h, -h, -h, h, -h, -h, h, h, -h, -h, h, -h}, {h, -h, h, -h, -h, h, -h, h, h, h, h, h},
				{-h, -h, h, -h, -h, -h, -h, h, -h, -h, h, h}, {h, -h, -h, h, -h, h, h, h, h, h, h, -h},
				{-h, h, -h, h, h, -h, h, h, h, -h, h, h}, {-h, -h, h, h, -h, h, h, -h, -h, -h, -h, -h}};
		for (float[] q : faces) {
			for (int k = 0; k < 4; k++) buffer.vertex(m, q[k * 3], q[k * 3 + 1], q[k * 3 + 2]).color(r, g, b, a);
		}
	}
}
