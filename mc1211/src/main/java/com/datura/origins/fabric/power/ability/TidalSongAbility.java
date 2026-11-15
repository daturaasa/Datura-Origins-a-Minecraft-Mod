package com.datura.origins.fabric.power.ability;

import com.datura.origins.fabric.power.Ability;
import com.datura.origins.fabric.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

public final class TidalSongAbility implements Ability {
	private final double radius;
	private final int duration;
	private final float heal;

	public TidalSongAbility(JsonObject json) {
		this.radius = JsonParams.num(json, "radius", 10.0F);
		this.duration = JsonParams.integer(json, "duration", 160);
		this.heal = JsonParams.num(json, "heal", 1.0F);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		LegendKit.anim(p, "cast", 20);
		LegendKit.snd(p, SoundEvents.BLOCK_CONDUIT_ACTIVATE, 1.0F, 1.4F);
		LegendKit.snd(p, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F, 0.9F);
		LegendKit.snd(p, SoundEvents.ENTITY_ALLAY_AMBIENT_WITH_ITEM, 0.9F, 0.7F);
		LegendKit.run(p, "tidal_song", duration, (pl, age) -> {
			Vec3d c = pl.getPos();
			if (age % 4 == 0) {
				pl.getServerWorld().spawnParticles(ParticleTypes.NOTE, pl.getX(), pl.getEyeY() + 0.4, pl.getZ(), 1, 0.4, 0.2, 0.4, 1.0);
				double a = age * 0.35;
				pl.getServerWorld().spawnParticles(ParticleTypes.BUBBLE_POP, c.x + Math.cos(a) * 2, c.y + 1, c.z + Math.sin(a) * 2, 3, 0.2, 0.3, 0.2, 0.02);
			}
			for (LivingEntity e : LegendKit.enemies(pl, c, radius)) {
				if (e instanceof MobEntity m) {
					m.setTarget(null);
					m.getNavigation().stop();
				}
				Vec3d pull = c.subtract(e.getPos());
				if (pull.lengthSquared() > 4) {
					pull = pull.normalize().multiply(0.06);
					e.addVelocity(pull.x, 0, pull.z);
					e.velocityModified = true;
				}
				if (age % 20 == 0) {
					e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 2, true, false, true));
					e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 40, 1, true, false, true));
					pl.getServerWorld().spawnParticles(ParticleTypes.NOTE, e.getX(), e.getEyeY() + 0.5, e.getZ(), 1, 0.2, 0.1, 0.2, 0.5);
				}
			}
			if (age % 20 == 0) {
				for (PlayerEntity ally : LegendKit.allies(pl, c, radius)) {
					ally.heal(heal);
					ally.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 400, 0, true, false, true));
					ally.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 100, 0, true, false, true));
				}
				LegendKit.snd(pl, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8F, 0.8F + (age / 20 % 4) * 0.15F);
			}
			if (age % 40 == 10) LegendKit.snd(pl, SoundEvents.ENTITY_DOLPHIN_PLAY, 0.6F, 1.2F);
			return true;
		});
		return true;
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		LegendKit.stop(p, "tidal_song");
	}
}
