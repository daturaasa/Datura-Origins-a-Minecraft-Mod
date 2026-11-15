package com.datura.origins.fabric.client.render;

import com.datura.origins.fabric.DaturaOriginsMod;
import com.datura.origins.fabric.entity.SpellProjectileEntity;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class SpellProjectileRenderer extends EntityRenderer<SpellProjectileEntity> {
	private static final Identifier SUN = DaturaOriginsMod.id("textures/entity/sun_cube.png");
	private static final Identifier MOON = DaturaOriginsMod.id("textures/entity/moon_cube.png");
	private static final float[][] FACES = {
			{0, 0, -1, -1, 0, 0, 0, 1, 0}, {0, 0, 1, 1, 0, 0, 0, 1, 0}, {-1, 0, 0, 0, 0, 1, 0, 1, 0},
			{1, 0, 0, 0, 0, -1, 0, 1, 0}, {0, 1, 0, 1, 0, 0, 0, 0, -1}, {0, -1, 0, 1, 0, 0, 0, 0, 1}};

	public SpellProjectileRenderer(EntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public void render(SpellProjectileEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
		float t = entity.age + tickDelta;
		if (entity.kind() == SpellProjectileEntity.CAULDRON) {
			matrices.push();
			matrices.translate(0.0F, 0.4F, 0.0F);
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 9.0F));
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(t * 14.0F));
			matrices.scale(0.8F, 0.8F, 0.8F);
			matrices.translate(-0.5F, -0.5F, -0.5F);
			MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(Blocks.CAULDRON.getDefaultState(), matrices,
					vertexConsumers, light, OverlayTexture.DEFAULT_UV);
			matrices.pop();
			super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
			return;
		}
		if (entity.kind() == SpellProjectileEntity.MANA) {
			mana(matrices, vertexConsumers, t, 0.45F * entity.size());
			super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
			return;
		}
		if (entity.kind() == SpellProjectileEntity.MOON) {
			cube(matrices, vertexConsumers, MOON, t, 1.1f * entity.size(), 0xEEF2FF, 0.35f, 0.8f);
		} else {
			cube(matrices, vertexConsumers, SUN, t, 0.75f * entity.size(), 0xFFD24A, 0.32f, 2.2f);
		}
		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
	}

	@Override
	public Identifier getTexture(SpellProjectileEntity entity) {
		return entity.kind() == SpellProjectileEntity.MOON ? MOON : SUN;
	}

	private static void cube(MatrixStack ms, VertexConsumerProvider vcp, Identifier tex, float t, float side, int glow, float glowA, float spin) {
		float h = side / 2;
		ms.push();
		ms.translate(0, 0.3f, 0);
		ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 3.1f * spin));
		ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(t * 2.3f * spin));
		ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(t * 1.7f * spin));
		MatrixStack.Entry e = ms.peek();
		texBox(e, vcp.getBuffer(RenderLayer.getEntityTranslucentEmissive(tex)), h);
		VertexConsumer g = vcp.getBuffer(RenderLayer.getLightning());
		float pulse = 0.85f + 0.15f * MathHelper.sin(t * 0.25f);
		glowBox(e.getPositionMatrix(), g, h * 1.12f, glow, glowA * pulse);
		glowBox(e.getPositionMatrix(), g, h * 1.3f, glow, glowA * 0.45f * pulse);
		ms.pop();
	}

	private static void mana(MatrixStack ms, VertexConsumerProvider vcp, float t, float side) {
		float h = side / 2;
		float pulse = 0.85F + 0.15F * MathHelper.sin(t * 0.3F);
		ms.push();
		ms.translate(0, 0.3F, 0);
		ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 9.0F));
		ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(t * 6.0F));
		Matrix4f m = ms.peek().getPositionMatrix();
		VertexConsumer g = vcp.getBuffer(RenderLayer.getLightning());
		glowBox(m, g, h * 0.45F, 0xFFFFFF, 0.9F);
		glowBox(m, g, h * 0.8F, 0x7FD6FF, 0.55F * pulse);
		glowBox(m, g, h * 1.15F, 0x4A9CFF, 0.3F * pulse);
		glowBox(m, g, h * 1.5F, 0x4A9CFF, 0.12F * pulse);
		ms.pop();
	}

	private static void texBox(MatrixStack.Entry e, VertexConsumer vc, float h) {
		Matrix4f m = e.getPositionMatrix();
		float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
		for (float[] f : FACES) {
			float[][] c = corners(f, h);
			for (int k = 0; k < 4; k++) {
				vc.vertex(m, c[k][0], c[k][1], c[k][2]).color(255, 255, 255, 255).texture(uv[k][0], uv[k][1])
						.overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(e, f[0], f[1], f[2]);
			}
		}
	}

	private static void glowBox(Matrix4f m, VertexConsumer vc, float h, int rgb, float a) {
		int r = rgb >> 16 & 255, gg = rgb >> 8 & 255, b = rgb & 255, al = (int) (MathHelper.clamp(a, 0, 1) * 255);
		for (float[] f : FACES) {
			float[][] c = corners(f, h);
			for (int k = 0; k < 4; k++) vc.vertex(m, c[k][0], c[k][1], c[k][2]).color(r, gg, b, al);
		}
	}

	private static float[][] corners(float[] f, float h) {
		Vector3f n = new Vector3f(f[0], f[1], f[2]), u = new Vector3f(f[3], f[4], f[5]), v = new Vector3f(f[6], f[7], f[8]);
		float[][] out = new float[4][];
		float[][] s = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
		for (int k = 0; k < 4; k++) {
			Vector3f p = new Vector3f(n).mul(h).add(new Vector3f(u).mul(s[k][0] * h)).add(new Vector3f(v).mul(s[k][1] * h));
			out[k] = new float[]{p.x, p.y, p.z};
		}
		return out;
	}
}
