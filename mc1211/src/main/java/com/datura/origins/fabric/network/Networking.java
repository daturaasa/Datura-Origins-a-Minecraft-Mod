package com.datura.origins.fabric.network;

import com.datura.origins.fabric.OriginData;
import com.datura.origins.fabric.power.types.BatFormPower;
import com.datura.origins.fabric.power.types.MountainLeapPower;
import com.datura.origins.fabric.race.RaceManager;
import com.datura.origins.race.RaceRegistry;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.LinkedHashSet;
import java.util.Set;

public final class Networking {
	private Networking() {
	}

	public static void init() {
		PayloadTypeRegistry.playS2C().register(OpenSelectionPayload.ID, OpenSelectionPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(SyncOriginPayload.ID, SyncOriginPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(BiteAnimPayload.ID, BiteAnimPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(BatStatePayload.ID, BatStatePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(AnimPayload.ID, AnimPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(FireballPayload.ID, FireballPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(EntityFlagPayload.ID, EntityFlagPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(MaturityPayload.ID, MaturityPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(FxPayload.ID, FxPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ChooseOriginPayload.ID, ChooseOriginPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(UseAbilityPayload.ID, UseAbilityPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(AirJumpPayload.ID, AirJumpPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(AltarOffersPayload.ID, AltarOffersPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(AltarChoicePayload.ID, AltarChoicePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(LeapPayload.ID, LeapPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(CasinoPayload.ID, CasinoPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(ChooseOriginPayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (OriginData.hasChosen(player)) {
				OriginData.sync(player);
				return;
			}
			RaceRegistry.get(payload.race()).ifPresent(race -> OriginData.set(player, race));
		});

		ServerPlayNetworking.registerGlobalReceiver(UseAbilityPayload.ID,
				(payload, context) -> RaceManager.useAbility(context.player()));

		ServerPlayNetworking.registerGlobalReceiver(AirJumpPayload.ID,
				(payload, context) -> BatFormPower.of(context.player()).ifPresent(power -> power.airJump(context.player())));

		ServerPlayNetworking.registerGlobalReceiver(AltarChoicePayload.ID,
				(payload, context) -> RaceManager.choose(context.player(), payload.option()));

		ServerPlayNetworking.registerGlobalReceiver(LeapPayload.ID, (payload, context) ->
				MountainLeapPower.of(context.player()).ifPresent(power -> {
					if (payload.released()) power.leap(context.player(), payload.charge());
					else power.charging(context.player(), true);
				}));

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			OriginData.sync(player);
			if (OriginData.hasChosen(player)) {
				RaceManager.apply(player);
			} else {
				OriginData.requestSelection(player);
			}
		});
	}

	public static void sendNear(ServerWorld world, Vec3d pos, double radius, CustomPayload payload) {
		for (ServerPlayerEntity p : PlayerLookup.around(world, pos, radius)) {
			if (ServerPlayNetworking.canSend(p, payload.getId())) ServerPlayNetworking.send(p, payload);
		}
	}

	public static void sendAround(CustomPayload payload, Entity... entities) {
		Set<ServerPlayerEntity> targets = new LinkedHashSet<>();
		for (Entity e : entities) {
			if (e == null) continue;
			targets.addAll(PlayerLookup.tracking(e));
			if (e instanceof ServerPlayerEntity sp) targets.add(sp);
		}
		for (ServerPlayerEntity p : targets) {
			if (ServerPlayNetworking.canSend(p, payload.getId())) ServerPlayNetworking.send(p, payload);
		}
	}
}
