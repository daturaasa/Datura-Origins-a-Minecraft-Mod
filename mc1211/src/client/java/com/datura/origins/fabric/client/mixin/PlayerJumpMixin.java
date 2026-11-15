package com.datura.origins.fabric.client.mixin;

import com.datura.origins.fabric.client.anim.MountainLeap;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerJumpMixin {
	@Inject(method = "jump", at = @At("HEAD"), cancellable = true)
	private void datura$chargeLeap(CallbackInfo ci) {
		if (MountainLeap.holdsJump((PlayerEntity) (Object) this)) ci.cancel();
	}
}
