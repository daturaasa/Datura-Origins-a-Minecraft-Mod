package com.datura.origins.fabric.power.ability;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public final class TempBlocks {
	private static final Map<RegistryKey<World>, Map<BlockPos, Block>> PLACED = new HashMap<>();

	private TempBlocks() {
	}

	public static void bootstrap() {
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
			Map<BlockPos, Block> placed = PLACED.get(world.getRegistryKey());
			if (placed == null || placed.get(pos) != state.getBlock()) return true;
			placed.remove(pos);
			world.removeBlock(pos, false);
			return false;
		});
	}

	public static boolean place(ServerWorld world, BlockPos pos, BlockState state) {
		BlockState old = world.getBlockState(pos);
		if (!old.isAir() && !old.isReplaceable()) return false;
		world.setBlockState(pos, state);
		PLACED.computeIfAbsent(world.getRegistryKey(), k -> new HashMap<>()).put(pos.toImmutable(), state.getBlock());
		return true;
	}

	public static void remove(ServerWorld world, BlockPos pos) {
		Map<BlockPos, Block> placed = PLACED.get(world.getRegistryKey());
		if (placed == null) return;
		Block block = placed.remove(pos);
		if (block != null && world.getBlockState(pos).isOf(block)) world.removeBlock(pos, false);
	}
}
