package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.RacePower;
import com.datura.origins.fabric.race.RaceAccess;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowerBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public final class NatureOathPower implements RacePower {
	public NatureOathPower(JsonObject json) {
	}

	private static boolean has(PlayerEntity player) {
		return RaceAccess.getRace(player).map(r -> r.powers().stream().anyMatch(p -> p instanceof NatureOathPower)).orElse(false);
	}

	public static void bootstrap() {
		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (!(player instanceof ServerPlayerEntity sp) || sp.isCreative() || !state.isIn(BlockTags.LOGS) || !has(sp)) return;
			sp.sendMessage(Text.translatable("datura_origins.message.dryad_betrayal").formatted(Formatting.DARK_GREEN), false);
			world.playSound(null, pos, SoundEvents.BLOCK_AZALEA_LEAVES_BREAK, SoundCategory.PLAYERS, 1.0F, 0.5F);
			sp.damage(sp.getDamageSources().magic(), Float.MAX_VALUE);
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayerEntity sp && has(sp)) scatterFlowers(sp.getServerWorld(), sp.getBlockPos());
		});
	}

	private static void scatterFlowers(ServerWorld world, BlockPos center) {
		List<Block> flowers = new ArrayList<>();
		for (Block block : Registries.BLOCK) {
			if (block instanceof FlowerBlock && block != Blocks.WITHER_ROSE) flowers.add(block);
		}
		if (flowers.isEmpty()) return;
		for (int i = 0; i < 28; i++) {
			BlockPos pos = center.add(world.random.nextInt(9) - 4, world.random.nextInt(5) - 2, world.random.nextInt(9) - 4);
			BlockState flower = flowers.get(world.random.nextInt(flowers.size())).getDefaultState();
			if (world.isAir(pos) && flower.canPlaceAt(world, pos)) world.setBlockState(pos, flower);
		}
		world.playSound(null, center, SoundEvents.BLOCK_FLOWERING_AZALEA_PLACE, SoundCategory.PLAYERS, 1.0F, 0.8F);
	}
}
