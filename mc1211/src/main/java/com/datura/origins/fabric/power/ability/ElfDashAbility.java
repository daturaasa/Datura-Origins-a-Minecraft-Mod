package com.datura.origins.fabric.power.ability;

import com.datura.origins.fabric.network.AnimPayload;
import com.datura.origins.fabric.network.Networking;
import com.datura.origins.fabric.power.Ability;
import com.datura.origins.fabric.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

public final class ElfDashAbility implements Ability {
	private final double power, lift;
	private final boolean forward, orb;
	private static float orbDamage = 6f;
	private final double orbSpeed;

	public ElfDashAbility(JsonObject json) {
		this.power = JsonParams.num(json, "power", 1.3f);
		this.lift = JsonParams.num(json, "lift", 0.45f);
		this.forward = JsonParams.bool(json, "forward", false);
		this.orb = JsonParams.bool(json, "orb", false);
		orbDamage = JsonParams.num(json, "orb_damage", 6f);
		this.orbSpeed = JsonParams.num(json, "orb_speed", 1.6f);
	}

	public static void bootstrap() {
		com.datura.origins.fabric.entity.SpellProjectileEntity.onEntityHit(com.datura.origins.fabric.entity.SpellProjectileEntity.ARCANE, (proj, hit) -> {
			if (hit instanceof net.minecraft.entity.LivingEntity l && proj.getOwner() instanceof ServerPlayerEntity owner) {
				l.timeUntilRegen = 0;
				l.damage(owner.getDamageSources().indirectMagic(proj, owner), orbDamage);
				Vec3d push = proj.getVelocity().normalize();
				l.addVelocity(push.x * 0.8, 0.3, push.z * 0.8);
				l.velocityModified = true;
			}
		});
		com.datura.origins.fabric.entity.SpellProjectileEntity.onImpact(com.datura.origins.fabric.entity.SpellProjectileEntity.ARCANE, (proj, at) -> {
			var w = (net.minecraft.server.world.ServerWorld) proj.getWorld();
			LegendKit.fxAt(w, "boom_arcane", at, 1.6f, 0, 0);
			w.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.PLAYERS, 1.0f, 1.5f);
		});
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		Vec3d look = p.getRotationVec(1f);
		Vec3d flat = new Vec3d(look.x, 0, look.z);
		flat = flat.lengthSquared() < 1e-4 ? Vec3d.ZERO : flat.normalize().multiply(forward ? power : -power);
		p.setVelocity(flat.x, lift, flat.z);
		p.velocityModified = true;
		p.fallDistance = 0;
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, 1, true, false, true));
		Networking.sendAround(new AnimPayload(p.getId(), "fx:dash", 0), p);
		if (orb) {
			LegendKit.run(p, "elf_orb", 4, (pl, age) -> {
				if (age < 3) return true;
				com.datura.origins.fabric.entity.SpellProjectileEntity o = new com.datura.origins.fabric.entity.SpellProjectileEntity(pl.getWorld(), pl,
						com.datura.origins.fabric.entity.SpellProjectileEntity.ARCANE, 0.0, 1.0, 50);
				o.setVelocity(pl.getRotationVec(1f).multiply(orbSpeed));
				pl.getWorld().spawnEntity(o);
				LegendKit.anim(pl, "throw", 8);
				LegendKit.snd(pl, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 1.8f);
				LegendKit.snd(pl, SoundEvents.ENTITY_BREEZE_SHOOT, 0.8f, 1.4f);
				return false;
			});
		}
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_BREEZE_JUMP, SoundCategory.PLAYERS, 0.7f, 1.3f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_ARMOR_EQUIP_LEATHER.value(), SoundCategory.PLAYERS, 0.6f, 1.4f);
		return true;
	}
}
