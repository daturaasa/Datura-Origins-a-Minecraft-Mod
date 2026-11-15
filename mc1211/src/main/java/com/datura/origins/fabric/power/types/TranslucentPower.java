package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.EntityFlags;
import com.datura.origins.fabric.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;

public final class TranslucentPower implements RacePower {
	public static final String FLAG = "translucent";

	public TranslucentPower(JsonObject json) {
	}

	@Override
	public void onApply(ServerPlayerEntity player) {
		EntityFlags.set(player, FLAG, true);
	}

	@Override
	public void onRemove(ServerPlayerEntity player) {
		EntityFlags.set(player, FLAG, false);
	}
}
