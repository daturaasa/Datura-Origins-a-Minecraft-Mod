package com.datura.origins.fabric.client.render;

import com.datura.origins.fabric.power.EntityFlags;
import com.datura.origins.fabric.power.types.LilyPadPower;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public final class LilyPadRenderer {
	private LilyPadRenderer() {
	}

	public static void render(WorldRenderContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null || context.consumers() == null) return;
		MatrixStack matrices = context.matrixStack();
		VertexConsumerProvider consumers = context.consumers();
		Vec3d camera = context.camera().getPos();
		float tickDelta = context.tickCounter().getTickDelta(true);
		for (PlayerEntity player : client.world.getPlayers()) {
			if (!EntityFlags.clientHas(player, LilyPadPower.FLAG)) continue;
			Vec3d pos = player.getLerpedPos(tickDelta);
			float t = player.age + tickDelta;
			float size = 1.1F + player.getScale() * 0.5F;
			matrices.push();
			matrices.translate(pos.x - camera.x, pos.y - camera.y - 0.05 + 0.012 * MathHelper.sin(t * 0.12F), pos.z - camera.z);
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw) + 15));
			matrices.scale(size, 1.0F, size);
			matrices.translate(-0.5F, 0.0F, -0.5F);
			int light = WorldRenderer.getLightmapCoordinates(client.world, player.getBlockPos());
			client.getBlockRenderManager().renderBlockAsEntity(Blocks.LILY_PAD.getDefaultState(), matrices, consumers, light, OverlayTexture.DEFAULT_UV);
			matrices.pop();
		}
	}
}
