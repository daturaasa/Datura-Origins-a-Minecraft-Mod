package com.datura.origins.fabric.race;

import com.datura.origins.fabric.power.RacePower;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.CakeBlock;
import net.minecraft.block.CandleCakeBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.Optional;

public final class RaceEvents {
	private RaceEvents() {
	}

	public static void register() {
		UseItemCallback.EVENT.register((player, world, hand) -> {
			ItemStack stack = player.getStackInHand(hand);
			Optional<RaceProfile> race = raceOf(player);
			if (race.isEmpty()) return TypedActionResult.pass(stack);
			boolean food = stack.get(DataComponentTypes.FOOD) != null;
			for (RacePower power : race.get().powers()) {
				Text t = power.useRejection(player, stack);
				if (t == null && food) t = power.consumeRejection(player, stack);
				if (t != null) {
					notify(player, world, t);
					return TypedActionResult.fail(stack);
				}
			}
			return TypedActionResult.pass(stack);
		});

		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			BlockState state = world.getBlockState(hit.getBlockPos());
			if (!(state.getBlock() instanceof CakeBlock) && !(state.getBlock() instanceof CandleCakeBlock)) {
				return ActionResult.PASS;
			}
			if (player.isSneaking() && !player.getStackInHand(hand).isEmpty()) return ActionResult.PASS;
			Optional<RaceProfile> race = raceOf(player);
			if (race.isEmpty()) return ActionResult.PASS;
			ItemStack cake = new ItemStack(Items.CAKE);
			for (RacePower power : race.get().powers()) {
				Text t = power.consumeRejection(player, cake);
				if (t != null) {
					notify(player, world, t);
					return ActionResult.FAIL;
				}
			}
			return ActionResult.PASS;
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!(source.getAttacker() instanceof ServerPlayerEntity killer) || killer == entity) return;
			RaceManager.getRace(killer).ifPresent(r -> {
				for (RacePower power : r.powers()) power.onKill(killer, entity);
			});
		});

		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayerEntity player)) return true;
			Optional<RaceProfile> race = RaceManager.getRace(player);
			if (race.isEmpty()) return true;
			for (RacePower power : race.get().powers()) {
				if (!power.allowDamage(player, source, amount)) return false;
			}
			return true;
		});
	}

	private static Optional<RaceProfile> raceOf(PlayerEntity player) {
		if (player.isCreative()) return Optional.empty();
		return RaceAccess.getRace(player);
	}

	private static void notify(PlayerEntity player, World world, Text message) {
		if (!world.isClient) player.sendMessage(message, true);
	}
}
