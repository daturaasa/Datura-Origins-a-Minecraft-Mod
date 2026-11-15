package com.datura.origins.fabric.client.mixin;

import com.datura.origins.fabric.client.anim.BatDisguise;
import com.datura.origins.fabric.power.ability.BatFormState;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.BatEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
	@Inject(method = "render(Lnet/minecraft/entity/Entity;DDDFFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
			at = @At("HEAD"), cancellable = true)
	private void datura$renderAsBat(Entity entity, double x, double y, double z, float yaw, float tickDelta,
									MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
		if (entity instanceof AbstractClientPlayerEntity player && BatFormState.isBat(player)) {
			BatEntity bat = BatDisguise.prepare(player);
			((EntityRenderDispatcher) (Object) this).render(bat, x, y, z, yaw, tickDelta, matrices, vertexConsumers, light);
			ci.cancel();
		}
	}
}
