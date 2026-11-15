package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.EntityFlags;
import com.datura.origins.fabric.power.RacePower;
import com.datura.origins.fabric.race.RaceAccess;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class LilyPadPower implements RacePower {
	public static final String FLAG = "lily_pad";

	public LilyPadPower(JsonObject json) {
	}

	public static boolean has(PlayerEntity player) {
		return RaceAccess.getRace(player).map(r -> r.powers().stream().anyMatch(p -> p instanceof LilyPadPower)).orElse(false);
	}

	public static double surface(PlayerEntity player) {
		if (player.getAbilities().flying || player.isSneaking() || player.hasVehicle() || player.isSpectator() || player.isFallFlying()) {
			return Double.NaN;
		}
		World world = player.getWorld();
		BlockPos pos = BlockPos.ofFloored(player.getX(), player.getY() - 0.05, player.getZ());
		for (int i = 0; i < 2; i++, pos = pos.up()) {
			FluidState fluid = world.getFluidState(pos);
			if (!fluid.isIn(FluidTags.WATER) || world.getFluidState(pos.up()).isIn(FluidTags.WATER)) continue;
			double top = pos.getY() + fluid.getHeight(world, pos);
			if (player.getY() <= top + 0.05 && player.getY() > top - 1.4) return top;
		}
		return Double.NaN;
	}

	public static void support(PlayerEntity player) {
		double top = surface(player);
		if (Double.isNaN(top)) return;
		if (player.getY() < top) player.setPosition(player.getX(), top, player.getZ());
		Vec3d v = player.getVelocity();
		if (v.y < 0) player.setVelocity(v.x, 0, v.z);
		player.setOnGround(true);
		player.fallDistance = 0;
	}

	@Override
	public void tick(ServerPlayerEntity player) {
		boolean on = !Double.isNaN(surface(player));
		if (on != EntityFlags.has(player, FLAG)) EntityFlags.set(player, FLAG, on);
	}

	@Override
	public void onRemove(ServerPlayerEntity player) {
		EntityFlags.set(player, FLAG, false);
	}
}
