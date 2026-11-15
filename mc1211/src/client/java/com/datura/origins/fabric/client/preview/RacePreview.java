package com.datura.origins.fabric.client.preview;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;

@FunctionalInterface
public interface RacePreview {
	void render(DrawContext context, LivingEntity player, float x, float y, float size, float yaw, float pitch, float delta);
}
