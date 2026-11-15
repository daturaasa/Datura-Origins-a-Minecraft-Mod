package com.datura.origins.fabric.client.anim;

import com.datura.origins.fabric.client.mixin.LivingEntityInvoker;
import com.datura.origins.fabric.network.LeapPayload;
import com.datura.origins.fabric.power.types.MountainLeapPower;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class MountainLeap {
	private static final int TAP = 5;
	private static int charge = -1;
	private static boolean bypass;

	private MountainLeap() {
	}

	public static boolean holdsJump(PlayerEntity player) {
		if (bypass || !(player instanceof ClientPlayerEntity local)) return false;
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player != local || !client.options.jumpKey.isPressed() || MountainLeapPower.of(local).isEmpty()) return false;
		if (local.isTouchingWater() || local.isClimbing() || local.hasVehicle() || local.getAbilities().flying) return false;
		if (charge < 0) {
			charge = 0;
			send(false, 0);
		}
		return true;
	}

	public static void tick(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		if (charge < 0 || player == null) {
			charge = -1;
			return;
		}
		MountainLeapPower power = MountainLeapPower.of(player).orElse(null);
		if (power == null || !player.isOnGround() || player.isTouchingWater()) {
			charge = -1;
			send(true, 0);
			return;
		}
		if (client.options.jumpKey.isPressed() && client.currentScreen == null) {
			charge = Math.min(charge + 1, power.maxCharge());
			if (charge >= TAP) showCharge(client, charge / (float) power.maxCharge());
			return;
		}
		int held = charge;
		charge = -1;
		if (held < TAP) {
			send(true, 0);
			bypass = true;
			((LivingEntityInvoker) player).datura$jump();
			bypass = false;
			return;
		}
		float c = held / (float) power.maxCharge();
		float height = 1.25F + (power.maxHeight() - 1.25F) * (float) Math.pow(c, 1.5);
		Vec3d look = player.getRotationVector();
		Vec3d flat = new Vec3d(look.x, 0, look.z);
		flat = flat.lengthSquared() < 1.0E-4 ? Vec3d.ZERO : flat.normalize().multiply(power.forward() * c);
		Vec3d v = player.getVelocity();
		player.setVelocity(v.x + flat.x, launchSpeed(height), v.z + flat.z);
		player.fallDistance = 0;
		client.inGameHud.setOverlayMessage(Text.empty(), false);
		send(true, held);
	}

	private static void showCharge(MinecraftClient client, float c) {
		int filled = MathHelper.clamp(Math.round(c * 20), 0, 20);
		Text bar = Text.literal("|".repeat(filled)).formatted(c >= 1 ? Formatting.GOLD : Formatting.WHITE)
				.append(Text.literal("|".repeat(20 - filled)).formatted(Formatting.DARK_GRAY));
		client.inGameHud.setOverlayMessage(bar, false);
	}

	private static double launchSpeed(float height) {
		double low = 0.42, high = 5.0;
		for (int i = 0; i < 30; i++) {
			double mid = (low + high) / 2;
			if (apex(mid) < height) low = mid;
			else high = mid;
		}
		return low;
	}

	private static double apex(double speed) {
		double y = 0, v = speed;
		while (v > 0) {
			y += v;
			v = (v - 0.08) * 0.98;
		}
		return y;
	}

	private static void send(boolean released, int held) {
		if (ClientPlayNetworking.canSend(LeapPayload.ID)) ClientPlayNetworking.send(new LeapPayload(released, held));
	}
}
