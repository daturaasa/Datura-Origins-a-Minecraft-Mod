package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.RacePower;
import com.datura.origins.fabric.race.RaceAccess;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public final class ButcherPower implements RacePower {
	public ButcherPower(JsonObject json) {
	}

	public static void bootstrap() {
		ServerLivingEntityEvents.AFTER_DEATH.register((victim, source) -> {
			if (!(victim instanceof AnimalEntity) || !(source.getAttacker() instanceof ServerPlayerEntity player)) return;
			if (!RaceAccess.getRace(player).map(r -> r.powers().stream().anyMatch(p -> p instanceof ButcherPower)).orElse(false)) return;
			ServerWorld world = player.getServerWorld();
			LootContextParameterSet params = new LootContextParameterSet.Builder(world)
					.add(LootContextParameters.THIS_ENTITY, victim)
					.add(LootContextParameters.ORIGIN, victim.getPos())
					.add(LootContextParameters.DAMAGE_SOURCE, source)
					.addOptional(LootContextParameters.ATTACKING_ENTITY, player)
					.add(LootContextParameters.LAST_DAMAGE_PLAYER, player)
					.build(LootContextTypes.ENTITY);
			world.getServer().getReloadableRegistries().getLootTable(victim.getLootTable()).generateLoot(params, victim::dropStack);
		});
	}
}
