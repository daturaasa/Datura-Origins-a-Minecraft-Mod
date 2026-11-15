package com.datura.origins.fabric.client.mixin;

import com.datura.origins.fabric.power.EntityFlags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
	@Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
	private void datura$goldWardOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (EntityFlags.clientHas(entity, "gold_ward")) cir.setReturnValue(true);
	}
}
