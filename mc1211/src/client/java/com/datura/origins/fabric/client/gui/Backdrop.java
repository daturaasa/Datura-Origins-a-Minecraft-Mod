package com.datura.origins.fabric.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

final class Backdrop {
	static final Style INTRO = new Style(0xFF0C241B, 0xFF173A2B, 0xFF3E1C33, 0x5A2F8F64, 0x55C25A8C, 0xFFF4D6E6, 70, 1.0F);
	static final Style SELECTION = new Style(0xFF091913, 0xFF0E241B, 0xFF4A2338, 0x38297552, 0x50B4537F, 0xFFEAD9C2, 38, 0.55F);

	private final Style style;
	private final Mote[] motes;

	Backdrop(Style style) {
		this.style = style;
		Random random = Random.create(0x6461747572L);
		this.motes = new Mote[style.motes];
		for (int i = 0; i < motes.length; i++) {
			int layer = i % 3;
			motes[i] = new Mote(
					random.nextFloat(),
					random.nextFloat(),
					0.004F + layer * 0.006F + random.nextFloat() * 0.004F,
					random.nextFloat() * MathHelper.TAU,
					0.6F + random.nextFloat() * 1.4F,
					layer);
		}
	}

	void render(DrawContext context, int width, int height, float time) {
		int split = Math.round(height * 0.55F);
		Draw.vGradient(context, 0, 0, width, split, style.top | 0xFF000000, style.middle);
		Draw.vGradient(context, 0, split, width, height, style.middle, style.bottom);

		float breathe = 0.85F + 0.15F * MathHelper.sin(time * 0.35F);
		Draw.glow(context, width * 0.18F, height * 0.12F, width * 0.55F, height * 0.6F, Draw.fade(style.glowTop, breathe));
		Draw.glow(context, width * 0.82F, height * 1.02F, width * 0.7F, height * 0.55F, Draw.fade(style.glowBottom, breathe));
		Draw.glow(context, width * 0.5F, height * 0.98F, width * 0.6F, height * 0.32F, Draw.fade(style.glowBottom, 0.7F));

		drawMist(context, width, height, time);
		drawMotes(context, width, height, time);
		drawVignette(context, width, height);
	}

	private void drawMist(DrawContext context, int width, int height, float time) {
		for (int i = 0; i < 3; i++) {
			float y = height * (0.58F + i * 0.13F) + MathHelper.sin(time * 0.2F + i * 1.7F) * height * 0.015F;
			float h = height * (0.06F + i * 0.02F);
			int color = Draw.fade(Draw.mix(style.middle, style.bottom, 0.5F + i * 0.2F), 0.10F + i * 0.04F);
			Draw.vGradient(context, 0, y - h, width, y, color & 0x00FFFFFF, color);
			Draw.vGradient(context, 0, y, width, y + h, color, color & 0x00FFFFFF);
		}
	}

	private void drawMotes(DrawContext context, int width, int height, float time) {
		for (Mote mote : motes) {
			float speed = mote.speed * style.motion;
			float y = mote.y - time * speed;
			y -= MathHelper.floor(y);
			float sway = MathHelper.sin(time * 0.6F + mote.phase) * 0.012F * (mote.layer + 1);
			float x = mote.x + sway;
			x -= MathHelper.floor(x);
			float twinkle = 0.45F + 0.55F * MathHelper.sin(time * (0.8F + mote.layer * 0.4F) + mote.phase);
			float edge = MathHelper.clamp(y * 6.0F, 0.0F, 1.0F) * MathHelper.clamp((1.0F - y) * 4.0F, 0.0F, 1.0F);
			float alpha = (0.12F + mote.layer * 0.14F) * twinkle * edge * style.motion;
			if (alpha < 0.03F) {
				continue;
			}
			float px = x * width;
			float py = y * height;
			float size = mote.size * (0.6F + mote.layer * 0.45F);
			if (mote.layer == 2) {
				Draw.glow(context, px, py, size * 4.0F, size * 4.0F, Draw.fade(style.mote, alpha * 0.35F));
			}
			Draw.rect(context, px - size / 2.0F, py - size / 2.0F, px + size / 2.0F, py + size / 2.0F, Draw.fade(style.mote, alpha));
		}
	}

	private static void drawVignette(DrawContext context, int width, int height) {
		int dark = 0xB0030806;
		int clear = 0x00030806;
		float vertical = height * 0.22F;
		float horizontal = width * 0.18F;
		Draw.vGradient(context, 0, 0, width, vertical, dark, clear);
		Draw.vGradient(context, 0, height - vertical, width, height, clear, dark);
		Draw.hGradient(context, 0, 0, horizontal, height, dark, clear);
		Draw.hGradient(context, width - horizontal, 0, width, height, clear, dark);
	}

	record Style(int top, int middle, int bottom, int glowTop, int glowBottom, int mote, int motes, float motion) {
	}

	private record Mote(float x, float y, float speed, float phase, float size, int layer) {
	}
}
