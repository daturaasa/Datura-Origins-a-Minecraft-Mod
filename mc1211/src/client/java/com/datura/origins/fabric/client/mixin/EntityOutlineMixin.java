package com.datura.origins.fabric.client.mixin;

import com.datura.origins.fabric.power.EntityFlags;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityOutlineMixin {
	@Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
	private void datura$goldWard(CallbackInfoReturnable<Integer> cir) {
		if (EntityFlags.clientHas((Entity) (Object) this, "gold_ward")) cir.setReturnValue(0xFFD45E);
	}
}
