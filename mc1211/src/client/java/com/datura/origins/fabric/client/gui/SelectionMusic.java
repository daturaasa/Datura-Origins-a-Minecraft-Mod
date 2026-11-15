package com.datura.origins.fabric.client.gui;

import com.datura.origins.DaturaOrigins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.MusicSound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class SelectionMusic extends MovingSoundInstance {
	private static final SoundEvent THEME = SoundEvent.of(Identifier.of(DaturaOrigins.MOD_ID, "music.selection"));
	private static final float MAX_VOLUME = 0.6F;
	private static SelectionMusic current;

	private boolean leaving;

	private SelectionMusic() {
		super(THEME, SoundCategory.MUSIC, SoundInstance.createRandom());
		this.repeat = true;
		this.repeatDelay = 0;
		this.relative = true;
		this.attenuationType = SoundInstance.AttenuationType.NONE;
		this.volume = 0.01F;
	}

	public static void tick(MinecraftClient client) {
		boolean wanted = client.currentScreen instanceof IntroScreen || client.currentScreen instanceof OriginSelectionScreen;
		if (wanted) {
			MusicSound vanilla = client.getMusicType();
			if (vanilla != null && client.getMusicTracker().isPlayingType(vanilla)) client.getMusicTracker().stop();
			if (current == null || current.isDone() || !client.getSoundManager().isPlaying(current)) {
				if (current != null) client.getSoundManager().stop(current);
				current = new SelectionMusic();
				client.getSoundManager().play(current);
			}
			current.leaving = false;
		} else if (current != null) {
			current.leaving = true;
			if (current.isDone()) current = null;
		}
	}

	@Override
	public void tick() {
		if (leaving) {
			volume -= 0.02F;
			if (volume <= 0.0F) {
				volume = 0.0F;
				setDone();
			}
		} else if (volume < MAX_VOLUME) {
			volume = Math.min(MAX_VOLUME, volume + 0.01F);
		}
	}

	@Override
	public boolean shouldAlwaysPlay() {
		return true;
	}
}
