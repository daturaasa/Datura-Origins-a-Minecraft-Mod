package com.datura.origins.fabric.mixin;

import com.datura.origins.fabric.power.types.EffectImmunityPower;
import com.datura.origins.fabric.power.types.WingFlightPower;
import com.datura.origins.fabric.power.types.InvertedPotionPower;
import com.datura.origins.fabric.power.types.MarkerPower;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.tag.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "swimUpward", at = @At("HEAD"), cancellable = true)
	private void datura$noSwim(TagKey<Fluid> fluid, CallbackInfo ci) {
		if ((Object) this instanceof PlayerEntity p && MarkerPower.has(p, "no_swim")) ci.cancel();
	}

	@Inject(method = "canHaveStatusEffect", at = @At("HEAD"), cancellable = true)
	private void datura$effectImmunity(StatusEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof PlayerEntity p && EffectImmunityPower.blocks(p, effect)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "hasInvertedHealingAndHarm", at = @At("HEAD"), cancellable = true)
	private void datura$invertedPotions(CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof PlayerEntity p && InvertedPotionPower.inverts(p)) cir.setReturnValue(true);
	}

	@Inject(method = "tickFallFlying", at = @At("HEAD"), cancellable = true)
	private void datura$fairyGlide(CallbackInfo ci) {
		if ((Object) this instanceof PlayerEntity p && p.isFallFlying() && WingFlightPower.canKeepFlying(p) && WingFlightPower.of(p).isPresent()) {
			ci.cancel();
		}
	}
}
