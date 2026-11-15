package com.datura.origins.fabric.power.ability;

import com.datura.origins.fabric.power.Ability;
import com.datura.origins.fabric.power.Control;
import com.datura.origins.fabric.power.EntityFlags;
import com.datura.origins.fabric.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class DarkCometAbility implements Ability {
	public static final String CHAIN_PREFIX = "chain#";
	private static final DustParticleEffect BLOOD = new DustParticleEffect(new Vector3f(0.55F, 0.03F, 0.08F), 1.4F);

	private final double radius;
	private final double leash;
	private final float damage;
	private final float tickDamage;
	private final int root;
	private final int chain;
	private final int maxTargets;

	public DarkCometAbility(JsonObject json) {
		radius = JsonParams.num(json, "radius", 10.0F);
		leash = JsonParams.num(json, "leash", 10.0F);
		damage = JsonParams.num(json, "damage", 6.0F);
		tickDamage = JsonParams.num(json, "tick_damage", 1.5F);
		root = JsonParams.integer(json, "root", 60);
		chain = JsonParams.integer(json, "chain", 160);
		maxTargets = Math.max(1, JsonParams.integer(json, "max_targets", 3));
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		List<LivingEntity> targets = new ArrayList<>(LegendKit.enemies(p, p.getPos(), radius));
		targets.removeIf(e -> e.getPos().squaredDistanceTo(p.getPos()) > radius * radius || !p.canSee(e));
		targets.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(p)));
		if (targets.isEmpty()) {
			p.sendMessage(Text.translatable("datura_origins.message.no_target").formatted(Formatting.GRAY), true);
			return false;
		}
		LegendKit.anim(p, "cast", 8);
		LegendKit.snd(p, SoundEvents.BLOCK_CHAIN_PLACE, 1.2F, 0.6F);
		LegendKit.snd(p, SoundEvents.ENTITY_WITHER_SHOOT, 0.4F, 1.6F);
		burst(p.getServerWorld(), p.getPos().add(0, 1, 0), 0.8F);
		for (int i = 0; i < Math.min(maxTargets, targets.size()); i++) bind(p, targets.get(i));
		return true;
	}

	private static void burst(ServerWorld w, Vec3d at, float size) {
		w.spawnParticles(BLOOD, at.x, at.y, at.z, Math.round(18 * size), 0.35 * size, 0.35 * size, 0.35 * size, 0);
	}

	private void bind(ServerPlayerEntity p, LivingEntity t) {
		ServerWorld w = p.getServerWorld();
		String key = "dark_chain#" + t.getId();
		LegendKit.stop(p, key);
		LegendKit.hurt(p, t, damage, false);
		Vec3d mid = t.getPos().add(0, t.getHeight() * 0.5, 0);
		burst(w, mid, 1.4F);
		LegendKit.sndAt(w, mid, SoundEvents.BLOCK_CHAIN_BREAK, 1.2F, 0.7F);
		String flag = CHAIN_PREFIX + t.getId();
		EntityFlags.set(p, flag, true);
		Vec3d anchor = t.getPos();
		LegendKit.run(p, key, chain, (pl, age) -> {
			if (!t.isAlive() || t.isRemoved() || t.getWorld() != pl.getWorld() || t.squaredDistanceTo(pl) > 40 * 40) return false;
			if (age < root) {
				pin(t, anchor);
				Control.lock(t, 2, anchor);
				if (t instanceof MobEntity mob) mob.setAiDisabled(true);
			} else {
				if (age == root && t instanceof MobEntity mob) mob.setAiDisabled(false);
				Vec3d to = pl.getPos().subtract(t.getPos());
				double d = to.length();
				if (d > leash) {
					Vec3d pull = to.normalize().multiply(Math.min(0.6, (d - leash) * 0.15));
					t.addVelocity(pull.x, Math.max(0, pull.y) * 0.3, pull.z);
					t.velocityModified = true;
				}
			}
			if (age % 20 == 10) {
				t.timeUntilRegen = 0;
				LegendKit.hurt(pl, t, tickDamage, false);
				burst(w, t.getPos().add(0, t.getHeight() * 0.5, 0), 0.4F);
			}
			return true;
		}, pl -> {
			EntityFlags.set(pl, flag, false);
			if (t instanceof MobEntity mob) mob.setAiDisabled(false);
			LegendKit.sndAt(pl.getServerWorld(), t.getPos(), SoundEvents.BLOCK_CHAIN_BREAK, 0.7F, 1.4F);
		});
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		LegendKit.stopPrefix(p, "dark_chain#");
	}

	private static void pin(LivingEntity target, Vec3d at) {
		Control.lock(target, 3);
		target.setVelocity(Vec3d.ZERO);
		target.velocityModified = true;
		if (target instanceof ServerPlayerEntity sp) {
			if (sp.getPos().squaredDistanceTo(at) > 0.01) sp.networkHandler.requestTeleport(at.x, at.y, at.z, sp.getYaw(), sp.getPitch());
		} else {
			target.setPosition(at.x, at.y, at.z);
		}
		target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 6, 254, true, false, false));
		target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 6, 254, true, false, false));
		target.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 6, 4, true, false, false));
		if (target instanceof MobEntity mob) {
			mob.setTarget(null);
			mob.getNavigation().stop();
		}
	}
}
