package com.datura.origins.fabric.mixin;

import com.datura.origins.fabric.power.types.MarkerPower;
import net.minecraft.block.PowderSnowBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PowderSnowBlock.class)
public abstract class PowderSnowBlockMixin {
	@Inject(method = "canWalkOnPowderSnow", at = @At("HEAD"), cancellable = true)
	private static void datura$snowWalker(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (entity instanceof PlayerEntity p && MarkerPower.has(p, "powder_snow_walker")) cir.setReturnValue(true);
	}
}
