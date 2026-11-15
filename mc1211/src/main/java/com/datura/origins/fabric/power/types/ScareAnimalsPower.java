package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class ScareAnimalsPower implements RacePower {
	public ScareAnimalsPower(JsonObject json) {
	}

	@Override
	public void tick(ServerPlayerEntity p) {
		if (p.age % 10 != 0 || p.isSpectator()) return;
		for (AnimalEntity a : p.getServerWorld().getEntitiesByClass(AnimalEntity.class, p.getBoundingBox().expand(9),
				x -> !(x instanceof TameableEntity t && t.isTamed()) && !x.hasPassengers())) {
			Vec3d away = a.getPos().subtract(p.getPos()).multiply(1, 0, 1);
			if (away.lengthSquared() < 1e-3) away = new Vec3d(1, 0, 0);
			Vec3d to = a.getPos().add(away.normalize().multiply(8));
			a.getNavigation().startMovingTo(to.x, to.y, to.z, 1.6);
		}
	}
}
