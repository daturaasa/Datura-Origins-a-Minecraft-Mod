package com.datura.origins.fabric.client.anim;

import com.datura.origins.fabric.power.types.WingFlightPower;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.math.Vec3d;

public final class WingFlight {
	private static boolean jumpHeld;

	private WingFlight() {
	}

	public static void tick(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		boolean pressed = player != null && client.currentScreen == null && client.options.jumpKey.isPressed();
		if (player == null) {
			jumpHeld = false;
			return;
		}
		WingFlightPower power = WingFlightPower.of(player).orElse(null);
		if (power != null) {
			if (pressed && !jumpHeld && !player.isFallFlying() && player.getVelocity().y < 0.2 && WingFlightPower.canKeepFlying(player)
					&& player.checkFallFlying() && player.networkHandler != null) {
				player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
			}
			if (player.isFallFlying() && !player.isSneaking()) boost(player, power.speed());
		}
		jumpHeld = pressed;
	}

	private static void boost(ClientPlayerEntity player, float speed) {
		Vec3d look = player.getRotationVector();
		Vec3d v = player.getVelocity();
		player.setVelocity(v.add(
				look.x * 0.1 + (look.x * speed - v.x) * 0.5,
				look.y * 0.1 + (look.y * speed - v.y) * 0.5,
				look.z * 0.1 + (look.z * speed - v.z) * 0.5));
	}
}
