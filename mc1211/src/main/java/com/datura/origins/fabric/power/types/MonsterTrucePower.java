package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.JsonParams;
import com.datura.origins.fabric.power.RacePower;
import com.datura.origins.fabric.race.RaceAccess;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.EntityTypeTags;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MonsterTrucePower implements RacePower {
	private static final Map<UUID, Long> BROKEN = new ConcurrentHashMap<>();

	private final boolean undeadOnly;
	private final int breakTicks;

	public MonsterTrucePower(JsonObject json) {
		this.undeadOnly = JsonParams.bool(json, "undead_only", false);
		this.breakTicks = JsonParams.integer(json, "break_on_attack", 0);
	}

	public static void bootstrap() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim, source, amount) -> {
			if (victim instanceof Monster && source.getAttacker() instanceof PlayerEntity player) {
				BROKEN.put(player.getUuid(), player.getWorld().getTime());
			}
			return true;
		});
	}

	public static boolean blocks(MobEntity mob, LivingEntity target) {
		if (!(mob instanceof Monster) || !(target instanceof PlayerEntity player)) return false;
		if (mob.getAttacker() == player) return false;
		Long broke = BROKEN.get(player.getUuid());
		long now = player.getWorld().getTime();
		return RaceAccess.getRace(player).map(r -> r.powers().stream().anyMatch(p -> p instanceof MonsterTrucePower t
				&& !(t.breakTicks > 0 && broke != null && now - broke < t.breakTicks)
				&& (!t.undeadOnly || mob.getType().isIn(EntityTypeTags.UNDEAD)))).orElse(false);
	}
}
