package com.datura.origins.fabric.client.mixin;

import com.datura.origins.fabric.client.model.PartModel;
import com.datura.origins.fabric.client.model.RaceModels;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
	private PlayerEntityRendererMixin(EntityRendererFactory.Context context, PlayerEntityModel<AbstractClientPlayerEntity> model, float shadowRadius) {
		super(context, model, shadowRadius);
	}

	@Inject(method = "setModelPose", at = @At("TAIL"))
	private void datura$hideLegs(AbstractClientPlayerEntity player, CallbackInfo ci) {
		if (player.isSpectator()) return;
		for (Identifier id : RaceModels.modelsOf(player)) {
			if (!PartModel.get(id).hidesLegs()) continue;
			PlayerEntityModel<AbstractClientPlayerEntity> model = getModel();
			model.leftLeg.visible = false;
			model.rightLeg.visible = false;
			model.leftPants.visible = false;
			model.rightPants.visible = false;
			return;
		}
	}
}
