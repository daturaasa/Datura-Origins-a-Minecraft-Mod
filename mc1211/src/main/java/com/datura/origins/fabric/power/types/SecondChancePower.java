package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.JsonParams;
import com.datura.origins.fabric.power.RacePower;
import com.datura.origins.fabric.race.RaceAccess;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SecondChancePower implements RacePower {
	public static final String ANIM = "second_chance";
	private static final Map<UUID, Long> READY_AT = new HashMap<>();

	private final int cooldown;
	private final int regenTicks;
	private final float burstDamage, burstRadius, burstPush;

	public SecondChancePower(JsonObject json) {
		this.cooldown = JsonParams.integer(json, "cooldown", 6000);
		this.regenTicks = Math.round(JsonParams.num(json, "regen_seconds", 5f) * 20);
		this.burstDamage = JsonParams.num(json, "burst_damage", 0f);
		this.burstRadius = JsonParams.num(json, "burst_radius", 4f);
		this.burstPush = JsonParams.num(json, "burst_push", 1.6f);
	}

	public static void bootstrap() {
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayerEntity player) || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
			return RaceAccess.getRace(player).flatMap(r -> r.powers().stream()
					.filter(p -> p instanceof SecondChancePower).map(p -> (SecondChancePower) p).findFirst())
					.map(p -> !p.trySave(player)).orElse(true);
		});
	}

	private boolean trySave(ServerPlayerEntity player) {
		long now = player.getServerWorld().getTime();
		Long ready = READY_AT.get(player.getUuid());
		if (ready != null && now < ready) return false;
		READY_AT.put(player.getUuid(), now + cooldown);

		player.setHealth(2f);
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, regenTicks, 0, true, false, true));
		ServerWorld w = player.getServerWorld();
		if (burstDamage > 0) {
			GoldenWardPower.burst(player, burstRadius, burstPush, burstDamage);
			return true;
		}
		w.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.55f, 0.5f);
		w.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_WITHER_SKELETON_AMBIENT, SoundCategory.PLAYERS, 0.8f, 0.55f);
		w.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PARTICLE_SOUL_ESCAPE.value(), SoundCategory.PLAYERS, 1.2f, 0.6f);
		w.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getBodyY(0.5), player.getZ(), 40, 0.4, 0.6, 0.4, 0.4);
		w.spawnParticles(ParticleTypes.SOUL, player.getX(), player.getBodyY(0.5), player.getZ(), 16, 0.4, 0.6, 0.4, 0.04);
		return true;
	}

	@Override
	public void onRemove(ServerPlayerEntity player) {
		READY_AT.remove(player.getUuid());
	}
}
