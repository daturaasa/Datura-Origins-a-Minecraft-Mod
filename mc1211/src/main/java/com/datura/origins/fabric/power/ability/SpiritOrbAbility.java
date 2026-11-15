package com.datura.origins.fabric.power.ability;

import com.datura.origins.fabric.DaturaOriginsMod;
import com.datura.origins.fabric.entity.SpellProjectileEntity;
import com.datura.origins.fabric.power.Ability;
import com.datura.origins.fabric.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;

public final class SpiritOrbAbility implements Ability {
	private static final ExplosionBehavior HARMLESS = new ExplosionBehavior() {
		@Override
		public boolean shouldDamage(Explosion explosion, Entity entity) {
			return false;
		}

		@Override
		public float getKnockbackModifier(Entity entity) {
			return 0.0F;
		}
	};

	private static final SoundEvent CAST = SoundEvent.of(DaturaOriginsMod.id("mana_cast"));
	private static final SoundEvent BURST = SoundEvent.of(DaturaOriginsMod.id("mana_burst"));

	private final double range;
	private final double speed;
	private final float damage;
	private final float radius;
	private final float returnDamage;
	private final float returnRadius;

	public SpiritOrbAbility(JsonObject json) {
		this.range = JsonParams.num(json, "range", 14.0F);
		this.speed = JsonParams.num(json, "speed", 1.1F);
		this.damage = JsonParams.num(json, "damage", 6.0F);
		this.radius = JsonParams.num(json, "radius", 2.5F);
		this.returnDamage = JsonParams.num(json, "return_damage", 5.0F);
		this.returnRadius = JsonParams.num(json, "return_radius", 3.5F);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		ServerWorld w = p.getServerWorld();
		SpellProjectileEntity orb = new SpellProjectileEntity(w, p, SpellProjectileEntity.MANA, 0, 1, 600);
		orb.scripted = true;
		orb.setSize(1.3F);
		orb.setVelocity(p.getRotationVec(1.0F).multiply(speed));
		w.spawnEntity(orb);
		LegendKit.anim(p, "cast", 10);
		LegendKit.snd(p, CAST, 1.0F, 1.0F);
		Vec3d start = orb.getPos();
		boolean[] back = {false};
		boolean[] hit = {false};
		LegendKit.run(p, "spirit_orb", 240, (pl, age) -> {
			if (orb.isRemoved()) return false;
			Vec3d pos = orb.getPos();
			if (!back[0]) {
				Vec3d next = pos.add(orb.getVelocity());
				LivingEntity victim = w.getEntitiesByClass(LivingEntity.class, orb.getBoundingBox().stretch(orb.getVelocity()).expand(0.4),
						e -> e.isAlive() && e != pl && !e.isSpectator()).stream().findFirst().orElse(null);
				if (victim != null) {
					hit[0] = true;
					back[0] = true;
					blast(pl, w, victim.getPos().add(0, victim.getHeight() * 0.5, 0), radius, damage, victim, 2.2F);
				} else if (w.raycast(new RaycastContext(pos, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, orb))
						.getType() != HitResult.Type.MISS || pos.distanceTo(start) >= range) {
					back[0] = true;
				}
				if (back[0]) orb.setVelocity(orb.getVelocity().multiply(0.2));
				return true;
			}
			Vec3d home = pl.getPos().add(0, pl.getHeight() * 0.6, 0);
			Vec3d to = home.subtract(pos);
			double d = to.length();
			if (d < 1.1) {
				if (hit[0]) blast(pl, w, home, returnRadius, returnDamage, null, 3.2F);
				orb.discard();
				return false;
			}
			double sp = Math.min(d, speed * 1.25 + age * 0.01);
			orb.setVelocity(orb.getVelocity().multiply(0.35).add(to.normalize().multiply(sp * 0.65)));
			orb.velocityModified = true;
			return true;
		}, pl -> {
			if (!orb.isRemoved()) orb.discard();
		});
		return true;
	}

	private static void freeze(LivingEntity target, int ticks) {
		if (!target.canFreeze()) return;
		target.setFrozenTicks(Math.max(target.getFrozenTicks(), target.getMinFreezeDamageTicks() + ticks));
		target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, ticks, 3, false, true));
	}

	private static void blast(ServerPlayerEntity p, ServerWorld w, Vec3d c, float r, float dmg, LivingEntity primary, float fx) {
		if (primary != null) {
			LegendKit.hurt(p, primary, dmg, false);
			freeze(primary, 60);
			w.createExplosion(p, null, HARMLESS, c.x, c.y, c.z, 1.6F, false, World.ExplosionSourceType.MOB);
		}
		for (LivingEntity e : LegendKit.enemies(p, c, r)) {
			if (e == primary) continue;
			LegendKit.hurt(p, e, dmg * 0.75F, false);
			freeze(e, 40);
			Vec3d push = LegendKit.flat(e.getPos().subtract(c)).multiply(0.4);
			e.addVelocity(push.x, 0.2, push.z);
			e.velocityModified = true;
		}
		LegendKit.fxAt(w, "boom_mana", c, fx, 0, 0);
		LegendKit.sndAt(w, c, BURST, 1.1F, 0.9F + w.random.nextFloat() * 0.2F);
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		LegendKit.stop(p, "spirit_orb");
	}
}
