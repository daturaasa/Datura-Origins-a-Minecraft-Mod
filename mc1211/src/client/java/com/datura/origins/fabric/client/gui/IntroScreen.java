package com.datura.origins.fabric.client.gui;

import com.datura.origins.DaturaOrigins;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;

public class IntroScreen extends Screen {
	private static final SoundEvent CHIME = sound("gui.grimoire.ambient");
	private static final SoundEvent SPARKLE = sound("gui.grimoire.sparkle");
	private static final SoundEvent CRACKLE = sound("gui.grimoire.crackle");
	private static final Identifier SHADOW = Identifier.of(DaturaOrigins.MOD_ID, "textures/gui/shadow.png");
	private static final Identifier SPARK = Identifier.of(DaturaOrigins.MOD_ID, "textures/gui/sparkle.png");

	private static final float LEAVE_TIME = 0.34F;

	private final Backdrop backdrop = new Backdrop(Backdrop.INTRO);
	private final EnchantGlyphs glyphs = new EnchantGlyphs(30);
	private final List<Spark> sparks = new ArrayList<>();
	private final Random random = Random.create();
	private final long openedAt = Util.getMeasuringTimeMs();
	private long clickedAt = -1L;
	private float nextSpark = 0.6F;
	private float nextCrackle = 1.8F;
	private float lastTime;
	private float spin;
	private float hover;
	private float lookX;
	private float lookY;
	private boolean shown;
	private boolean finished;
	private float bookX;
	private float bookY;
	private float unit;

	public IntroScreen() {
		super(Text.translatable("screen.datura_origins.intro"));
	}

	private static SoundEvent sound(String path) {
		return SoundEvent.of(Identifier.of(DaturaOrigins.MOD_ID, path));
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		backdrop.render(context, width, height, seconds(), mouseX, mouseY);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		float time = seconds();
		float step = MathHelper.clamp(time - lastTime, 0.0F, 0.1F);
		lastTime = time;
		float t = leaving();
		renderBackground(context, mouseX, mouseY, delta);

		unit = Math.max(1.6F, Math.min(height * 0.0185F, width * 0.016F));
		bookX = width / 2.0F;
		bookY = height / 2.0F - unit * 0.5F;

		float enter = Draw.easeOut(time / 1.4F);
		float leave = t < 0 ? 0.0F : Draw.easeInOut(t / LEAVE_TIME);
		hover = MathHelper.lerp(Math.min(1.0F, step * 8.0F), hover, t < 0 && isOverBook(mouseX, mouseY) ? 1.0F : 0.0F);
		lookX = MathHelper.lerp(Math.min(1.0F, step * 3.0F), lookX, MathHelper.clamp((mouseX - bookX) / (width * 0.5F), -1.0F, 1.0F));
		lookY = MathHelper.lerp(Math.min(1.0F, step * 3.0F), lookY, MathHelper.clamp((mouseY - bookY) / (height * 0.5F), -1.0F, 1.0F));
		spin += step * (0.07F + hover * 0.05F + leave * 1.6F);

		float bob = MathHelper.sin(time * 1.1F) * unit * 0.45F - hover * unit * 0.35F;
		float radius = unit * 13.5F;

		renderShadow(context, bob, enter);
		glyphs.render(context, bookX, bookY, radius, Math.max(5.0F, unit * 0.95F), spin, time, enter * (0.75F + 0.25F * hover) * (1.0F - leave * 0.6F), 60.0F);
		updateSparks(time, radius);
		renderSparks(context, time, enter);
		renderBook(context, time, bob, enter, leave);

		float hint = Draw.easeOut((time - 2.6F) / 1.2F) * (1.0F - hover) * (t < 0 ? 1.0F : 0.0F);
		if (hint > 0.02F) {
			float breathe = 0.55F + 0.2F * MathHelper.sin(time * 1.3F);
			Draw.centered(context, textRenderer, Text.translatable("screen.datura_origins.intro.hint"), width / 2.0F,
					height - Math.max(22.0F, height * 0.09F), Draw.fade(0xFFD9C7D0, hint * breathe), 1.0F, false);
		}

		if (leave > 0.0F) {
			context.getMatrices().push();
			context.getMatrices().translate(0.0F, 0.0F, 900.0F);
			Draw.rect(context, 0, 0, width, height, Draw.fade(0xFFF3E6EC, leave * 0.55F));
			context.getMatrices().pop();
		}

		if (!shown) {
			shown = true;
			GrimoireHook.current().onBookShown(this);
		}
		playAmbience(time, t);
		if (t >= LEAVE_TIME && !finished && client != null) {
			finished = true;
			GrimoireHook.current().onOpened(this);
			client.setScreen(new OriginSelectionScreen(true));
		}
	}

