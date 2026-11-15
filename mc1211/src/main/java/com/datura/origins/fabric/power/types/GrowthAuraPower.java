package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.JsonParams;
import com.datura.origins.fabric.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CocoaBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.StemBlock;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GrowthAuraPower implements RacePower {
	private static final Map<UUID, Vec3d> STILL = new ConcurrentHashMap<>();

	private final int radius;
	private final int interval;
	private final int attempts;
	private final float chance;

	public GrowthAuraPower(JsonObject json) {
		this.radius = JsonParams.integer(json, "radius", 6);
		this.interval = Math.max(1, JsonParams.integer(json, "interval", 10));
		this.attempts = JsonParams.integer(json, "attempts", 8);
		this.chance = JsonParams.num(json, "chance", 0.3F);
	}

	@Override
	public void tick(ServerPlayerEntity player) {
		if (player.isSpectator()) return;
		ServerWorld world = player.getServerWorld();
		Random random = world.random;
		BlockPos center = player.getBlockPos();
		if (player.age % interval == 0) {
			for (int i = 0; i < attempts; i++) {
				BlockPos pos = center.add(random.nextInt(radius * 2 + 1) - radius, random.nextInt(5) - 2, random.nextInt(radius * 2 + 1) - radius);
				BlockState state = world.getBlockState(pos);
				if (!(state.getBlock() instanceof PlantBlock) || !(state.getBlock() instanceof Fertilizable plant)) continue;
				if (!plant.isFertilizable(world, pos, state) || random.nextFloat() > chance) continue;
				plant.grow(world, random, pos, state);
				world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 3, 0.25, 0.2, 0.25, 0);
			}
		}
		if (player.age % 20 == 0) standingStill(player, world, center);
	}

	private static void standingStill(ServerPlayerEntity player, ServerWorld world, BlockPos center) {
		Vec3d last = STILL.put(player.getUuid(), player.getPos());
		if (last == null || last.squaredDistanceTo(player.getPos()) > 0.01) return;
		for (int i = 0; i < 24; i++) {
			BlockPos pos = center.add(world.random.nextInt(11) - 5, world.random.nextInt(5) - 2, world.random.nextInt(11) - 5);
			BlockState state = world.getBlockState(pos);
			Block block = state.getBlock();
			if (block instanceof CropBlock || block instanceof StemBlock || block instanceof CocoaBlock || block instanceof SweetBerryBushBlock) {
				state.randomTick(world, pos, world.random);
				if (world.random.nextInt(3) == 0) {
					world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 2, 0.25, 0.2, 0.25, 0);
				}
			}
		}
	}

	@Override
	public void onRemove(ServerPlayerEntity player) {
		STILL.remove(player.getUuid());
	}
}
