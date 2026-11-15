package com.datura.origins.fabric.mixin;

import com.datura.origins.fabric.power.EntityFlags;
import com.datura.origins.fabric.power.types.MarkerPower;
import com.datura.origins.fabric.power.types.StealthPower;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
	@Inject(method = "playStepSound", at = @At("HEAD"), cancellable = true)
	private void datura$silentSteps(BlockPos pos, BlockState state, CallbackInfo ci) {
		if ((Object) this instanceof PlayerEntity p && StealthPower.silent(p)) ci.cancel();
	}

	@Inject(method = "isFireImmune", at = @At("HEAD"), cancellable = true)
	private void datura$fireproof(CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof PlayerEntity p && fireproof(p)) cir.setReturnValue(true);
	}

	private static boolean fireproof(PlayerEntity p) {
		return p.getWorld().isClient ? EntityFlags.clientHas(p, "fireproof")
				: p instanceof ServerPlayerEntity sp && EntityFlags.has(sp, "fireproof");
	}

	@Inject(method = "updateSwimming", at = @At("HEAD"), cancellable = true)
	private void datura$noSwimming(CallbackInfo ci) {
		if ((Object) this instanceof PlayerEntity p && MarkerPower.has(p, "no_swim")) {
			p.setSwimming(false);
			ci.cancel();
		}
	}
}
