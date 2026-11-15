package com.datura.origins.fabric.power.ability;

import com.datura.origins.fabric.network.AltarOffersPayload;
import com.datura.origins.fabric.power.Ability;
import com.datura.origins.fabric.power.JsonParams;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class BlessingAltarAbility implements Ability {
	private record Session(Item item, List<RegistryEntry<Enchantment>> enchantments, List<Integer> levels) {
	}

	private static final Map<UUID, Session> SESSIONS = new HashMap<>();
	private final int options;

	public BlessingAltarAbility(JsonObject json) {
		this.options = Math.max(1, Math.min(3, JsonParams.integer(json, "options", 3)));
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		ItemStack hand = p.getMainHandStack();
		if (hand.isEmpty()) {
			p.sendMessage(Text.translatable("datura_origins.message.altar_empty").formatted(Formatting.GRAY), true);
			return false;
		}
		Registry<Enchantment> registry = p.getWorld().getRegistryManager().get(RegistryKeys.ENCHANTMENT);
		List<RegistryEntry<Enchantment>> pool = new ArrayList<>();
		for (RegistryEntry<Enchantment> e : registry.streamEntries().toList()) {
			if (e.value().isAcceptableItem(hand) && EnchantmentHelper.getLevel(e, hand) < e.value().getMaxLevel()
					&& EnchantmentHelper.isCompatible(EnchantmentHelper.getEnchantments(hand).getEnchantments(), e)) {
				pool.add(e);
			}
		}
		if (pool.isEmpty()) {
			p.sendMessage(Text.translatable("datura_origins.message.altar_none").formatted(Formatting.GRAY), true);
			return false;
		}
		Collections.shuffle(pool, new Random(p.getRandom().nextLong()));
		List<RegistryEntry<Enchantment>> chosen = new ArrayList<>();
		List<String> ids = new ArrayList<>();
		List<Integer> levels = new ArrayList<>();
		for (int i = 0; i < Math.min(options, pool.size()); i++) {
			RegistryEntry<Enchantment> e = pool.get(i);
			int level = 1 + p.getRandom().nextInt(e.value().getMaxLevel());
			chosen.add(e);
			levels.add(level);
			ids.add(e.getKey().map(k -> k.getValue().toString()).orElse(""));
		}
		SESSIONS.put(p.getUuid(), new Session(hand.getItem(), chosen, levels));
		ServerPlayNetworking.send(p, new AltarOffersPayload(ids, levels, p.getRandom().nextInt()));
		LegendKit.anim(p, "cast", 30);
		p.getServerWorld().spawnParticles(ParticleTypes.ENCHANT, p.getX(), p.getBodyY(0.7), p.getZ(), 60, 0.6, 0.8, 0.6, 1.0);
		LegendKit.snd(p, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 1.0F, 1.1F);
		LegendKit.snd(p, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8F, 1.4F);
		return true;
	}

	@Override
	public void choose(ServerPlayerEntity p, int option) {
		Session session = SESSIONS.remove(p.getUuid());
		if (session == null || option < 0) return;
		ItemStack hand = p.getMainHandStack();
		if (option >= session.enchantments.size() || hand.isEmpty() || hand.getItem() != session.item) {
			p.sendMessage(Text.translatable("datura_origins.message.altar_lost").formatted(Formatting.GRAY), true);
			return;
		}
		RegistryEntry<Enchantment> e = session.enchantments.get(option);
		int level = Math.max(session.levels.get(option), EnchantmentHelper.getLevel(e, hand));
		hand.addEnchantment(e, level);
		p.setStackInHand(Hand.MAIN_HAND, hand);
		p.sendMessage(Text.translatable("datura_origins.message.altar_done", Enchantment.getName(e, level)).formatted(Formatting.AQUA), true);
		p.getServerWorld().spawnParticles(ParticleTypes.GLOW, p.getX(), p.getBodyY(0.7), p.getZ(), 30, 0.5, 0.6, 0.5, 0.05);
		LegendKit.snd(p, SoundEvents.ENTITY_PLAYER_LEVELUP, 0.8F, 1.6F);
		LegendKit.snd(p, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.2F, 1.9F);
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		SESSIONS.remove(p.getUuid());
	}
}