	private void renderShadow(DrawContext context, float bob, float enter) {
		float lift = MathHelper.clamp(-bob / (unit * 0.8F), -1.0F, 1.0F);
		float w = unit * (12.0F - lift * 1.2F);
		float h = w * 0.22F;
		float y = bookY + unit * 9.6F;
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		context.setShaderColor(1.0F, 1.0F, 1.0F, enter * (0.55F - lift * 0.08F));
		context.getMatrices().push();
		context.getMatrices().translate(bookX - w / 2.0F, y - h / 2.0F, 40.0F);
		context.getMatrices().scale(w / 96.0F, h / 96.0F, 1.0F);
		context.drawTexture(SHADOW, 0, 0, 0.0F, 0.0F, 96, 96, 96, 96);
		context.getMatrices().pop();
		context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableBlend();
	}

	private void renderBook(DrawContext context, float time, float bob, float enter, float leave) {
		float yaw = -8.0F + MathHelper.sin(time * 0.4F) * 5.0F + lookX * 9.0F;
		float tilt = 48.0F + MathHelper.sin(time * 0.65F + 1.3F) * 1.5F + lookY * 5.0F;
		float scale = unit * (0.92F + 0.08F * enter) * (1.0F + hover * 0.03F + leave * 0.05F);
		float glow = 0.6F + 0.2F * MathHelper.sin(time * 1.4F) + 0.2F * hover;

		MatrixStack matrices = context.getMatrices();
		matrices.push();
		matrices.translate(bookX, bookY + bob, 400.0F);
		matrices.scale(scale, -scale, scale);
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(tilt));
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
		matrices.translate(-8.25F, -7.0F, -7.55F);
		GrimoireModel.get().render(matrices, context.getVertexConsumers(), time * 20.0F, 0.0F, 1.0F, 0.0F, glow * enter);
		context.draw();
		matrices.pop();
	}

	private void updateSparks(float time, float radius) {
		sparks.removeIf(spark -> time - spark.born > spark.life);
		if (time < nextSpark || clickedAt >= 0L) {
			return;
		}
		nextSpark = time + 0.35F + random.nextFloat() * 0.65F;
		float angle = random.nextFloat() * MathHelper.TAU;
		float r = radius * (0.92F + random.nextFloat() * 0.22F);
		sparks.add(new Spark(MathHelper.cos(angle) * r, MathHelper.sin(angle) * r, time, 0.6F + random.nextFloat() * 0.5F,
				0.6F + random.nextFloat() * 0.6F));
		if (random.nextFloat() < 0.3F && time > 1.0F) {
			play(SPARKLE, 0.85F + random.nextFloat() * 0.45F, 0.1F + random.nextFloat() * 0.08F);
		}
	}

	private void renderSparks(DrawContext context, float time, float enter) {
		if (sparks.isEmpty()) {
			return;
		}
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
		for (Spark spark : sparks) {
			float age = (time - spark.born) / spark.life;
			float a = MathHelper.sin(age * MathHelper.PI) * enter;
			float s = unit * 1.6F * spark.size * (0.6F + 0.4F * a);
			context.setShaderColor(1.0F, 0.9F, 0.96F, a * 0.85F);
			context.getMatrices().push();
			context.getMatrices().translate(bookX + spark.x - s / 2.0F, bookY + spark.y - s / 2.0F - age * unit * 0.8F, 70.0F);
			context.getMatrices().scale(s / 32.0F, s / 32.0F, 1.0F);
			context.drawTexture(SPARK, 0, 0, 0.0F, 0.0F, 32, 32, 32, 32);
			context.getMatrices().pop();
		}
		context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
	}

	private void playAmbience(float time, float t) {
		if (t >= 0 || client == null || time < nextCrackle) {
			return;
		}
		nextCrackle = time + 2.4F + random.nextFloat() * 2.6F;
		play(CRACKLE, 0.85F + random.nextFloat() * 0.35F, 0.12F + random.nextFloat() * 0.08F);
	}

	private void play(SoundEvent sound, float pitch, float volume) {
		if (client != null) {
			client.getSoundManager().play(PositionedSoundInstance.master(sound, pitch, volume));
		}
	}

	private boolean isOverBook(double mouseX, double mouseY) {
		double dx = (mouseX - bookX) / (unit * 10.0F);
		double dy = (mouseY - bookY) / (unit * 9.0F);
		return dx * dx + dy * dy <= 1.0;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && clickedAt < 0L && isOverBook(mouseX, mouseY) && GrimoireHook.current().beforeOpen(this)) {
			clickedAt = Util.getMeasuringTimeMs();
			play(CHIME, 1.25F, 0.4F);
			play(SPARKLE, 1.1F, 0.3F);
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private float seconds() {
		return (Util.getMeasuringTimeMs() - openedAt) / 1000.0F;
	}

	private float leaving() {
		return clickedAt < 0L ? -1.0F : (Util.getMeasuringTimeMs() - clickedAt) / 1000.0F;
	}

	private record Spark(float x, float y, float born, float life, float size) {
	}
}
