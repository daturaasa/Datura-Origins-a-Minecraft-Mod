package com.datura.origins.fabric.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

final class ScrollArea {
	private static final float STEP = 14.0F;

	private int x;
	private int y;
	private int width;
	private int height;
	private int content;
	private float offset;
	private float target;
	private float activity;

	void setBounds(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = Math.max(0, width);
		this.height = Math.max(0, height);
		clamp();
	}

	void setContent(int content) {
		this.content = content;
		clamp();
	}

	void reset() {
		offset = 0.0F;
		target = 0.0F;
	}

	int x() {
		return x;
	}

	int y() {
		return y;
	}

	int width() {
		return width;
	}

	int height() {
		return height;
	}

	float offset() {
		return offset;
	}

	int maxScroll() {
		return Math.max(0, content - height);
	}

	boolean contains(double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	boolean scroll(double mouseX, double mouseY, double amount) {
		if (!contains(mouseX, mouseY) || maxScroll() == 0) {
			return false;
		}
		target -= (float) amount * STEP;
		clamp();
		activity = 1.0F;
		return true;
	}

	void ensureVisible(int top, int bottom) {
		if (top < target) {
			target = top;
		} else if (bottom > target + height) {
			target = bottom - height;
		}
		clamp();
	}

	void update() {
		offset = MathHelper.lerp(0.35F, offset, target);
		if (Math.abs(offset - target) < 0.1F) {
			offset = target;
		}
		activity = Math.max(0.0F, activity - 0.02F);
	}

	void begin(DrawContext context) {
		context.enableScissor(x, y, x + width, y + height);
	}

	void end(DrawContext context, float alpha) {
		context.disableScissor();
		int max = maxScroll();
		if (max == 0) {
			return;
		}
		int trackX = x + width + 4;
		int thumb = Math.max(10, Math.round(height * (height / (float) content)));
		int thumbY = y + Math.round((height - thumb) * (offset / max));
		float strength = 0.25F + 0.5F * activity;
		context.fill(trackX, thumbY, trackX + 1, thumbY + thumb, Draw.fade(0xFFD9A3B9, alpha * strength));
	}

	private void clamp() {
		target = MathHelper.clamp(target, 0.0F, maxScroll());
		offset = MathHelper.clamp(offset, 0.0F, maxScroll());
	}
}
