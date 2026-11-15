package com.datura.origins.fabric.client.hud;

import com.datura.origins.fabric.DaturaOriginsMod;
import com.datura.origins.fabric.client.model.PartModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public final class CasinoHud {
	private static final Identifier MODEL = DaturaOriginsMod.id("roleta");
	private static final Identifier TEXTURE = DaturaOriginsMod.id("textures/entity/part/roleta.png");
	private static final float CENTER_Y = 36.0F;
	private static final float RADIUS = 27.0F;

	private static long start = -1;
	private static int multiplier;
	private static float spinSeconds;

	private CasinoHud() {
	}

	public static void start(int mult, int ticks) {
		multiplier = mult;
		spinSeconds = Math.max(1.0F, ticks / 20.0F);
		start = System.currentTimeMillis();
	}

	public static void clear() {
		start = -1;
	}

	public static void render(DrawContext context, RenderTickCounter counter) {
		if (start < 0) return;
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) {
			start = -1;
			return;
		}
		float elapsed = (System.currentTimeMillis() - start) / 1000.0F;
		boolean win = multiplier >= 2;
		float hold = win ? 2.6F : 1.6F;
		float total = spinSeconds + hold;
		if (elapsed > total) {
			start = -1;
			return;
		}
		PartModel model = PartModel.get(MODEL);
		if (model.isEmpty()) return;
		String spin = win ? "giro_verde" : "giro_preto";
		float spinLength = model.length(spin);
		model.resetPose();
		if (elapsed < spinSeconds) {
			model.animate(spin, elapsed / spinSeconds * spinLength, 1.0F);
		} else if (win) {
			model.animate(spin, spinLength, 1.0F - MathHelper.clamp((elapsed - spinSeconds) * 3.0F, 0.0F, 1.0F));
			model.animate("vitoria", elapsed - spinSeconds, MathHelper.clamp((elapsed - spinSeconds) * 3.0F, 0.0F, 1.0F));
		} else {
			model.animate(spin, spinLength, 1.0F);
		}

		float fadeIn = MathHelper.clamp(elapsed * 4.0F, 0.0F, 1.0F);
		float fadeOut = MathHelper.clamp((total - elapsed) * 3.0F, 0.0F, 1.0F);
		float appear = Math.min(fadeIn, fadeOut);
		int width = context.getScaledWindowWidth(), height = context.getScaledWindowHeight();
		float size = Math.min(width, height) * 0.62F;
		float scale = size / (RADIUS * 2) * (0.85F + 0.15F * appear);
		float cx = width / 2.0F, cy = height / 2.0F - 8;

		MatrixStack matrices = context.getMatrices();
		DiffuseLighting.method_34742();
		matrices.push();
		matrices.translate(cx, cy, 150.0F);
		matrices.scale(scale, scale, scale);
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0F));
		matrices.translate(0.0F, -CENTER_Y, 0.0F);
		int alpha = Math.round(appear * 255);
		model.render(matrices, context.getVertexConsumers().getBuffer(RenderLayer.getEntityTranslucent(TEXTURE)), 0xF000F0,
				OverlayTexture.DEFAULT_UV, alpha << 24 | 0xFFFFFF);
		context.draw();
		matrices.pop();
		DiffuseLighting.enableGuiDepthLighting();

		Text line;
		int color;
		if (elapsed < spinSeconds) {
			line = Text.translatable("screen.datura_origins.casino.spinning");
			color = 0xE8D7A8;
		} else if (multiplier == 0) {
			line = Text.translatable("screen.datura_origins.casino.lose").formatted(Formatting.BOLD);
			color = 0xFF6B6B;
		} else if (multiplier == 1) {
			line = Text.translatable("screen.datura_origins.casino.even").formatted(Formatting.BOLD);
			color = 0xDDDDDD;
		} else {
			line = Text.translatable("screen.datura_origins.casino.win", multiplier).formatted(Formatting.BOLD);
			color = ((long) (elapsed * 1000) / 150) % 2 == 0 ? 0xFFE066 : 0xFFB020;
		}
		int y = Math.round(cy + size / 2 + 6);
		context.drawText(client.textRenderer, line, Math.round(cx - client.textRenderer.getWidth(line) / 2.0F), y, (alpha << 24) | color, true);
	}
}
