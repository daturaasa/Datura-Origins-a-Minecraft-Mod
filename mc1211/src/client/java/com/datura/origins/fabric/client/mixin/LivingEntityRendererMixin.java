package com.datura.origins.fabric.client.mixin;

import com.datura.origins.fabric.client.model.ModelAnimator;
import com.datura.origins.fabric.client.model.RaceModels;
import com.datura.origins.fabric.entity.FoxCloneEntity;
import com.datura.origins.fabric.power.EntityFlags;
import com.datura.origins.fabric.power.types.ClaustrophobiaPower;
import com.datura.origins.fabric.power.types.StatuePower;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	@Unique
	private static int datura$tint = -1;

	@Inject(method = "isShaking", at = @At("RETURN"), cancellable = true)
	private void datura$crampedShake(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() && EntityFlags.clientHas(entity, ClaustrophobiaPower.FLAG)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
			at = @At("HEAD"))
	private void datura$tintStart(LivingEntity entity, float yaw, float tickDelta, MatrixStack matrices,
								  VertexConsumerProvider vertices, int light, CallbackInfo ci) {
		if (entity instanceof FoxCloneEntity c) datura$tint = 0xA8000000 | (c.isSnow() ? 0xC8E6FF : 0xFFC9A0);
		else if (entity instanceof PlayerEntity p && RaceModels.isGhost(p)) datura$tint = 0x8CFFFFFF;
		else if (entity instanceof PlayerEntity p && EntityFlags.clientHas(p, StatuePower.FLAG)) datura$tint = 0xFF8E8E8E;
		else datura$tint = -1;
	}

	@Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
			at = @At("RETURN"))
	private void datura$tintEnd(LivingEntity entity, float yaw, float tickDelta, MatrixStack matrices,
								VertexConsumerProvider vertices, int light, CallbackInfo ci) {
		datura$tint = -1;
	}

	@SuppressWarnings("unchecked")
	@Inject(method = "getRenderLayer", at = @At("HEAD"), cancellable = true)
	private void datura$cloneLayer(LivingEntity entity, boolean showBody, boolean translucent, boolean showOutline,
								   CallbackInfoReturnable<RenderLayer> cir) {
		if (showBody && (entity instanceof FoxCloneEntity || entity instanceof PlayerEntity p && RaceModels.isGhost(p))) {
			cir.setReturnValue(RenderLayer.getItemEntityTranslucentCull(
					((EntityRenderer<LivingEntity>) (Object) this).getTexture(entity)));
		}
	}

	@ModifyArg(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"),
			index = 4, require = 0)
	private int datura$cloneTint(int color) {
		return datura$tint != -1 ? datura$tint : color;
	}

	@Inject(method = "setupTransforms", at = @At("TAIL"))
	private void datura$mermaidSit(LivingEntity entity, MatrixStack matrices, float animationProgress, float bodyYaw, float tickDelta,
								   float scale, CallbackInfo ci) {
		if (!(entity instanceof PlayerEntity player)) return;
		float sit = ModelAnimator.sitAmount(player, player.age + tickDelta);
		if (sit > 0.001F) matrices.translate(0.0F, -0.66F * sit, 0.0F);
	}
}
