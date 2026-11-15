package com.datura.origins.fabric.power.ability;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class DragonFire {
	private DragonFire() {}

	public static DamageSource fire(ServerWorld world, Entity attacker) {
		return new DamageSource(world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(DamageTypes.IN_FIRE), attacker);
	}

	public static void burn(ServerWorld w, LivingEntity t, Entity attacker, float fireDamage, int seconds) {
		if (t.isFireImmune() || !t.isAlive()) return;
		t.setOnFireFor(seconds);
		if (fireDamage > 0) {
			t.timeUntilRegen = 0;
			t.damage(fire(w, attacker), fireDamage);
		}
	}

	public static void burnArea(ServerWorld w, Vec3d c, double radius, Entity attacker, float fireDamage, int seconds) {
		for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(c, c).expand(radius), LivingEntity::isAlive)) {
			if (e == attacker || e.getPos().distanceTo(c) > radius + e.getWidth()) continue;
			burn(w, e, attacker, fireDamage, seconds);
		}
	}
}
