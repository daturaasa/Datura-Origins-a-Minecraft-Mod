package com.datura.origins.fabric.mixin;

import com.datura.origins.fabric.power.types.AnimalPeacePower;
import com.datura.origins.fabric.power.types.MonsterTrucePower;
import com.datura.origins.fabric.power.types.StealthPower;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin {
	@Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
	private void datura$stealth(LivingEntity target, CallbackInfo ci) {
		MobEntity self = (MobEntity) (Object) this;
		if (target instanceof PlayerEntity p && StealthPower.hidden(p)
				|| target != null && (MonsterTrucePower.blocks(self, target) || AnimalPeacePower.blocks(self, target))) {
			((MobEntity) (Object) this).setTarget(null);
			ci.cancel();
		}
	}
}
