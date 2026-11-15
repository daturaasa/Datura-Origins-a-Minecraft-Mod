package com.datura.origins.fabric.power.ability;

import com.datura.origins.fabric.power.Ability;
import com.datura.origins.fabric.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

public final class FlowerSanctuaryAbility implements Ability {
	private static final DustParticleEffect[] PETALS = {
			new DustParticleEffect(new Vector3f(0.96F, 0.65F, 0.78F), 1.2F),
			new DustParticleEffect(new Vector3f(1.0F, 0.89F, 0.48F), 1.2F),
			new DustParticleEffect(new Vector3f(0.79F, 0.65F, 0.96F), 1.2F),
			new DustParticleEffect(new Vector3f(0.95F, 0.95F, 0.95F), 1.2F)};

	private final float radius;
	private final float heal;
	private final int duration;
	private final int interval;

	public FlowerSanctuaryAbility(JsonObject json) {
		this.radius = JsonParams.num(json, "radius", 7.0F);
		this.duration = JsonParams.integer(json, "duration", 300);
		this.heal = JsonParams.num(json, "heal", 1.0F);
		this.interval = Math.max(1, JsonParams.integer(json, "interval", 20));
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		ServerWorld w = p.getServerWorld();
		Vec3d c = ground(w, p.getPos());
		LegendKit.anim(p, "cast", 12);
		LegendKit.sndAt(w, c, SoundEvents.BLOCK_AZALEA_LEAVES_PLACE, 1.2F, 1.0F);
		LegendKit.sndAt(w, c, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F, 1.4F);
		LegendKit.run(p, "sanctuary", duration, (pl, age) -> {
			if (age % 5 == 0) particles(w, c, age);
			double r2 = radius * radius;
			for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(c, c).expand(radius + 3, 4, radius + 3), LivingEntity::isAlive)) {
				double dx = e.getX() - c.x, dz = e.getZ() - c.z, d2 = dx * dx + dz * dz;
				if (e instanceof Monster) {
					if (d2 < (radius + 0.6) * (radius + 0.6)) {
						Vec3d out = LegendKit.flat(new Vec3d(dx, 0, dz));
						double push = d2 < r2 ? 0.55 : 0.3;
						e.setVelocity(out.x * push, Math.max(e.getVelocity().y, 0.08), out.z * push);
						e.velocityModified = true;
					}
					if (e instanceof MobEntity mob && mob.getTarget() != null) {
						LivingEntity t = mob.getTarget();
						double tx = t.getX() - c.x, tz = t.getZ() - c.z;
						if (tx * tx + tz * tz < r2) {
							mob.setTarget(null);
							mob.getNavigation().stop();
						}
					}
				} else if (d2 < r2 && age % interval == 0 && (e instanceof PlayerEntity || e instanceof TameableEntity pet && pet.isTamed())) {
					if (e.getHealth() < e.getMaxHealth()) {
						e.heal(heal);
						w.spawnParticles(ParticleTypes.HEART, e.getX(), e.getBodyY(1.0) + 0.3, e.getZ(), 1, 0.2, 0.1, 0.2, 0);
					}
				}
			}
			return true;
		});
		return true;
	}

	private void particles(ServerWorld w, Vec3d c, int age) {
		int ring = Math.max(24, Math.round(radius * 6));
		for (int i = 0; i < ring; i++) {
			double a = i * Math.PI * 2 / ring + age * 0.01;
			w.spawnParticles(PETALS[i % PETALS.length], c.x + Math.cos(a) * radius, c.y + 0.15, c.z + Math.sin(a) * radius, 1, 0, 0.05, 0, 0);
		}
		for (int i = 0; i < 10; i++) {
			double a = w.random.nextDouble() * Math.PI * 2, d = Math.sqrt(w.random.nextDouble()) * radius;
			w.spawnParticles(i % 2 == 0 ? ParticleTypes.CHERRY_LEAVES : PETALS[i % PETALS.length],
					c.x + Math.cos(a) * d, c.y + 0.2 + w.random.nextDouble(), c.z + Math.sin(a) * d, 1, 0, 0.02, 0, 0);
		}
		if (age % 20 == 0) w.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR, c.x, c.y + 1.5, c.z, 20, radius * 0.5, 0.6, radius * 0.5, 0);
	}

	private static Vec3d ground(ServerWorld w, Vec3d at) {
		BlockPos pos = BlockPos.ofFloored(at);
		for (int i = 0; i < 6 && w.getBlockState(pos.down()).getCollisionShape(w, pos.down()).isEmpty(); i++) pos = pos.down();
		return new Vec3d(at.x, pos.getY(), at.z);
	}
}
