package com.datura.origins.fabric.mixin;

import com.datura.origins.fabric.power.EntityFlags;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.screen.slot.ArmorSlot")
public abstract class ArmorSlotMixin {
	@Inject(method = "canTakeItems", at = @At("HEAD"), cancellable = true)
	private void datura$curseImmune(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
		boolean immune = player instanceof ServerPlayerEntity sp ? EntityFlags.has(sp, "curse_immune") : EntityFlags.clientHas(player, "curse_immune");
		if (immune && !((Slot) (Object) this).getStack().isEmpty()) cir.setReturnValue(true);
	}
}
