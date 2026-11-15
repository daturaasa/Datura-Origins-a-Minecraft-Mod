package com.datura.origins.fabric.client.gui;

import com.datura.origins.fabric.client.ClientOrigin;
import com.datura.origins.fabric.client.model.RaceModels;
import com.datura.origins.fabric.client.preview.RacePreviews;
import com.datura.origins.fabric.network.ChooseOriginPayload;
import com.datura.origins.race.Race;
import com.datura.origins.race.RaceRegistry;
import com.datura.origins.race.RomanNumerals;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Language;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class OriginSelectionScreen extends Screen {
	private static final int ROW_HEIGHT = 18;
	private static final int PANEL_PAD = 8;
	private static final long DETAIL_FADE_MS = 280L;

	private final Backdrop backdrop = new Backdrop(Backdrop.SELECTION);
	private final boolean fromIntro;
	private final long openedAt = Util.getMeasuringTimeMs();
	private final ScrollArea list = new ScrollArea();
	private final ScrollArea details = new ScrollArea();

	private static final int FORM_BAR = 20;

	private List<Race> races = List.of();
	private List<Race> forms = List.of();
	private int formIndex;
	private float[] formHover = new float[0];
	private int formY0, formY1;
	private Race chosenBase;
	private float[] rowHover = new float[0];
	private int selected;
	private long changedAt = openedAt;
	private Details layout = Details.EMPTY;

	private int leftX0, leftX1, rightX0, rightX1, panelTop, panelBottom;
	private int centerX0, centerX1;
	private int buttonX0, buttonX1, buttonY0, buttonY1;
	private int headerY;
	private int titleScale;
	private float buttonHover;

	private float yaw = 22.0F;
	private float pitch;
	private boolean dragging;
	private boolean dragged;
	private long lastInteraction = openedAt;
	private long lastFrame = openedAt;
	private boolean confirmed;
	private boolean viewOnly;
	private Race chosen;
	private boolean confirming;
	private long confirmAt;
	private float backHover;
	private float acceptHover;
	private int dialogX0, dialogX1, dialogY0, dialogY1;
	private int backX0, backX1, acceptX0, acceptX1, dialogButtonY0, dialogButtonY1;
	private List<OrderedText> dialogLines = List.of();

	public OriginSelectionScreen(boolean fromIntro) {
		super(Text.translatable("screen.datura_origins.select"));
		this.fromIntro = fromIntro;
	}

	@Override
	protected void init() {
		races = RaceRegistry.pages();
		if (rowHover.length != races.size()) {
			rowHover = new float[races.size()];
			chosen = fromIntro ? null : ClientOrigin.get().orElse(null);
			viewOnly = chosen != null;
			chosenBase = chosen == null ? null : RaceRegistry.baseOf(chosen);
			selected = Math.max(0, races.indexOf(viewOnly ? chosenBase : RaceRegistry.defaultRace()));
			loadForms();
		}

		int margin = MathHelper.clamp(Math.round(Math.min(width, height) * 0.04F), 6, 28);
		headerY = margin;
		Text title = getTitle();
		titleScale = textRenderer.getWidth(title) * 2 <= width - margin * 2 && height >= 220 ? 2 : 1;
		int headerBottom = headerY + 12 + 9 * titleScale + 8;

		int buttonWidth = MathHelper.clamp(Math.round(width * 0.2F), 110, 180);
		if (viewOnly) {
			buttonWidth = Math.min(width - margin * 2, Math.max(buttonWidth, textRenderer.getWidth(currentLabel()) + 30));
		}
		int buttonHeight = 20;
		buttonX0 = (width - buttonWidth) / 2;
		buttonX1 = buttonX0 + buttonWidth;
		buttonY1 = height - margin;
		buttonY0 = buttonY1 - buttonHeight;

		panelTop = headerBottom + 4;
		panelBottom = Math.max(panelTop + 40, buttonY0 - 10);

		int gap = Math.max(8, margin / 2);
		int column = MathHelper.clamp(Math.round(width * 0.26F), 110, 220);
		int minCenter = 90;
		if (width - margin * 2 - gap * 2 - column * 2 < minCenter) {
			column = Math.max(80, (width - margin * 2 - gap * 2 - minCenter) / 2);
		}
		leftX0 = margin;
		leftX1 = margin + column;
		rightX1 = width - margin;
		rightX0 = rightX1 - column;
		centerX0 = leftX1 + gap;
		centerX1 = rightX0 - gap;

		int listTop = panelTop + PANEL_PAD + 9 + 7;
		list.setBounds(leftX0 + 4, listTop, column - 12, panelBottom - PANEL_PAD + 2 - listTop);
		list.setContent(races.size() * ROW_HEIGHT);
		layoutDetails();
		layoutDialog();
	}

	private Text currentLabel() {
		return Text.translatable("screen.datura_origins.select.current", chosen == null ? Text.empty() : fullName(chosen));
	}

	private static Text fullName(Race race) {
		if (race.variantName() == null || race.variantName().isEmpty()) {
			return text(race.name());
		}
		return text(race.name()).copy().append(" (").append(text(race.variantName())).append(")");
	}

	private void layoutDialog() {
		int dialogWidth = Math.min(260, width - 40);
		int inner = dialogWidth - 24;
		dialogLines = textRenderer.wrapLines(Text.translatable("screen.datura_origins.confirm.body", fullName(current())), inner);
		int dialogHeight = 14 + 9 + 10 + dialogLines.size() * 10 + 12 + 20 + 12;
		dialogX0 = (width - dialogWidth) / 2;
		dialogX1 = dialogX0 + dialogWidth;
		dialogY0 = (height - dialogHeight) / 2;
		dialogY1 = dialogY0 + dialogHeight;
		int buttonWidth = (inner - 8) / 2;
		backX0 = dialogX0 + 12;
		backX1 = backX0 + buttonWidth;
		acceptX1 = dialogX1 - 12;
		acceptX0 = acceptX1 - buttonWidth;
		dialogButtonY1 = dialogY1 - 12;
		dialogButtonY0 = dialogButtonY1 - 20;
	}

	private Race current() {
		if (races.isEmpty()) {
			return RaceRegistry.defaultRace();
		}
		return forms.isEmpty() ? races.get(MathHelper.clamp(selected, 0, races.size() - 1)) : forms.get(MathHelper.clamp(formIndex, 0, forms.size() - 1));
	}

	private void loadForms() {
		forms = races.isEmpty() ? List.of() : RaceRegistry.forms(races.get(MathHelper.clamp(selected, 0, races.size() - 1)));
		formHover = new float[forms.size()];
		formIndex = chosen != null && forms.contains(chosen) ? forms.indexOf(chosen) : 0;
	}

	private void layoutDetails() {
		int column = rightX1 - rightX0;
		int top = panelTop + PANEL_PAD;
		if (forms.size() > 1) {
			formY0 = top;
			formY1 = top + FORM_BAR - 4;
			top += FORM_BAR + 2;
		}
		details.setBounds(rightX0 + PANEL_PAD + 2, top, column - PANEL_PAD * 2 - 4, panelBottom - PANEL_PAD - top);
		rebuildDetails();
	}

	private void selectForm(int index) {
		if (index < 0 || index >= forms.size() || index == formIndex) {
			return;
		}
		formIndex = index;
		changedAt = Util.getMeasuringTimeMs();
		details.reset();
		rebuildDetails();
		if (client != null) {
			client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.2F, 0.6F));
		}
	}

	private int formAt(double mouseX, double mouseY) {
		if (forms.size() < 2 || mouseY < formY0 || mouseY >= formY1) {
			return -1;
		}
		int x0 = rightX0 + PANEL_PAD;
		int span = rightX1 - PANEL_PAD - x0;
		int index = (int) Math.floor((mouseX - x0) / (span / (double) forms.size()));
		return mouseX >= x0 && index >= 0 && index < forms.size() ? index : -1;
	}

	private void renderForms(DrawContext context, int mouseX, int mouseY, float alpha) {
		if (forms.size() < 2) {
			return;
		}
		int hovered = confirming ? -1 : formAt(mouseX, mouseY);
		int x0 = rightX0 + PANEL_PAD;
		int span = rightX1 - PANEL_PAD - x0;
		int gap = 4;
		for (int i = 0; i < forms.size(); i++) {
			formHover[i] = MathHelper.lerp(0.3F, formHover[i], i == hovered ? 1.0F : 0.0F);
			int cx0 = x0 + span * i / forms.size() + (i > 0 ? gap / 2 : 0);
			int cx1 = x0 + span * (i + 1) / forms.size() - (i < forms.size() - 1 ? gap / 2 : 0);
			Race form = forms.get(i);
			boolean active = i == formIndex;
			int tint = 0xFF000000 | form.color();
			int fill = active ? Draw.mix(0xE00D0A0C, (tint & 0x00FFFFFF) | 0xE0000000, 0.28F) : Draw.mix(0xC00D0A0C, 0xC0222A26, formHover[i]);
			context.fill(cx0, formY0, cx1, formY1, Draw.fade(fill, alpha));
			Draw.frame(context, cx0, formY0, cx1, formY1, Draw.fade(active ? Draw.GOLD : Draw.mix(0xFF3B4A42, tint, formHover[i]), alpha));
			Text label = text(form.variantName() == null || form.variantName().isEmpty() ? form.name() : form.variantName());
			OrderedText trimmed = Language.getInstance().reorder(textRenderer.trimToWidth(label, cx1 - cx0 - 6));
			int color = active ? Draw.TEXT_BRIGHT : Draw.mix(Draw.TEXT_MUTED, Draw.TEXT_BRIGHT, formHover[i]);
			Draw.text(context, textRenderer, trimmed, (cx0 + cx1) / 2.0F - textRenderer.getWidth(trimmed) / 2.0F,
					(formY0 + formY1) / 2.0F - 4, Draw.fade(color, alpha), 1.0F, true);
		}
	}

	private void select(int index) {
		if (index < 0 || index >= races.size() || index == selected) {
			return;
		}
		selected = index;
		changedAt = Util.getMeasuringTimeMs();
		details.reset();
		loadForms();
		layoutDetails();
		list.ensureVisible(index * ROW_HEIGHT, (index + 1) * ROW_HEIGHT);
		if (client != null) {
			client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.3F, 0.5F));
		}
	}

	private void rebuildDetails() {
		layout = Details.build(textRenderer, current(), details.width());
		details.setContent(layout.height);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return !fromIntro && !confirming;
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		backdrop.render(context, width, height, (Util.getMeasuringTimeMs() - openedAt) / 1000.0F);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		long now = Util.getMeasuringTimeMs();
		float frame = Math.min(0.1F, (now - lastFrame) / 1000.0F);
		lastFrame = now;
		float time = (now - openedAt) / 1000.0F;
		float alpha = Draw.easeOut((time - (fromIntro ? 0.15F : 0.0F)) / 0.6F);

		renderBackground(context, mouseX, mouseY, delta);
		list.update();
		details.update();

		renderHeader(context, alpha);
		renderModel(context, mouseX, mouseY, frame, alpha);
		renderSpecies(context, mouseX, mouseY, alpha);
		renderDetails(context, mouseX, mouseY, now, alpha);
		if (viewOnly) {
			renderCurrent(context, alpha);
		} else {
			renderButton(context, confirming ? -1 : mouseX, confirming ? -1 : mouseY, alpha);
		}
		if (confirming) {
			renderDialog(context, mouseX, mouseY, now);
		}

		if (fromIntro) {
			float flash = 1.0F - Draw.easeOut(time / 0.7F);
			if (flash > 0.0F) {
				context.getMatrices().push();
				context.getMatrices().translate(0.0F, 0.0F, 900.0F);
				Draw.rect(context, 0, 0, width, height, Draw.fade(0xFFF7E3EC, flash));
				context.getMatrices().pop();
			}
		}
	}

	private void renderHeader(DrawContext context, float alpha) {
		int cx = width / 2;
		Text sub = Draw.spaced("ORIGINS");
		Draw.centered(context, textRenderer, sub, cx, headerY, Draw.fade(Draw.GOLD, alpha), 1.0F, true);
		int subHalf = textRenderer.getWidth(sub) / 2;
		Draw.diamond(context, cx - subHalf - 8, headerY + 4, Draw.fade(Draw.GOLD_DIM, alpha));
		Draw.diamond(context, cx + subHalf + 8, headerY + 4, Draw.fade(Draw.GOLD_DIM, alpha));

		int titleY = headerY + 13;
		Draw.centered(context, textRenderer, getTitle(), cx, titleY, Draw.fade(Draw.TEXT_BRIGHT, alpha), titleScale, true);
		int half = Math.min(width / 2 - 10, textRenderer.getWidth(getTitle()) * titleScale / 2 + 30);
		Draw.divider(context, cx - half, cx + half, titleY + 9 * titleScale + 4, Draw.fade(Draw.GOLD, alpha * 0.8F));
	}

	private void renderSpecies(DrawContext context, int mouseX, int mouseY, float alpha) {
		Draw.ornamentFrame(context, leftX0, panelTop, leftX1, panelBottom, alpha);
		Text heading = Text.translatable("screen.datura_origins.select.species");
		int headingWidth = leftX1 - leftX0 - PANEL_PAD * 2;
		OrderedText trimmed = Language.getInstance().reorder(textRenderer.trimToWidth(heading, headingWidth));
		Draw.text(context, textRenderer, trimmed, leftX0 + PANEL_PAD, panelTop + PANEL_PAD, Draw.fade(Draw.GOLD, alpha), 1.0F, true);
		Draw.divider(context, leftX0 + PANEL_PAD - 2, leftX1 - PANEL_PAD + 2, panelTop + PANEL_PAD + 11, Draw.fade(Draw.GOLD_DIM, alpha));

		int hovered = confirming ? -1 : rowAt(mouseX, mouseY);
		int numeralWidth = textRenderer.getWidth("VIII.");
		list.begin(context);
		for (int i = 0; i < races.size(); i++) {
			rowHover[i] = MathHelper.lerp(0.3F, rowHover[i], i == hovered ? 1.0F : 0.0F);
			int y0 = list.y() + Math.round(i * ROW_HEIGHT - list.offset());
			int y1 = y0 + ROW_HEIGHT - 2;
			if (y1 < list.y() || y0 > list.y() + list.height()) {
				continue;
			}
			int x0 = list.x();
			int x1 = list.x() + list.width();
			boolean active = i == selected;
			if (active) {
				Draw.hGradient(context, x0, y0, x1, y1, Draw.fade(0x7A1F5A40, alpha), Draw.fade(0x302F1B2A, alpha));
				context.fill(x0, y0, x0 + 2, y1, Draw.fade(Draw.GOLD, alpha));
				Draw.frame(context, x0, y0, x1, y1, Draw.fade(0x50C9A75A, alpha));
			} else if (rowHover[i] > 0.02F) {
				Draw.hGradient(context, x0, y0, x1, y1, Draw.fade(0x26FFFFFF, alpha * rowHover[i]), Draw.fade(0x08FFFFFF, alpha * rowHover[i]));
			}
			int textY = y0 + (ROW_HEIGHT - 2 - 8) / 2;
			float shift = rowHover[i] * 2.0F + (active ? 2.0F : 0.0F);
			String numeral = RomanNumerals.of(i + 1) + ".";
			Draw.text(context, textRenderer, Text.literal(numeral), x0 + 6 + numeralWidth - textRenderer.getWidth(numeral) + shift, textY,
					Draw.fade(active ? Draw.GOLD : Draw.GOLD_DIM, alpha), 1.0F, true);
			int nameX = x0 + 6 + numeralWidth + 6;
			Text name = text(races.get(i).name());
			OrderedText nameText = Language.getInstance().reorder(textRenderer.trimToWidth(name, x1 - nameX - (races.get(i).equals(chosenBase) ? 14 : 4)));
			int color = active ? Draw.TEXT_BRIGHT : Draw.mix(Draw.TEXT, Draw.TEXT_BRIGHT, rowHover[i]);
			if (races.get(i).equals(chosenBase)) {
				Draw.diamond(context, x1 - 7, y0 + (ROW_HEIGHT - 2) / 2, Draw.fade(Draw.GOLD, alpha));
			}
			Draw.text(context, textRenderer, nameText, nameX + shift, textY, Draw.fade(color, alpha), 1.0F, true);
		}
		list.end(context, alpha);
	}

	private void renderDetails(DrawContext context, int mouseX, int mouseY, long now, float alpha) {
		Draw.ornamentFrame(context, rightX0, panelTop, rightX1, panelBottom, alpha);
		renderForms(context, mouseX, mouseY, alpha);
		float appear = Draw.easeOut((now - changedAt) / (float) DETAIL_FADE_MS);
		float a = alpha * appear;
		float slide = (1.0F - appear) * 12.0F;
		details.begin(context);
		layout.render(context, textRenderer, details.x() + slide, details.y() - details.offset(), a);
		details.end(context, alpha);
	}

	private void renderModel(DrawContext context, int mouseX, int mouseY, float frame, float alpha) {
		if (client == null || client.player == null || centerX1 - centerX0 < 20) {
			return;
		}
		long now = Util.getMeasuringTimeMs();
		pitch = dragging ? pitch : MathHelper.lerp(0.08F, pitch, 0.0F);

		float areaHeight = panelBottom - panelTop;
		float areaWidth = centerX1 - centerX0;
		float size = Math.min(areaHeight * 0.74F / 1.9F, areaWidth * 0.8F);
		float pop = 0.94F + 0.06F * Draw.easeOut((now - changedAt) / (float) DETAIL_FADE_MS);
		float cx = (centerX0 + centerX1) / 2.0F;
		float cy = panelTop + areaHeight * 0.47F;
		float feet = cy + size * 0.92F;

		Draw.glow(context, cx, cy, size * 0.9F, size * 1.3F, Draw.fade(0x3A2F8F64, alpha));
		Draw.glow(context, cx, feet, size * 0.75F, size * 0.16F, Draw.fade(0x70C25A8C, alpha));
		Draw.glow(context, cx, feet, size * 0.42F, size * 0.08F, Draw.fade(0x90E6D08F, alpha));
		Draw.divider(context, Math.round(cx - size * 0.6F), Math.round(cx + size * 0.6F), Math.round(feet + size * 0.05F), Draw.fade(Draw.GOLD, alpha * 0.7F));

		context.enableScissor(centerX0, panelTop, centerX1, panelBottom);
		RaceModels.beginPreview(current());
		try {
			RacePreviews.get(current()).render(context, client.player, cx, cy, size * pop, yaw, pitch, frame);
		} finally {
			RaceModels.endPreview();
		}
		context.disableScissor();

		float hint = dragged ? 0.0F : alpha * 0.7F;
		if (hint > 0.02F) {
			Text drag = Text.translatable("screen.datura_origins.select.drag");
			if (textRenderer.getWidth(drag) <= areaWidth) {
				Draw.centered(context, textRenderer, drag, cx, Math.min(panelBottom - 10, feet + size * 0.1F + 4), Draw.fade(Draw.TEXT_MUTED, hint), 1.0F, false);
			}
		}
	}

	private void renderButton(DrawContext context, int mouseX, int mouseY, float alpha) {
		buttonHover = MathHelper.lerp(0.3F, buttonHover, isOverButton(mouseX, mouseY) ? 1.0F : 0.0F);
		drawButton(context, buttonX0, buttonY0, buttonX1, buttonY1, Text.translatable("screen.datura_origins.select.confirm"), buttonHover, alpha, true);
	}

	private void drawButton(DrawContext context, int x0, int y0, int x1, int y1, Text label, float hover, float alpha, boolean primary) {
		int background = Draw.mix(0xF20D0A0C, primary ? 0xF2221319 : 0xF2121A16, hover);
		int border = primary ? Draw.mix(Draw.BUTTON_BORDER, Draw.PINK, hover * 0.8F) : Draw.mix(0xFF3B4A42, 0xFF7E9488, hover);
		if (primary) {
			Draw.glow(context, (x0 + x1) / 2.0F, (y0 + y1) / 2.0F, (x1 - x0) * 0.75F, 18.0F,
					Draw.fade(0x40C25A8C, alpha * (0.4F + 0.6F * hover)));
		}
		context.fill(x0, y0, x1, y1, Draw.fade(background, alpha));
		Draw.frame(context, x0, y0, x1, y1, Draw.fade(border, alpha));
		context.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, Draw.fade(0x18FFFFFF, alpha));

		int labelWidth = textRenderer.getWidth(label);
		int midY = (y0 + y1) / 2;
		int textColor = Draw.fade(Draw.mix(Draw.TEXT, Draw.TEXT_BRIGHT, hover), alpha);
		if (primary) {
			int x = (x0 + x1 - (10 + labelWidth)) / 2;
			context.fill(x, midY - 2, x + 4, midY + 2, Draw.fade(Draw.GOLD, alpha));
			context.fill(x + 1, midY - 1, x + 3, midY + 1, Draw.fade(0xFFF2DFA6, alpha * hover));
			Draw.text(context, textRenderer, label, x + 10, midY - 4, textColor, 1.0F, true);
		} else {
			Draw.centered(context, textRenderer, label, (x0 + x1) / 2.0F, midY - 4, textColor, 1.0F, true);
		}
	}

	private void renderCurrent(DrawContext context, float alpha) {
		context.fill(buttonX0, buttonY0, buttonX1, buttonY1, Draw.fade(0xE00D0A0C, alpha));
		Draw.frame(context, buttonX0, buttonY0, buttonX1, buttonY1, Draw.fade(Draw.GOLD_DIM, alpha));
		Text label = currentLabel();
		int midY = (buttonY0 + buttonY1) / 2;
		int x = (buttonX0 + buttonX1 - (10 + textRenderer.getWidth(label))) / 2;
		context.fill(x, midY - 2, x + 4, midY + 2, Draw.fade(Draw.GOLD, alpha));
		Draw.text(context, textRenderer, label, x + 10, midY - 4, Draw.fade(Draw.TEXT_BRIGHT, alpha), 1.0F, true);
		Text hint = Text.translatable("screen.datura_origins.select.close_hint");
		if (buttonY1 + 3 + 8 <= height) {
			Draw.centered(context, textRenderer, hint, width / 2.0F, buttonY1 + 3, Draw.fade(Draw.TEXT_MUTED, alpha * 0.8F), 1.0F, false);
		}
	}

	private void renderDialog(DrawContext context, int mouseX, int mouseY, long now) {
		float a = Draw.easeOut((now - confirmAt) / 220.0F);
		context.getMatrices().push();
		context.getMatrices().translate(0.0F, 0.0F, 500.0F);
		Draw.rect(context, 0, 0, width, height, Draw.fade(0xC0040907, a));
		int lift = Math.round((1.0F - a) * 6.0F);
		context.getMatrices().translate(0.0F, lift, 0.0F);
		Draw.ornamentFrame(context, dialogX0, dialogY0, dialogX1, dialogY1, a);
		context.fill(dialogX0 + 1, dialogY0 + 1, dialogX1 - 1, dialogY1 - 1, Draw.fade(0xF0091310, a));
		int cx = (dialogX0 + dialogX1) / 2;
		int y = dialogY0 + 14;
		Draw.centered(context, textRenderer, Draw.spaced(Text.translatable("screen.datura_origins.confirm.title").getString()), cx, y, Draw.fade(Draw.GOLD, a), 1.0F, true);
		y += 9 + 4;
		Draw.divider(context, dialogX0 + 16, dialogX1 - 16, y, Draw.fade(Draw.GOLD_DIM, a));
		y += 7;
		for (OrderedText line : dialogLines) {
			Draw.text(context, textRenderer, line, cx - textRenderer.getWidth(line) / 2.0F, y, Draw.fade(Draw.TEXT, a), 1.0F, false);
			y += 10;
		}
		backHover = MathHelper.lerp(0.3F, backHover, isOver(mouseX, mouseY, backX0, dialogButtonY0, backX1, dialogButtonY1) ? 1.0F : 0.0F);
		acceptHover = MathHelper.lerp(0.3F, acceptHover, isOver(mouseX, mouseY, acceptX0, dialogButtonY0, acceptX1, dialogButtonY1) ? 1.0F : 0.0F);
		drawButton(context, backX0, dialogButtonY0, backX1, dialogButtonY1, Text.translatable("screen.datura_origins.confirm.back"), backHover, a, false);
		drawButton(context, acceptX0, dialogButtonY0, acceptX1, dialogButtonY1, Text.translatable("screen.datura_origins.confirm.accept"), acceptHover, a, true);
		context.getMatrices().pop();
	}

	private static boolean isOver(double mouseX, double mouseY, int x0, int y0, int x1, int y1) {
		return mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1;
	}

	private void openConfirm() {
		if (viewOnly || confirmed) {
			return;
		}
		layoutDialog();
		confirming = true;
		confirmAt = Util.getMeasuringTimeMs();
		backHover = 0.0F;
		acceptHover = 0.0F;
		if (client != null) {
			client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		}
	}

	private int rowAt(double mouseX, double mouseY) {
		if (!list.contains(mouseX, mouseY)) {
			return -1;
		}
		int index = (int) Math.floor((mouseY - list.y() + list.offset()) / ROW_HEIGHT);
		return index >= 0 && index < races.size() ? index : -1;
	}

	private boolean isOverButton(double mouseX, double mouseY) {
		return mouseX >= buttonX0 && mouseX < buttonX1 && mouseY >= buttonY0 && mouseY < buttonY1;
	}

	private boolean isOverModel(double mouseX, double mouseY) {
		return mouseX >= centerX0 && mouseX < centerX1 && mouseY >= panelTop && mouseY < panelBottom;
	}

	private void confirm() {
		if (viewOnly || confirmed || client == null) {
			return;
		}
		confirmed = true;
		client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
		if (ClientPlayNetworking.canSend(ChooseOriginPayload.ID)) {
			ClientPlayNetworking.send(new ChooseOriginPayload(current().id()));
		}
		client.setScreen(null);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (confirming) {
			if (button == 0 && isOver(mouseX, mouseY, acceptX0, dialogButtonY0, acceptX1, dialogButtonY1)) {
				confirm();
			} else if (button == 0 && isOver(mouseX, mouseY, backX0, dialogButtonY0, backX1, dialogButtonY1)) {
				confirming = false;
			}
			return true;
		}
		if (button == 0) {
			int row = rowAt(mouseX, mouseY);
			if (row >= 0) {
				select(row);
				return true;
			}
			int form = formAt(mouseX, mouseY);
			if (form >= 0) {
				selectForm(form);
				return true;
			}
			if (!viewOnly && isOverButton(mouseX, mouseY)) {
				openConfirm();
				return true;
			}
			if (isOverModel(mouseX, mouseY)) {
				dragging = true;
				lastInteraction = Util.getMeasuringTimeMs();
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (dragging && button == 0) {
			yaw += (float) deltaX * 1.2F;
			pitch = MathHelper.clamp(pitch + (float) deltaY * 0.5F, -20.0F, 20.0F);
			dragged = true;
			lastInteraction = Util.getMeasuringTimeMs();
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (dragging && button == 0) {
			dragging = false;
			lastInteraction = Util.getMeasuringTimeMs();
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (confirming) {
			return true;
		}
		if (list.scroll(mouseX, mouseY, verticalAmount) || details.scroll(mouseX, mouseY, verticalAmount)) {
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (confirming) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
				confirming = false;
			} else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
				confirm();
			}
			return true;
		}
		switch (keyCode) {
			case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_W -> select(selected - 1);
			case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_S -> select(selected + 1);
			case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> openConfirm();
			default -> {
				return super.keyPressed(keyCode, scanCode, modifiers);
			}
		}
		return true;
	}

	static Text text(String value) {
		return Text.translatableWithFallback(value, value);
	}

	private static final class Details {
		static final Details EMPTY = new Details(List.of(), List.of(), 0);

		private final List<Line> lines;
		private final List<Block> blocks;
		private final int height;

		private Details(List<Line> lines, List<Block> blocks, int height) {
			this.lines = lines;
			this.blocks = blocks;
			this.height = height;
		}

		static Details build(TextRenderer font, Race race, int width) {
			List<Line> lines = new ArrayList<>();
			List<Block> blocks = new ArrayList<>();
			if (width <= 20) {
				return EMPTY;
			}
			int y = 0;

			Text name = text(race.name());
			int scale = font.getWidth(name) * 2 <= width ? 2 : 1;
			for (OrderedText part : font.wrapLines(name, width / scale)) {
				lines.add(new Line(part, 0, y, Draw.TEXT_BRIGHT, scale, true));
				y += 9 * scale + 1;
			}
			y += 3;
			blocks.add(new Block(Block.DIVIDER, 0, y, Math.min(width, Math.max(60, width * 3 / 4)), 1, 0xFF000000 | race.color()));
			y += 7;

			if (!race.description().isEmpty()) {
				for (OrderedText part : font.wrapLines(text(race.description()), width)) {
					lines.add(new Line(part, 0, y, Draw.TEXT, 1, false));
					y += 10;
				}
				y += 6;
			}

			y = section(font, lines, blocks, y, width, "screen.datura_origins.select.advantages", "+", Draw.GREEN, race.advantages());
			y = section(font, lines, blocks, y, width, "screen.datura_origins.select.disadvantages", "-", Draw.RED, race.disadvantages());
			if (race.abilityName() != null && !race.abilityName().isEmpty()) {
				y = abilitySection(font, lines, blocks, y, width, race);
			}
			y = section(font, lines, blocks, y, width, "screen.datura_origins.select.passives", "*", Draw.PINK, race.passives());
			return new Details(lines, blocks, Math.max(0, y - 4));
		}

		private static int abilitySection(TextRenderer font, List<Line> lines, List<Block> blocks, int y, int width, Race race) {
			int start = y;
			y += 5;
			lines.add(new Line(Language.getInstance().reorder(font.trimToWidth(Text.translatable("screen.datura_origins.select.ability"), width - 10)), 7, y, Draw.GOLD, 1, true));
			y += 13;
			for (OrderedText part : font.wrapLines(text(race.abilityName()), width - 11)) {
				lines.add(new Line(part, 7, y, Draw.TEXT_BRIGHT, 1, true));
				y += 10;
			}
			y += 2;
			if (race.ability() != null && !race.ability().isEmpty()) {
				for (OrderedText part : font.wrapLines(text(race.ability()), width - 11)) {
					lines.add(new Line(part, 7, y, Draw.TEXT, 1, false));
					y += 10;
				}
			}
			y += 5;
			blocks.add(new Block(Block.SECTION, 0, start, width, y - start, Draw.GOLD));
			return y + 8;
		}

		private static int section(TextRenderer font, List<Line> lines, List<Block> blocks, int y, int width,
				String key, String marker, int color, List<String> entries) {
			if (entries.isEmpty()) {
				return y;
			}
			int start = y;
			y += 5;
			lines.add(new Line(Language.getInstance().reorder(font.trimToWidth(Text.translatable(key), width - 10)), 7, y, color, 1, true));
			y += 13;
			int indent = 16;
			for (String entry : entries) {
				lines.add(new Line(Text.literal(marker).asOrderedText(), 7, y, color, 1, true));
				for (OrderedText part : font.wrapLines(text(entry), width - indent - 4)) {
					lines.add(new Line(part, indent, y, Draw.TEXT, 1, false));
					y += 10;
				}
				y += 3;
			}
			y += 2;
			blocks.add(new Block(Block.SECTION, 0, start, width, y - start, color));
			return y + 8;
		}

		void render(DrawContext context, TextRenderer font, float x, float y, float alpha) {
			for (Block block : blocks) {
				int x0 = Math.round(x + block.x);
				int y0 = Math.round(y + block.y);
				if (block.type == Block.DIVIDER) {
					Draw.hGradient(context, x0, y0, x0 + block.width, y0 + 1, Draw.fade(block.color, alpha), block.color & 0x00FFFFFF);
				} else {
					Draw.hGradient(context, x0, y0, x0 + block.width, y0 + block.height,
							Draw.fade((block.color & 0x00FFFFFF) | 0x22000000, alpha), Draw.fade((block.color & 0x00FFFFFF) | 0x08000000, alpha));
					context.fill(x0, y0, x0 + 1, y0 + block.height, Draw.fade((block.color & 0x00FFFFFF) | 0xA0000000, alpha));
				}
			}
			for (Line line : lines) {
				Draw.text(context, font, line.text, x + line.x, y + line.y, Draw.fade(line.color, alpha), line.scale, line.shadow);
			}
		}

		private record Line(OrderedText text, int x, int y, int color, float scale, boolean shadow) {
		}

		private record Block(int type, int x, int y, int width, int height, int color) {
			static final int DIVIDER = 0;
			static final int SECTION = 1;
		}
	}
}
