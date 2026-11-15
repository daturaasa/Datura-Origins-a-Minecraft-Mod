package com.datura.origins.fabric.client;

import com.datura.origins.fabric.client.anim.AbilityAnimations;
import com.datura.origins.fabric.client.anim.BatDisguise;
import com.datura.origins.fabric.client.anim.RaceEffects;
import com.datura.origins.fabric.client.anim.SpellEffects;
import com.datura.origins.fabric.client.anim.WingFlight;
import com.datura.origins.fabric.client.gui.AltarScreen;
import com.datura.origins.fabric.client.gui.IntroScreen;
import com.datura.origins.fabric.client.gui.OriginSelectionScreen;
import com.datura.origins.fabric.client.gui.SelectionMusic;
import com.datura.origins.fabric.client.hud.CasinoHud;
import com.datura.origins.fabric.client.render.ChainRenderer;
import com.datura.origins.fabric.client.render.FoxCloneRenderer;
import com.datura.origins.fabric.client.render.LilyPadRenderer;
import com.datura.origins.fabric.client.render.RaceModelFeature;
import com.datura.origins.fabric.client.render.SoulOrbRenderer;
import com.datura.origins.fabric.client.render.SpellProjectileRenderer;
import com.datura.origins.fabric.entity.ModEntities;
import com.datura.origins.fabric.network.AirJumpPayload;
import com.datura.origins.fabric.network.AltarOffersPayload;
import com.datura.origins.fabric.network.AnimPayload;
import com.datura.origins.fabric.network.BatStatePayload;
import com.datura.origins.fabric.network.BiteAnimPayload;
import com.datura.origins.fabric.network.CasinoPayload;
import com.datura.origins.fabric.network.EntityFlagPayload;
import com.datura.origins.fabric.network.FireballPayload;
import com.datura.origins.fabric.network.FxPayload;
import com.datura.origins.fabric.network.MaturityPayload;
import com.datura.origins.fabric.network.OpenSelectionPayload;
import com.datura.origins.fabric.network.SyncOriginPayload;
import com.datura.origins.fabric.network.UseAbilityPayload;
import com.datura.origins.fabric.power.EntityFlags;
import com.datura.origins.fabric.power.ability.BatFormState;
import com.datura.origins.fabric.power.types.BatFormPower;
import com.datura.origins.fabric.power.types.MaturityPower;
import com.datura.origins.fabric.race.RaceAccess;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.ZombieEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class DaturaOriginsClient implements ClientModInitializer {
	public static final KeyBinding OPEN_ORIGINS = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.datura_origins.origins", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, "category.datura_origins"));
	public static final KeyBinding ABILITY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.datura_origins.ability", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Z, "category.datura_origins"));

	private static boolean jumpHeld;

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.FOX_CLONE, FoxCloneRenderer::new);
		EntityRendererRegistry.register(ModEntities.SPELL_PROJECTILE, SpellProjectileRenderer::new);
		EntityRendererRegistry.register(ModEntities.UNDEAD_MINION, ZombieEntityRenderer::new);
		EntityRendererRegistry.register(ModEntities.SOUL_ORB, SoulOrbRenderer::new);
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
			if (renderer instanceof PlayerEntityRenderer playerRenderer) {
				helper.register(new RaceModelFeature(playerRenderer));
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(OpenSelectionPayload.ID,
				(payload, context) -> context.client().setScreen(new IntroScreen()));
		ClientPlayNetworking.registerGlobalReceiver(SyncOriginPayload.ID,
				(payload, context) -> ClientOrigin.set(payload.race()));

		ClientPlayNetworking.registerGlobalReceiver(BiteAnimPayload.ID, (payload, context) -> {
			if (context.client().world == null) return;
			if (payload.durationTicks() > 0) {
				AbilityAnimations.start(context.client().world, payload.vampireId(), "bite", payload.durationTicks());
				AbilityAnimations.start(context.client().world, payload.victimId(), "bitten", payload.durationTicks());
			} else {
				AbilityAnimations.stop(context.client().world, payload.vampireId(), "bite");
				AbilityAnimations.stop(context.client().world, payload.victimId(), "bitten");
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(AnimPayload.ID, (payload, context) -> {
			if (context.client().world == null) return;
			if (payload.channel().startsWith("fx:")) {
				RaceEffects.fx(payload.channel().substring(3), payload.entityId(), payload.durationTicks());
			} else if (payload.durationTicks() > 0) {
				AbilityAnimations.start(context.client().world, payload.entityId(), payload.channel(), payload.durationTicks());
			} else {
				AbilityAnimations.stop(context.client().world, payload.entityId(), payload.channel());
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(FireballPayload.ID, (payload, context) ->
				RaceEffects.fireball(payload.id(), new Vec3d(payload.x(), payload.y(), payload.z()),
						new Vec3d(payload.vx(), payload.vy(), payload.vz()), payload.state()));

		ClientPlayNetworking.registerGlobalReceiver(FxPayload.ID, (payload, context) ->
				RaceEffects.fxAt(payload.name(), new Vec3d(payload.x(), payload.y(), payload.z()), payload.a(), payload.b(), payload.c()));

		ClientPlayNetworking.registerGlobalReceiver(EntityFlagPayload.ID, (payload, context) ->
				EntityFlags.clientSet(payload.entityId(), payload.flag(), payload.value()));

		ClientPlayNetworking.registerGlobalReceiver(MaturityPayload.ID, (payload, context) ->
				MaturityPower.clientPoints = payload.points());

		ClientPlayNetworking.registerGlobalReceiver(BatStatePayload.ID, (payload, context) -> {
			if (payload.bat()) {
				BatFormState.CLIENT.add(payload.entityId());
			} else {
				BatFormState.CLIENT.remove(payload.entityId());
				BatDisguise.forget(payload.entityId());
			}
			if (context.client().world != null) {
				Entity e = context.client().world.getEntityById(payload.entityId());
				if (e != null) e.calculateDimensions();
			}
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientOrigin.set("");
			RaceAccess.clientPlayerId = null;
			MaturityPower.clientPoints = 0;
			BatFormState.CLIENT.clear();
			EntityFlags.CLIENT.clear();
			BatDisguise.clear();
			AbilityAnimations.clear();
			RaceEffects.clear();
			SpellEffects.clear();
			RaceModelFeature.clear();
			CasinoHud.clear();
			jumpHeld = false;
		});

		WorldRenderEvents.AFTER_ENTITIES.register(LilyPadRenderer::render);
		WorldRenderEvents.AFTER_ENTITIES.register(ChainRenderer::render);
		HudRenderCallback.EVENT.register(CasinoHud::render);
		ClientPlayNetworking.registerGlobalReceiver(CasinoPayload.ID, (payload, context) ->
				CasinoHud.start(payload.multiplier(), payload.ticks()));
		ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> RaceModelFeature.forget(entity.getId()));
		ClientPlayNetworking.registerGlobalReceiver(AltarOffersPayload.ID, (payload, context) ->
				context.client().setScreen(new AltarScreen(payload.enchantments(), payload.levels(), payload.seed())));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			SelectionMusic.tick(client);
			if (client.player != null) {
				RaceAccess.clientPlayerId = client.player.getUuid();
			}
			while (OPEN_ORIGINS.wasPressed()) {
				if (client.player != null && client.currentScreen == null) {
					client.setScreen(new OriginSelectionScreen(false));
				}
			}
			while (ABILITY.wasPressed()) {
				if (client.player != null && ClientOrigin.hasChosen() && ClientPlayNetworking.canSend(UseAbilityPayload.ID)) {
					ClientPlayNetworking.send(UseAbilityPayload.INSTANCE);
				}
			}
			tickAirJump(client);
			WingFlight.tick(client);
			RaceEffects.tick(client);
			SpellEffects.tick(client);
		});
	}

	private static void tickAirJump(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		boolean pressed = player != null && client.currentScreen == null && client.options.jumpKey.isPressed();
		if (pressed && !jumpHeld && player != null && !player.isOnGround() && !player.isTouchingWater()
				&& !player.getAbilities().flying && !player.hasVehicle() && !player.isClimbing() && !player.isFallFlying()
				&& !BatFormState.isBat(player) && BatFormPower.of(player).isPresent()
				&& ClientPlayNetworking.canSend(AirJumpPayload.ID)) {
			ClientPlayNetworking.send(AirJumpPayload.INSTANCE);
		}
		jumpHeld = pressed;
	}
}
