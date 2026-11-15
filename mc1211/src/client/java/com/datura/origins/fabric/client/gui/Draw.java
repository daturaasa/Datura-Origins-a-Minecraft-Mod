package com.datura.origins.fabric.client.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

final class Draw {
	static final int GOLD = 0xFFC9A75A;
	static final int GOLD_DIM = 0xFF8C7340;
	static final int TEXT = 0xFFC9CEC8;
	static final int TEXT_MUTED = 0xFF8D978F;
	static final int TEXT_BRIGHT = 0xFFF3EEE4;
	static final int GREEN = 0xFF86D69B;
	static final int PINK = 0xFFE58AB0;
	static final int RED = 0xFFE27D93;
	static final int PANEL = 0xC00B1712;
	static final int BUTTON_BORDER = 0xFF8E3A5C;

	private Draw() {
	}

	static void quad(DrawContext context, float x0, float y0, float x1, float y1, int topLeft, int topRight, int bottomRight, int bottomLeft) {
		Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
		VertexConsumer buffer = context.getVertexConsumers().getBuffer(RenderLayer.getGui());
		buffer.vertex(matrix, x0, y0, 0.0F).color(topLeft);
		buffer.vertex(matrix, x0, y1, 0.0F).color(bottomLeft);
		buffer.vertex(matrix, x1, y1, 0.0F).color(bottomRight);
		buffer.vertex(matrix, x1, y0, 0.0F).color(topRight);
		context.draw();
	}

	static void rect(DrawContext context, float x0, float y0, float x1, float y1, int color) {
		quad(context, x0, y0, x1, y1, color, color, color, color);
	}

	static void vGradient(DrawContext context, float x0, float y0, float x1, float y1, int top, int bottom) {
		quad(context, x0, y0, x1, y1, top, top, bottom, bottom);
	}

	static void hGradient(DrawContext context, float x0, float y0, float x1, float y1, int left, int right) {
		quad(context, x0, y0, x1, y1, left, right, right, left);
	}

	static void radial(DrawContext context, float cx, float cy, float rx, float ry, int center, int edge) {
		Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
		VertexConsumer buffer = context.getVertexConsumers().getBuffer(RenderLayer.getGui());
		int segments = 32;
		float step = MathHelper.TAU / segments;
		for (int i = 0; i < segments; i++) {
			float a0 = -i * step;
			float a1 = -(i + 1) * step;
			buffer.vertex(matrix, cx, cy, 0.0F).color(center);
			buffer.vertex(matrix, cx, cy, 0.0F).color(center);
			buffer.vertex(matrix, cx + MathHelper.cos(a0) * rx, cy + MathHelper.sin(a0) * ry, 0.0F).color(edge);
			buffer.vertex(matrix, cx + MathHelper.cos(a1) * rx, cy + MathHelper.sin(a1) * ry, 0.0F).color(edge);
		}
		context.draw();
	}

	static void glow(DrawContext context, float cx, float cy, float rx, float ry, int color) {
		radial(context, cx, cy, rx, ry, color, color & 0x00FFFFFF);
	}

	static void frame(DrawContext context, int x0, int y0, int x1, int y1, int color) {
		context.fill(x0, y0, x1, y0 + 1, color);
		context.fill(x0, y1 - 1, x1, y1, color);
		context.fill(x0, y0 + 1, x0 + 1, y1 - 1, color);
		context.fill(x1 - 1, y0 + 1, x1, y1 - 1, color);
	}

	static void ornamentFrame(DrawContext context, int x0, int y0, int x1, int y1, float alpha) {
		context.fill(x0, y0, x1, y1, fade(PANEL, alpha));
		frame(context, x0, y0, x1, y1, fade(GOLD_DIM, alpha * 0.55F));
		int corner = fade(GOLD, alpha);
		int len = 6;
		context.fill(x0, y0, x0 + len, y0 + 1, corner);
		context.fill(x0, y0, x0 + 1, y0 + len, corner);
		context.fill(x1 - len, y0, x1, y0 + 1, corner);
		context.fill(x1 - 1, y0, x1, y0 + len, corner);
		context.fill(x0, y1 - 1, x0 + len, y1, corner);
		context.fill(x0, y1 - len, x0 + 1, y1, corner);
		context.fill(x1 - len, y1 - 1, x1, y1, corner);
		context.fill(x1 - 1, y1 - len, x1, y1, corner);
	}

	static void divider(DrawContext context, int x0, int x1, int y, int color) {
		int mid = (x0 + x1) / 2;
		hGradient(context, x0, y, mid, y + 1, color & 0x00FFFFFF, color);
		hGradient(context, mid, y, x1, y + 1, color, color & 0x00FFFFFF);
	}

	static void diamond(DrawContext context, int cx, int cy, int color) {
		context.fill(cx - 1, cy - 2, cx + 1, cy + 2, color);
		context.fill(cx - 2, cy - 1, cx + 2, cy + 1, color);
	}

	static void text(DrawContext context, TextRenderer font, OrderedText text, float x, float y, int color, float scale, boolean shadow) {
		if (alphaOf(color) < 0.02F) {
			return;
		}
		context.getMatrices().push();
		context.getMatrices().translate(x, y, 0.0F);
		context.getMatrices().scale(scale, scale, 1.0F);
		context.drawText(font, text, 0, 0, color, shadow);
		context.getMatrices().pop();
	}

	static void text(DrawContext context, TextRenderer font, Text text, float x, float y, int color, float scale, boolean shadow) {
		text(context, font, text.asOrderedText(), x, y, color, scale, shadow);
	}

	static void centered(DrawContext context, TextRenderer font, Text text, float cx, float y, int color, float scale, boolean shadow) {
		text(context, font, text, cx - font.getWidth(text) * scale / 2.0F, y, color, scale, shadow);
	}

	static Text spaced(String text) {
		StringBuilder out = new StringBuilder(text.length() * 2);
		for (int i = 0; i < text.length(); i++) {
			if (i > 0) {
				out.append(' ');
			}
			out.append(text.charAt(i));
		}
		return Text.literal(out.toString());
	}

	static int fade(int color, float alpha) {
		int a = Math.round(((color >>> 24) & 0xFF) * MathHelper.clamp(alpha, 0.0F, 1.0F));
		if (a < 5) {
			a = 0;
		}
		return (a << 24) | (color & 0x00FFFFFF);
	}

	static float alphaOf(int color) {
		return ((color >>> 24) & 0xFF) / 255.0F;
	}

	static int mix(int a, int b, float t) {
		t = MathHelper.clamp(t, 0.0F, 1.0F);
		int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
		int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
		return (Math.round(aa + (ba - aa) * t) << 24)
				| (Math.round(ar + (br - ar) * t) << 16)
				| (Math.round(ag + (bg - ag) * t) << 8)
				| Math.round(ab + (bb - ab) * t);
	}

	static float easeOut(float t) {
		t = MathHelper.clamp(t, 0.0F, 1.0F);
		return 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
	}

	static float easeInOut(float t) {
		t = MathHelper.clamp(t, 0.0F, 1.0F);
		return t * t * (3.0F - 2.0F * t);
	}

	static float easeOutBack(float t) {
		t = MathHelper.clamp(t, 0.0F, 1.0F);
		float c1 = 1.4F;
		float c3 = c1 + 1.0F;
		float u = t - 1.0F;
		return 1.0F + c3 * u * u * u + c1 * u * u;
	}
}
