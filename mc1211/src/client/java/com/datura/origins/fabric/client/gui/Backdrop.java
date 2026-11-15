package com.datura.origins.fabric.client.gui;

import net.minecraft.client.gui.DrawContext;

final class Backdrop {
	static final Style INTRO = new Style(0xFF0D241B);
	static final Style SELECTION = new Style(0xFF0D241B);

	private final Style style;

	Backdrop(Style style) {
		this.style = style;
	}

	void render(DrawContext context, int width, int height, float time, double mouseX, double mouseY) {
		context.fill(0, 0, width, height, style.color);
	}

	record Style(int color) {
	}
}
