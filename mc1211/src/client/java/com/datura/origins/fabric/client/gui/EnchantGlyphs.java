package com.datura.origins.fabric.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

final class EnchantGlyphs {
	private static final Identifier[] GLYPHS = new Identifier[26];

	static {
		for (int i = 0; i < GLYPHS.length; i++) {
			GLYPHS[i] = Identifier.ofVanilla("textures/particle/sga_" + (char) ('a' + i) + ".png");
		}
	}

	private final Glyph[] glyphs;

	EnchantGlyphs(int count) {
		Random random = Random.create(0x736761L);
		glyphs = new Glyph[count];
		for (int i = 0; i < count; i++) {
			glyphs[i] = new Glyph(
					i * MathHelper.TAU / count + (random.nextFloat() - 0.5F) * 0.08F,
					random.nextFloat() * MathHelper.TAU,
					0.18F + random.nextFloat() * 0.16F,
					random.nextInt(GLYPHS.length),
					0.82F + random.nextFloat() * 0.18F);
		}
	}

	void render(DrawContext context, float cx, float cy, float radius, float size, float spin, float time, float alpha, float z) {
		if (alpha <= 0.01F) {
			return;
		}
		MatrixStack matrices = context.getMatrices();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		for (Glyph glyph : glyphs) {
			float angle = glyph.angle + spin;
			float cycle = time * glyph.pulse + glyph.phase / MathHelper.TAU;
			float wave = 0.5F - 0.5F * MathHelper.cos(cycle * MathHelper.TAU);
			float a = alpha * (0.18F + 0.62F * wave * wave);
			if (a < 0.02F) {
				continue;
			}
			float r = radius * (1.0F + 0.025F * MathHelper.sin(time * 0.7F + glyph.phase));
			float x = cx + MathHelper.cos(angle) * r;
			float y = cy + MathHelper.sin(angle) * r;
			float s = size * (0.86F + 0.14F * wave);
			int index = Math.floorMod(glyph.letter + MathHelper.floor(cycle) * 7, GLYPHS.length);
			float shade = glyph.shade;
			context.setShaderColor(0.97F * shade, 0.86F * shade, 0.93F * shade, a);
			matrices.push();
			matrices.translate(x - s / 2.0F, y - s / 2.0F, z);
			matrices.scale(s / 8.0F, s / 8.0F, 1.0F);
			context.drawTexture(GLYPHS[index], 0, 0, 0.0F, 0.0F, 8, 8, 8, 8);
			matrices.pop();
		}
		context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableBlend();
	}

	private record Glyph(float angle, float phase, float pulse, int letter, float shade) {
	}
}
