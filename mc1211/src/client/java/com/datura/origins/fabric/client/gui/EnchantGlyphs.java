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
					random.nextFloat() * MathHelper.TAU,
					0.75F + random.nextFloat() * 0.55F,
					0.18F + random.nextFloat() * 0.22F,
					random.nextFloat(),
					0.05F + random.nextFloat() * 0.05F,
					random.nextInt(GLYPHS.length),
					0.75F + random.nextFloat() * 0.25F);
		}
	}

	void render(DrawContext context, float cx, float cy, float unit, float time, float converge, boolean front, float z) {
		MatrixStack matrices = context.getMatrices();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		float pull = Draw.easeInOut(converge);
		for (Glyph glyph : glyphs) {
			float cycle = time * glyph.rise + glyph.offset;
			float life = cycle - MathHelper.floor(cycle);
			float angle = glyph.phase + time * glyph.speed * (1.0F + pull * 5.0F);
			float depth = MathHelper.sin(angle);
			if ((depth >= 0.0F) != front) {
				continue;
			}
			float radius = glyph.radius * unit * 9.5F * (1.0F - pull * 0.85F);
			float x = cx + MathHelper.cos(angle) * radius;
			float y = cy + depth * radius * 0.3F + (0.5F - life) * unit * 9.0F * (1.0F - pull);
			float alpha = MathHelper.sin(life * MathHelper.PI) * (front ? 0.85F : 0.45F) * (1.0F - pull * pull);
			if (alpha < 0.03F) {
				continue;
			}
			float size = Math.max(4.0F, unit * 0.75F) * (front ? 1.0F : 0.8F);
			int index = (glyph.letter + MathHelper.floor(cycle) * 7) % GLYPHS.length;
			float shade = glyph.shade;
			context.setShaderColor(0.9F * shade, 0.88F * shade, shade, alpha);
			matrices.push();
			matrices.translate(x - size / 2.0F, y - size / 2.0F, z);
			matrices.scale(size / 8.0F, size / 8.0F, 1.0F);
			context.drawTexture(GLYPHS[index], 0, 0, 0.0F, 0.0F, 8, 8, 8, 8);
			matrices.pop();
		}
		context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableBlend();
	}

	private record Glyph(float phase, float radius, float speed, float offset, float rise, int letter, float shade) {
	}
}
