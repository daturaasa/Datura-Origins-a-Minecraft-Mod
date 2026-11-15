package com.datura.origins.fabric.client.gui;

import com.datura.origins.DaturaOrigins;
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
	private static final SoundEvent AMBIENT = sound("gui.grimoire.ambient");
	private static final SoundEvent TOUCH = sound("gui.grimoire.touch");
	private static final SoundEvent PAGE = sound("gui.grimoire.page");
	private static final SoundEvent REVEAL = sound("gui.grimoire.reveal");

	private static final float STRAP_END = 0.45F;
	private static final float COVER_START = 0.3F;
	private static final float COVER_TIME = 1.1F;
	private static final float FLASH_START = 1.45F;
	private static final float FLASH_TIME = 0.7F;
	private static final float SWITCH_AT = FLASH_START + FLASH_TIME;

	private final Backdrop backdrop = new Backdrop(Backdrop.INTRO);
	private final EnchantGlyphs glyphs = new EnchantGlyphs(34);
	private final Random random = Random.create();
	private final long openedAt = Util.getMeasuringTimeMs();
	private long clickedAt = -1L;
	private float nextAmbient = 0.8F;
	private boolean shown;
	private boolean finished;
	private int cues;
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
		backdrop.render(context, width, height, seconds());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		float time = seconds();
		float t = opening();
		renderBackground(context, mouseX, mouseY, delta);

		float enter = Draw.easeOut(time / 1.2F);
		float flash = t < 0 ? 0.0F : Draw.easeInOut((t - FLASH_START) / FLASH_TIME);

		unit = Math.max(2.0F, Math.min(height * 0.042F, width * 0.034F));
		bookX = width / 2.0F;
		bookY = height / 2.0F;

		float open = t < 0 ? 0.0F : Draw.easeInOut((t - COVER_START) / 1.2F);
		float pages = t < 0 ? 0.0F : Draw.easeOut((t - 0.6F) / 0.6F);
		float converge = t < 0 ? 0.0F : MathHelper.clamp(t / 1.2F, 0.0F, 1.0F);
		float breathe = 0.5F + 0.5F * MathHelper.sin(time * 0.9F);

		Draw.glow(context, bookX, bookY, unit * 12.0F, unit * 8.5F,
				Draw.fade(0xFFC25A8C, (0.18F + 0.06F * breathe + pages * 0.25F) * enter));
		Draw.glow(context, bookX, bookY + unit * 5.5F, unit * 8.0F, unit * 2.0F, Draw.fade(0xFF020604, 0.5F * enter));

		glyphs.render(context, bookX, bookY, unit, time, converge, false, 100.0F);
		renderBook(context, time, t, open, enter);
		glyphs.render(context, bookX, bookY, unit, time, converge, true, 800.0F);

		if (pages > 0.0F) {
			float lightX = bookX - unit * 0.6F * open;
			context.getMatrices().push();
			context.getMatrices().translate(0.0F, 0.0F, 850.0F);
			Draw.glow(context, lightX, bookY - unit * 1.5F, unit * 8.0F * pages, unit * 6.0F * pages, Draw.fade(0xFFFFE6F0, 0.4F * pages * (1.0F - flash)));
			Draw.glow(context, lightX, bookY - unit * 1.5F, unit * 3.0F * pages, unit * 2.4F * pages, Draw.fade(0xFFFFFFFF, 0.45F * pages));
			context.getMatrices().pop();
		}

		if (flash > 0.0F) {
			context.getMatrices().push();
			context.getMatrices().translate(0.0F, 0.0F, 900.0F);
			Draw.rect(context, 0, 0, width, height, Draw.fade(0xFFF7E3EC, flash));
			context.getMatrices().pop();
		}

		if (!shown) {
			shown = true;
			GrimoireHook.current().onBookShown(this);
		}
		playAmbient(time, t);
		playCues(t);
		if (t >= SWITCH_AT && !finished && client != null) {
			finished = true;
			GrimoireHook.current().onOpened(this);
			client.setScreen(new OriginSelectionScreen(true));
		}
	}

	private void renderBook(DrawContext context, float time, float t, float open, float enter) {
		float strapAlpha = t < 0 ? 1.0F : 1.0F - Draw.easeInOut(t / STRAP_END);
		float strapLift = t < 0 ? 0.0F : Draw.easeOut(t / STRAP_END) * 1.6F;
		float cover = t < 0 ? 0.0F : Draw.easeOutBack((t - COVER_START) / COVER_TIME) * 150.0F;
		float idleYaw = -6.0F + MathHelper.sin(time * 0.45F) * 7.0F;
		float idleTilt = 50.0F + MathHelper.sin(time * 0.7F + 1.3F) * 2.0F;
		float yaw = MathHelper.lerp(open, idleYaw, 0.0F);
		float tilt = MathHelper.lerp(open, idleTilt, 62.0F);
		float bob = MathHelper.sin(time * 1.2F) * 0.22F * unit * (1.0F - open);
		float scale = unit * (1.0F + 0.14F * open) * (0.94F + 0.06F * enter);
		float glow = 0.75F + 0.25F * MathHelper.sin(time * 1.6F) + 0.4F * open;

		MatrixStack matrices = context.getMatrices();
		matrices.push();
		matrices.translate(bookX, bookY + bob, 400.0F);
		matrices.scale(scale, -scale, scale);
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(tilt));
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
		matrices.translate(-MathHelper.lerp(open, 8.25F, 4.6F), -7.0F, -7.55F);
		GrimoireModel.get().render(matrices, context.getVertexConsumers(), time * 20.0F, cover, strapAlpha, strapLift, glow);
		context.draw();
		matrices.pop();
	}

	private void playAmbient(float time, float t) {
		if (t >= 0 || client == null || time < nextAmbient) {
			return;
		}
		nextAmbient = time + 2.6F + random.nextFloat() * 2.2F;
		play(AMBIENT, 0.8F + random.nextFloat() * 0.5F, 0.35F);
	}

	private void playCues(float t) {
		if (t < 0 || client == null) {
			return;
		}
		if (cues == 0) {
			play(TOUCH, 1.15F, 0.7F);
			play(PAGE, 0.9F, 0.8F);
			cues++;
		}
		if (cues == 1 && t >= FLASH_START) {
			play(REVEAL, 0.9F, 0.6F);
			cues++;
		}
	}

	private void play(SoundEvent sound, float pitch, float volume) {
		client.getSoundManager().play(PositionedSoundInstance.master(sound, pitch, volume));
	}

	private boolean isOverBook(double mouseX, double mouseY) {
		double dx = (mouseX - bookX) / (unit * 9.0F);
		double dy = (mouseY - bookY) / (unit * 7.5F);
		return dx * dx + dy * dy <= 1.0;
	}

	private void begin() {
		if (clickedAt < 0L && GrimoireHook.current().beforeOpen(this)) {
			clickedAt = Util.getMeasuringTimeMs();
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && clickedAt < 0L && isOverBook(mouseX, mouseY)) {
			begin();
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private float seconds() {
		return (Util.getMeasuringTimeMs() - openedAt) / 1000.0F;
	}

	private float opening() {
		return clickedAt < 0L ? -1.0F : (Util.getMeasuringTimeMs() - clickedAt) / 1000.0F;
	}
}
