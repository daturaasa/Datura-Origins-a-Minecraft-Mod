package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.JsonParams;
import com.datura.origins.fabric.power.RacePower;
import com.datura.origins.fabric.race.RaceAccess;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;

import java.util.List;

public final class FortunePower implements RacePower {
	private final int level;

	public FortunePower(JsonObject json) {
		this.level = Math.max(1, JsonParams.integer(json, "level", 2));
	}

	private static boolean isOre(BlockState state) {
		String path = Registries.BLOCK.getId(state.getBlock()).getPath();
		return path.endsWith("_ore") || path.equals("ancient_debris");
	}

	public static void bootstrap() {
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
			if (!(world instanceof ServerWorld sw) || !(player instanceof ServerPlayerEntity sp) || sp.isCreative() || !isOre(state)) return true;
			FortunePower power = RaceAccess.getRace(sp).flatMap(r -> r.powers().stream()
					.filter(p -> p instanceof FortunePower)
					.map(p -> (FortunePower) p)
					.findFirst()).orElse(null);
			if (power == null || !sp.canHarvest(state)) return true;
			ItemStack tool = sp.getMainHandStack();
			RegistryEntry<Enchantment> silk = sw.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(Enchantments.SILK_TOUCH).orElse(null);
			RegistryEntry<Enchantment> fortune = sw.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(Enchantments.FORTUNE).orElse(null);
			if (fortune == null || silk != null && EnchantmentHelper.getLevel(silk, tool) > 0) return true;
			if (EnchantmentHelper.getLevel(fortune, tool) >= power.level) return true;
			ItemStack used = tool.copy();
			used.addEnchantment(fortune, power.level);
			List<ItemStack> drops = Block.getDroppedStacks(state, sw, pos, blockEntity, sp, used);
			ItemStack before = tool.copy();
			sw.breakBlock(pos, false, sp);
			for (ItemStack drop : drops) Block.dropStack(sw, pos, drop);
			state.onStacksDropped(sw, pos, before, true);
			tool.postMine(sw, state, pos, sp);
			sp.incrementStat(Stats.MINED.getOrCreateStat(state.getBlock()));
			sp.addExhaustion(0.005F);
			return false;
		});
	}
}
