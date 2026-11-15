package com.datura.origins.fabric.power.ability;

import com.datura.origins.fabric.DaturaOriginsMod;
import com.datura.origins.fabric.power.EntityFlags;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;

public final class Channeling {
	public static final String FLAG = "channeling";
	private static final Identifier SLOW = DaturaOriginsMod.id("channel_slow");

	private Channeling() {}

	public static boolean active(PlayerEntity p) {
		return p.getWorld().isClient ? EntityFlags.clientHas(p, FLAG) : p instanceof ServerPlayerEntity sp && EntityFlags.has(sp, FLAG);
	}

	public static void set(ServerPlayerEntity p, boolean on) {
		EntityFlags.set(p, FLAG, on);
		var inst = p.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
		if (inst == null) return;
		inst.removeModifier(SLOW);
		if (on) inst.addTemporaryModifier(new EntityAttributeModifier(SLOW, -0.7, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	}

	public static void bootstrap() {
		AttackEntityCallback.EVENT.register((p, w, h, e, hit) -> active(p) ? ActionResult.FAIL : ActionResult.PASS);
		UseItemCallback.EVENT.register((p, w, h) -> active(p) ? TypedActionResult.fail(p.getStackInHand(h)) : TypedActionResult.pass(p.getStackInHand(h)));
		UseBlockCallback.EVENT.register((p, w, h, hit) -> active(p) ? ActionResult.FAIL : ActionResult.PASS);
	}
}
