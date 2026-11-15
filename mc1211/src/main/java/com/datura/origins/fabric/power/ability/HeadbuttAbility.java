package com.datura.origins.fabric.power.ability;

import com.datura.origins.fabric.power.Ability;
import com.datura.origins.fabric.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class HeadbuttAbility implements Ability {
	private final double range;
	private final double knockback;
	private final double lunge;
	private final float damage;

	public HeadbuttAbility(JsonObject json) {
		this.range = JsonParams.num(json, "range", 3.5F);
		this.damage = JsonParams.num(json, "damage", 4.0F);
		this.knockback = JsonParams.num(json, "knockback", 2.6F);
		this.lunge = JsonParams.num(json, "lunge", 0.7F);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		Vec3d look = p.getRotationVec(1.0F);
		Vec3d flat = new Vec3d(look.x, 0, look.z);
		if (flat.lengthSquared() < 1.0E-4) {
			flat = new Vec3d(-Math.sin(Math.toRadians(p.getYaw())), 0, Math.cos(Math.toRadians(p.getYaw())));
		}
		flat = flat.normalize();
		p.setVelocity(flat.x * lunge, Math.max(p.getVelocity().y, 0.1), flat.z * lunge);
		p.velocityModified = true;
		LegendKit.anim(p, "ram", 12);
		Vec3d eye = p.getPos().add(0, p.getHeight() * 0.6, 0);
		LivingEntity best = null;
		double bestDistance = Double.MAX_VALUE;
		Box area = p.getBoundingBox().stretch(flat.multiply(range)).expand(0.8, 0.5, 0.8);
		for (Entity e : p.getWorld().getOtherEntities(p, area, x -> x instanceof LivingEntity l && l.isAlive() && !x.isSpectator())) {
			Vec3d to = e.getPos().add(0, e.getHeight() * 0.5, 0).subtract(eye);
			double d = to.length();
			if (d > range + e.getWidth() || to.normalize().dotProduct(flat) < 0.45) continue;
			if (d < bestDistance) {
				bestDistance = d;
				best = (LivingEntity) e;
			}
		}
		if (best != null) {
			best.damage(p.getDamageSources().playerAttack(p), damage);
			double resist = 1.0 - best.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE) * 0.6;
			best.setVelocity(flat.x * knockback * resist, 0.55 * resist, flat.z * knockback * resist);
			best.velocityModified = true;
			Vec3d at = best.getPos().add(0, best.getHeight() * 0.5, 0);
			p.getServerWorld().spawnParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 10, 0.25, 0.25, 0.25, 0.25);
			p.getServerWorld().spawnParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 5, 0.2, 0.2, 0.2, 0.05);
			LegendKit.sndAt(p.getServerWorld(), at, SoundEvents.ENTITY_GOAT_RAM_IMPACT, 1.0F, 0.9F);
		} else {
			LegendKit.snd(p, SoundEvents.ENTITY_GOAT_PREPARE_RAM, 0.8F, 1.0F);
		}
		return true;
	}
}
