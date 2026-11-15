package com.datura.origins.fabric.client.gui;

import com.datura.origins.DaturaOrigins;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.Resource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.io.InputStream;
import java.util.Optional;

final class TitleBanner {
	private static final Identifier TEXTURE = Identifier.of(DaturaOrigins.MOD_ID, "textures/gui/title.png");
	private static final int OUTLINE = 0xFF07140E;
	private static final int[] DEPTH = {0xFF2A6B4B, 0xFF1F5139, 0xFF153A29};
	private static final int FACE = 0xFFE8F1E2;
	private static final int FACE_SHADE = 0xFFB9D3BE;

	private static boolean probed;
	private static int textureWidth;
	private static int textureHeight;

	private TitleBanner() {
	}

	static int render(DrawContext context, TextRenderer font, int centerX, int top, int maxWidth, int maxHeight, float alpha) {
		probe();
		if (textureWidth > 0) {
			float scale = Math.min(maxWidth / (float) textureWidth, maxHeight / (float) textureHeight);
			int w = Math.round(textureWidth * scale);
			int h = Math.round(textureHeight * scale);
			RenderSystem.enableBlend();
			context.setShaderColor(1.0F, 1.0F, 1.0F, MathHelper.clamp(alpha, 0.0F, 1.0F));
			context.drawTexture(TEXTURE, centerX - w / 2, top, w, h, 0.0F, 0.0F, textureWidth, textureHeight, textureWidth, textureHeight);
			context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.disableBlend();
			return top + h;
		}
		return drawFallback(context, font, centerX, top, maxWidth, maxHeight, alpha);
	}

	private static int drawFallback(DrawContext context, TextRenderer font, int centerX, int top, int maxWidth, int maxHeight, float alpha) {
		Text name = Text.literal("DATURA");
		int nameWidth = font.getWidth(name);
		int scale = MathHelper.clamp(Math.min((int) (maxWidth * 0.8F / nameWidth), (int) (maxHeight * 0.62F / 12.0F)), 1, 8);
		float x = centerX - nameWidth * scale / 2.0F;
		float y = top;

		for (int i = DEPTH.length; i >= 1; i--) {
			drawOutlined(context, font, name, x, y + i * scale * 0.5F, scale, Draw.fade(DEPTH[i - 1], alpha), Draw.fade(OUTLINE, alpha));
		}
		drawOutlined(context, font, name, x, y, scale, Draw.fade(FACE, alpha), Draw.fade(OUTLINE, alpha));
		context.enableScissor(0, Math.round(y + 4.0F * scale), context.getScaledWindowWidth(), Math.round(y + 8.0F * scale));
		Draw.text(context, font, name, x, y, Draw.fade(FACE_SHADE, alpha), scale, false);
		context.disableScissor();

		float ribbonY = y + 8.0F * scale + DEPTH.length * scale * 0.5F + Math.max(3, scale);
		int ribbonScale = Math.max(1, scale / 3);
		Text sub = Draw.spaced("ORIGINS");
		int subWidth = font.getWidth(sub) * ribbonScale;
		int padX = 8 * ribbonScale;
		int x0 = Math.round(centerX - subWidth / 2.0F - padX);
		int x1 = Math.round(centerX + subWidth / 2.0F + padX);
		int y0 = Math.round(ribbonY);
		int y1 = y0 + 9 * ribbonScale + 4;
		context.fill(x0, y0, x1, y1, Draw.fade(0xE0301424, alpha));
		Draw.frame(context, x0, y0, x1, y1, Draw.fade(Draw.BUTTON_BORDER, alpha));
		Draw.diamond(context, x0, (y0 + y1) / 2, Draw.fade(Draw.GOLD, alpha));
		Draw.diamond(context, x1, (y0 + y1) / 2, Draw.fade(Draw.GOLD, alpha));
		Draw.centered(context, font, sub, centerX, y0 + 3, Draw.fade(Draw.GOLD, alpha), ribbonScale, true);
		return y1;
	}

	private static void drawOutlined(DrawContext context, TextRenderer font, Text text, float x, float y, float scale, int color, int outline) {
		float o = Math.max(1.0F, scale / 2.0F);
		Draw.text(context, font, text, x - o, y, outline, scale, false);
		Draw.text(context, font, text, x + o, y, outline, scale, false);
		Draw.text(context, font, text, x, y - o, outline, scale, false);
		Draw.text(context, font, text, x, y + o, outline, scale, false);
		Draw.text(context, font, text, x, y, color, scale, false);
	}

	private static void probe() {
		if (probed) {
			return;
		}
		probed = true;
		Optional<Resource> resource = MinecraftClient.getInstance().getResourceManager().getResource(TEXTURE);
		if (resource.isEmpty()) {
			return;
		}
		try (InputStream stream = resource.get().getInputStream(); NativeImage image = NativeImage.read(stream)) {
			textureWidth = image.getWidth();
			textureHeight = image.getHeight();
		} catch (Exception e) {
			textureWidth = 0;
		}
	}
}
