package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.EntityFlags;
import com.datura.origins.fabric.power.JsonParams;
import com.datura.origins.fabric.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public final class ClaustrophobiaPower implements RacePower {
	public static final String FLAG = "cramped";
	private static final int INTERVAL = 20;

	private final int clearance;
	private final int slowness;
	private final float exhaustion;

	public ClaustrophobiaPower(JsonObject json) {
		this.clearance = Math.max(1, JsonParams.integer(json, "clearance", 6));
		this.slowness = JsonParams.integer(json, "slowness", 0);
		this.exhaustion = JsonParams.num(json, "exhaustion_per_second", 0.25f);
	}

	@Override
	public void tick(ServerPlayerEntity player) {
		if (player.age % INTERVAL != 0) return;
		boolean cramped = !player.isSpectator() && !player.getAbilities().flying && isCramped(player);
		EntityFlags.set(player, FLAG, cramped);
		if (!cramped) return;
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, INTERVAL + 10, slowness, true, false, true));
		if (exhaustion > 0) player.addExhaustion(exhaustion);
	}

	@Override
	public void onRemove(ServerPlayerEntity player) {
		EntityFlags.set(player, FLAG, false);
	}

		private boolean isCramped(ServerPlayerEntity player) {
		World world = player.getWorld();
		Box box = player.getBoundingBox();
		double inset = 0.05;
		double[][] columns = {
				{player.getX(), player.getZ()},
				{box.minX + inset, box.minZ + inset}, {box.maxX - inset, box.minZ + inset},
				{box.minX + inset, box.maxZ - inset}, {box.maxX - inset, box.maxZ - inset}};
		int baseY = (int) Math.floor(player.getY() + 0.5);
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (double[] col : columns) {
			int x = (int) Math.floor(col[0]), z = (int) Math.floor(col[1]);
			for (int dy = 0; dy < clearance; dy++) {
				pos.set(x, baseY + dy, z);
				if (!world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()) return true;
			}
		}
		return false;
	}
}
