package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.OriginData;
import com.datura.origins.fabric.network.AnimPayload;
import com.datura.origins.fabric.network.BatStatePayload;
import com.datura.origins.fabric.network.Networking;
import com.datura.origins.fabric.power.ConditionalPower;
import com.datura.origins.fabric.power.JsonParams;
import com.datura.origins.fabric.power.ability.BatFormState;
import com.datura.origins.fabric.power.ability.VampireBiteAbility;
import com.datura.origins.fabric.race.RaceAccess;
import com.datura.origins.fabric.race.RaceManager;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BatFormPower extends ConditionalPower {
	private static final float DEFAULT_FLY_SPEED = 0.05f;
	private static final Map<UUID, Long> READY = new ConcurrentHashMap<>();

	private final float flySpeed;
	private final int cooldown;
	private final float exhaustion;

	public BatFormPower(JsonObject json) {
		super(json);
		this.flySpeed = JsonParams.num(json, "fly_speed", 0.06f);
		this.cooldown = JsonParams.integer(json, "cooldown", 600);
		this.exhaustion = JsonParams.num(json, "exhaustion_per_second", 0f);
	}

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(BatFormPower::tickAll);

		EntityTrackingEvents.START_TRACKING.register((tracked, viewer) -> {
			if (tracked instanceof PlayerEntity p && BatFormState.SERVER.contains(p.getUuid())
					&& ServerPlayNetworking.canSend(viewer, BatStatePayload.ID)) {
				ServerPlayNetworking.send(viewer, new BatStatePayload(p.getId(), true));
			}
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (BatFormState.SERVER.remove(newPlayer.getUuid())) {
				Networking.sendAround(new BatStatePayload(newPlayer.getId(), false), newPlayer);
			}
		});

		AttackEntityCallback.EVENT.register((p, w, h, e, hit) -> BatFormState.isBat(p) ? ActionResult.FAIL : ActionResult.PASS);
		AttackBlockCallback.EVENT.register((p, w, h, pos, dir) -> BatFormState.isBat(p) ? ActionResult.FAIL : ActionResult.PASS);
		UseBlockCallback.EVENT.register((p, w, h, hit) -> BatFormState.isBat(p) ? ActionResult.FAIL : ActionResult.PASS);
		UseEntityCallback.EVENT.register((p, w, h, e, hit) -> BatFormState.isBat(p) ? ActionResult.FAIL : ActionResult.PASS);
		UseItemCallback.EVENT.register((p, w, h) -> BatFormState.isBat(p)
				? TypedActionResult.fail(p.getStackInHand(h)) : TypedActionResult.pass(p.getStackInHand(h)));
	}

	public static Optional<BatFormPower> of(PlayerEntity player) {
		return RaceAccess.getRace(player).flatMap(r -> r.powers().stream()
				.filter(p -> p instanceof BatFormPower).map(p -> (BatFormPower) p).findFirst());
	}

	public static void resetCooldown(ServerPlayerEntity player) {
		READY.remove(player.getUuid());
	}

	public void airJump(ServerPlayerEntity player) {
		if (BatFormState.SERVER.contains(player.getUuid()) || VampireBiteAbility.isBiting(player)) return;
		if (player.isOnGround() || player.isTouchingWater() || player.hasVehicle() || player.isClimbing()
				|| player.isFallFlying() || player.getAbilities().flying || player.isSpectator()) return;
		if (!isActive(player)) {
			player.sendMessage(Text.translatable("datura_origins.message.bat_hungry").formatted(Formatting.GRAY), true);
			return;
		}
		long ready = READY.getOrDefault(player.getUuid(), 0L);
		if (!OriginData.NO_COOLDOWN.contains(player.getUuid()) && RaceManager.ticks() < ready) {
			long secs = (ready - RaceManager.ticks() + 19) / 20;
			player.sendMessage(Text.translatable("datura_origins.message.bat_cooldown", secs).formatted(Formatting.GRAY), true);
			return;
		}
		transform(player);
	}

	@Override
	public void tick(ServerPlayerEntity player) {
		if (!BatFormState.SERVER.contains(player.getUuid())) return;
		if (exhaustion > 0 && player.age % 20 == 0) player.addExhaustion(exhaustion);
		if (!isActive(player) && revert(player, true)) startCooldown(player);
	}

	@Override
	public void onRemove(ServerPlayerEntity player) {
		if (BatFormState.SERVER.contains(player.getUuid())) revert(player, false);
		READY.remove(player.getUuid());
	}

	private void startCooldown(ServerPlayerEntity player) {
		if (cooldown > 0) READY.put(player.getUuid(), RaceManager.ticks() + cooldown);
	}

	private void transform(ServerPlayerEntity player) {
		BatFormState.SERVER.add(player.getUuid());
		PlayerAbilities abilities = player.getAbilities();
		abilities.allowFlying = true;
		abilities.flying = true;
		abilities.setFlySpeed(flySpeed);
		player.sendAbilitiesUpdate();
		player.calculateDimensions();
		Networking.sendAround(new BatStatePayload(player.getId(), true), player);
		poof(player, true);
	}

	private static boolean revert(ServerPlayerEntity player, boolean checkSpace) {
		if (checkSpace) {
			Box standing = new Box(player.getX() - 0.3, player.getY(), player.getZ() - 0.3,
					player.getX() + 0.3, player.getY() + 1.8, player.getZ() + 0.3);
			if (!player.getWorld().isSpaceEmpty(player, standing)) {
				player.sendMessage(Text.translatable("datura_origins.message.bat_no_room").formatted(Formatting.GRAY), true);
				return false;
			}
		}
		BatFormState.SERVER.remove(player.getUuid());
		PlayerAbilities abilities = player.getAbilities();
		boolean keepsFlight = player.isCreative() || player.isSpectator();
		abilities.allowFlying = keepsFlight;
		if (!keepsFlight) abilities.flying = false;
		abilities.setFlySpeed(DEFAULT_FLY_SPEED);
		player.sendAbilitiesUpdate();
		player.calculateDimensions();
		if (!player.isOnGround() && !keepsFlight) {
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 50, 0, false, false, true));
		}
		Networking.sendAround(new BatStatePayload(player.getId(), false), player);
		poof(player, false);
		return true;
	}

	private static void poof(ServerPlayerEntity player, boolean toBat) {
		if (!(player.getWorld() instanceof ServerWorld sw)) return;
		double y = player.getY() + (toBat ? 0.9 : 0.4);
		sw.spawnParticles(ParticleTypes.LARGE_SMOKE, player.getX(), y, player.getZ(), 8, 0.3, 0.45, 0.3, 0.02);
		Networking.sendAround(new AnimPayload(player.getId(), "fx:bat", toBat ? 1 : 0), player);
		sw.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE,
				SoundCategory.PLAYERS, 0.5f, toBat ? 0.8f : 0.6f);
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				toBat ? SoundEvents.ENTITY_BAT_TAKEOFF : SoundEvents.ENTITY_BAT_HURT,
				SoundCategory.PLAYERS, 0.7f, toBat ? 1.0f : 0.6f);
	}

	private static void tickAll(MinecraftServer server) {
		if (BatFormState.SERVER.isEmpty()) return;
		for (UUID id : new ArrayList<>(BatFormState.SERVER)) {
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(id);
			if (player == null) {
				BatFormState.SERVER.remove(id);
				continue;
			}
			PlayerAbilities abilities = player.getAbilities();
			if (!abilities.flying) {
				Optional<BatFormPower> power = of(player);
				if (revert(player, true)) {
					power.ifPresent(p -> p.startCooldown(player));
					continue;
				}
				abilities.allowFlying = true;
				abilities.flying = true;
				player.sendAbilitiesUpdate();
			}
			player.fallDistance = 0;
		}
	}
}
