package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.mixin.FoxEntityAccessor;
import com.datura.origins.fabric.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FoxFriendPower implements RacePower {
	private static final Map<UUID, Long> MET = new ConcurrentHashMap<>();

	private final boolean follow;

	public FoxFriendPower(JsonObject json) {
		this.follow = com.datura.origins.fabric.power.JsonParams.bool(json, "follow", true);
	}

	@Override
	public void tick(ServerPlayerEntity p) {
		if (p.age % 20 != 0 || p.isSpectator()) return;
		long now = p.getServerWorld().getTime();
		for (FoxEntity fox : p.getServerWorld().getEntitiesByClass(FoxEntity.class, p.getBoundingBox().expand(10), f -> f.isAlive())) {
			((FoxEntityAccessor) fox).datura$trust(p.getUuid());
			long met = MET.computeIfAbsent(fox.getUuid(), u -> now);
			long age = now - met;
			if (age > 900) MET.put(fox.getUuid(), now);
			if (follow && age < 200 && !fox.isSleeping() && fox.squaredDistanceTo(p) > 9) {
				fox.getNavigation().startMovingTo(p, 1.15);
			}
		}
	}
}
