package com.datura.origins.fabric.power.ability;

import com.datura.origins.fabric.DaturaOriginsMod;
import com.datura.origins.fabric.power.Ability;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

public final class PhaseAbility implements Ability {
	public static final AttachmentType<String> PREVIOUS_MODE = AttachmentRegistry.<String>builder()
			.persistent(Codec.STRING)
			.buildAndRegister(DaturaOriginsMod.id("phase_previous_mode"));

	public PhaseAbility(JsonObject json) {
	}

	public static boolean isPhased(PlayerEntity player) {
		return player.getAttached(PREVIOUS_MODE) != null;
	}

	@Override
	public boolean activate(ServerPlayerEntity player) {
		if (!isPhased(player)) {
			if (player.isSpectator()) return false;
			player.setAttached(PREVIOUS_MODE, player.interactionManager.getGameMode().getName());
			effects(player);
			player.changeGameMode(GameMode.SPECTATOR);
			return true;
		}
		if (!player.getWorld().isSpaceEmpty(player, player.getDimensions(player.getPose()).getBoxAt(player.getPos()))) {
			player.sendMessage(Text.translatable("datura_origins.message.phase_no_room").formatted(Formatting.GRAY), true);
			return false;
		}
		restore(player);
		effects(player);
		return true;
	}

	@Override
	public void onRemove(ServerPlayerEntity player) {
		if (isPhased(player)) restore(player);
	}

	private static void restore(ServerPlayerEntity player) {
		GameMode mode = GameMode.byName(player.getAttached(PREVIOUS_MODE), GameMode.SURVIVAL);
		player.removeAttached(PREVIOUS_MODE);
		player.changeGameMode(mode);
	}

	private static void effects(ServerPlayerEntity player) {
		player.getServerWorld().spawnParticles(ParticleTypes.SOUL, player.getX(), player.getBodyY(0.5), player.getZ(), 24, 0.35, 0.7, 0.35, 0.03);
		player.getServerWorld().spawnParticles(ParticleTypes.SCULK_SOUL, player.getX(), player.getBodyY(0.5), player.getZ(), 10, 0.3, 0.6, 0.3, 0.02);
		LegendKit.snd(player, SoundEvents.PARTICLE_SOUL_ESCAPE, 1.0F, 0.7F);
		LegendKit.snd(player, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 0.5F, 0.5F);
	}
}
