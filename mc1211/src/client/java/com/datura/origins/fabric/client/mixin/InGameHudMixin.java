package com.datura.origins.fabric.client.mixin;

import com.datura.origins.fabric.client.hud.MaturityHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
	@Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
	private void datura$maturityBar(DrawContext context, int x, CallbackInfo ci) {
		if (MaturityHud.render(context, x)) ci.cancel();
	}

	@Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
	private void datura$hideLevel(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		if (MaturityHud.active()) ci.cancel();
	}
}
