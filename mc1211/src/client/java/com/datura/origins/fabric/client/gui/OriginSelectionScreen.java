package com.datura.origins.fabric.client.gui;

import com.datura.origins.DaturaOrigins;
import com.datura.origins.fabric.client.ClientOrigin;
import com.datura.origins.fabric.client.model.RaceModels;
import com.datura.origins.fabric.client.preview.PreviewFrame;
import com.datura.origins.fabric.client.preview.RacePreviews;
import com.datura.origins.fabric.network.ChooseOriginPayload;
import com.datura.origins.race.Race;
import com.datura.origins.race.RaceRegistry;
import com.datura.origins.race.RomanNumerals;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;

public class OriginSelectionScreen extends Screen {
	private static final Identifier SHADOW = Identifier.of(DaturaOrigins.MOD_ID, "textures/gui/shadow.png");
	private static final int ROW_HEIGHT = 15;
	private static final long DETAIL_FADE_MS = 320L;

	static final int ROSE = 0xFFE3B3C6;
	static final int ROSE_DIM = 0xFF9C7787;
	static final int INK = 0xFFDCD6D3;
	static final int INK_SOFT = 0xFFA7ADA8;
	static final int INK_FAINT = 0xFF7B847E;
	static final int INK_BRIGHT = 0xFFF6EFEC;

	private final Backdrop backdrop = new Backdrop(Backdrop.SELECTION);
	private final boolean fromIntro;
	private final long openedAt = Util.getMeasuringTimeMs();
	private final ScrollArea list = new ScrollArea();
	private final ScrollArea details = new ScrollArea();

	private List<Race> races = List.of();
	private float[] rowHover = new float[0];
	private int selected;
	private long changedAt = openedAt;
	private Details layout = Details.EMPTY;

	private int areaTop, areaBottom;
	private int listX0, listX1;
	private int detailX0, detailX1;
	private int modelX0, modelX1;
	private int buttonX0, buttonX1, buttonY0, buttonY1;
	private float buttonHover;

	private float yaw = 22.0F;
	private float pitch;
	private boolean dragging;
	private boolean dragged;
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
		if (races.isEmpty()) {
			List<Race> all = new ArrayList<>();
			for (Race page : RaceRegistry.pages()) {
				all.addAll(RaceRegistry.forms(page));
			}
			races = all;
			rowHover = new float[races.size()];
			chosen = fromIntro ? null : ClientOrigin.get().orElse(null);
			viewOnly = chosen != null;
			selected = Math.max(0, races.indexOf(viewOnly ? chosen : RaceRegistry.defaultRace()));
			PreviewFrame.clear();
		}

		int margin = MathHelper.clamp(Math.round(Math.min(width, height) * 0.08F), 14, 52);
		int gap = MathHelper.clamp(Math.round(width * 0.045F), 14, 48);
		areaTop = Math.max(margin, Math.round(height * 0.13F));

		Text label = viewOnly ? currentLabel() : Text.translatable("screen.datura_origins.select.confirm");
		int buttonWidth = textRenderer.getWidth(label) + 28;
		buttonY1 = height - Math.max(margin - 4, Math.round(height * 0.07F));
		buttonY0 = buttonY1 - 16;
		buttonX0 = (width - buttonWidth) / 2;
		buttonX1 = buttonX0 + buttonWidth;
		areaBottom = Math.max(areaTop + 60, buttonY0 - 18);

		int listWidth = MathHelper.clamp(Math.round(width * 0.19F), 92, 150);
		int detailWidth = MathHelper.clamp(Math.round(width * 0.27F), 120, 214);
		int minModel = 80;
		int spare = width - margin * 2 - gap * 2 - listWidth - detailWidth;
		if (spare < minModel) {
			int cut = minModel - spare;
			listWidth = Math.max(80, listWidth - cut / 2);
			detailWidth = Math.max(100, detailWidth - cut / 2);
		}
		listX0 = margin;
		listX1 = margin + listWidth;
		detailX1 = width - margin;
		detailX0 = detailX1 - detailWidth;
		modelX0 = listX1 + gap;
		modelX1 = detailX0 - gap;

		int listHeight = Math.min(races.size() * ROW_HEIGHT, areaBottom - areaTop);
		int listTop = areaTop + (areaBottom - areaTop - listHeight) / 2;
		list.setBounds(listX0, listTop, listWidth, listHeight);
		list.setContent(races.size() * ROW_HEIGHT);
		list.ensureVisible(selected * ROW_HEIGHT, (selected + 1) * ROW_HEIGHT);
		details.setBounds(detailX0, areaTop, detailWidth, areaBottom - areaTop);
		rebuildDetails();
		layoutDialog();
	}

	private Text currentLabel() {
		return Text.translatable("screen.datura_origins.select.current", chosen == null ? Text.empty() : text(chosen.name()));
	}

	private void layoutDialog() {
		int dialogWidth = Math.min(240, width - 40);
		int inner = dialogWidth - 32;
		dialogLines = textRenderer.wrapLines(Text.translatable("screen.datura_origins.confirm.body", text(current().name())), inner);
		int dialogHeight = 18 + 9 + 12 + dialogLines.size() * 11 + 16 + 14 + 16;
		dialogX0 = (width - dialogWidth) / 2;
		dialogX1 = dialogX0 + dialogWidth;
		dialogY0 = (height - dialogHeight) / 2;
		dialogY1 = dialogY0 + dialogHeight;
		int buttonWidth = (inner - 12) / 2;
		backX0 = dialogX0 + 16;
		backX1 = backX0 + buttonWidth;
		acceptX1 = dialogX1 - 16;
		acceptX0 = acceptX1 - buttonWidth;
		dialogButtonY1 = dialogY1 - 16;
		dialogButtonY0 = dialogButtonY1 - 14;
	}

	private Race current() {
		if (races.isEmpty()) {
			return RaceRegistry.defaultRace();
		}
		return races.get(MathHelper.clamp(selected, 0, races.size() - 1));
	}

	private void select(int index) {
		if (index < 0 || index >= races.size() || index == selected) {
			return;
		}
		selected = index;
		changedAt = Util.getMeasuringTimeMs();
		details.reset();
		rebuildDetails();
		list.ensureVisible(index * ROW_HEIGHT, (index + 1) * ROW_HEIGHT);
		if (client != null) {
			client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.3F, 0.45F));
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
		backdrop.render(context, width, height, (Util.getMeasuringTimeMs() - openedAt) / 1000.0F, mouseX, mouseY);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		long now = Util.getMeasuringTimeMs();
		float frame = Math.min(0.1F, (now - lastFrame) / 1000.0F);
		lastFrame = now;
		float time = (now - openedAt) / 1000.0F;
		float alpha = Draw.easeOut((time - (fromIntro ? 0.1F : 0.0F)) / 0.7F);

		renderBackground(context, mouseX, mouseY, delta);
		list.update();
		details.update();

		renderModel(context, frame, alpha);
		renderList(context, mouseX, mouseY, alpha);
		renderDetails(context, now, alpha);
		if (viewOnly) {
			renderCurrent(context, alpha);
		} else {
			renderButton(context, confirming ? -1 : mouseX, confirming ? -1 : mouseY, alpha);
		}
		if (confirming) {
			renderDialog(context, mouseX, mouseY, now);
		}

		if (fromIntro) {
			float flash = 0.55F * (1.0F - Draw.easeOut(time / 0.65F));
			if (flash > 0.0F) {
				context.getMatrices().push();
				context.getMatrices().translate(0.0F, 0.0F, 900.0F);
				Draw.rect(context, 0, 0, width, height, Draw.fade(0xFFF3E6EC, flash));
				context.getMatrices().pop();
			}
		}
	}

	private void renderList(DrawContext context, int mouseX, int mouseY, float alpha) {
		int hovered = confirming ? -1 : rowAt(mouseX, mouseY);
		int numeralWidth = textRenderer.getWidth("XXIV.");
		list.begin(context);
		for (int i = 0; i < races.size(); i++) {
			rowHover[i] = MathHelper.lerp(0.25F, rowHover[i], i == hovered ? 1.0F : 0.0F);
			int y0 = list.y() + Math.round(i * ROW_HEIGHT - list.offset());
			if (y0 + ROW_HEIGHT < list.y() || y0 > list.y() + list.height()) {
				continue;
			}
			boolean active = i == selected;
			float lift = active ? 1.0F : rowHover[i];
			int textY = y0 + (ROW_HEIGHT - 8) / 2;
			String numeral = RomanNumerals.of(i + 1) + ".";
			int numeralX = list.x() + numeralWidth - textRenderer.getWidth(numeral);
			Draw.text(context, textRenderer, Text.literal(numeral), numeralX, textY,
					Draw.fade(Draw.mix(ROSE_DIM, ROSE, lift), alpha * (0.7F + 0.3F * lift)), 1.0F, false);
			int nameX = list.x() + numeralWidth + 7 + Math.round(lift * 2.0F);
			int limit = list.x() + list.width() - nameX - (races.get(i).equals(chosen) ? 10 : 0);
			OrderedText name = Language.getInstance().reorder(textRenderer.trimToWidth(text(races.get(i).name()), Math.max(10, limit)));
			int color = active ? INK_BRIGHT : Draw.mix(INK_FAINT, INK, rowHover[i]);
			Draw.text(context, textRenderer, name, nameX, textY, Draw.fade(color, alpha), 1.0F, active);
			if (races.get(i).equals(chosen)) {
				Draw.diamond(context, nameX + textRenderer.getWidth(name) + 6, textY + 4, Draw.fade(ROSE, alpha));
			}
		}
		list.end(context, alpha);
	}

	private void renderDetails(DrawContext context, long now, float alpha) {
		float appear = Draw.easeOut((now - changedAt) / (float) DETAIL_FADE_MS);
		float a = alpha * appear;
		float slide = (1.0F - appear) * 6.0F;
		details.begin(context);
		layout.render(context, textRenderer, details.x(), details.y() - details.offset() + slide, a);
		details.end(context, alpha);
	}

	private void renderModel(DrawContext context, float frame, float alpha) {
		if (client == null || client.player == null || modelX1 - modelX0 < 20) {
			return;
		}
		pitch = dragging ? pitch : MathHelper.lerp(0.08F, pitch, 0.0F);
		Race race = current();
		PreviewFrame bounds = PreviewFrame.of(race);

		float areaHeight = areaBottom - areaTop;
		float areaWidth = modelX1 - modelX0;
		float ground = areaBottom - areaHeight * 0.06F;
		float above = bounds.top() * 0.9375F / 16.0F * bounds.scale();
		float below = Math.max(0.0F, -bounds.bottom() * 0.9375F / 16.0F * bounds.scale());
		float size = areaHeight / 3.1F;
		size = Math.min(size, (ground - areaTop) / Math.max(0.5F, above));
		if (below > 0.0F) {
			size = Math.min(size, (areaBottom - ground + areaHeight * 0.02F) / below);
		}
		size = Math.min(size, areaWidth * 0.94F / Math.max(0.5F, bounds.width()));
		float settle = Draw.easeOut((Util.getMeasuringTimeMs() - changedAt) / (float) DETAIL_FADE_MS);
		float cx = (modelX0 + modelX1) / 2.0F;
		float cy = ground - bounds.middle() * bounds.scale() * size;

		float shadowWidth = Math.max(size * 0.9F * bounds.scale(), size * bounds.width() * 0.55F);
		drawShadow(context, cx, ground, shadowWidth, alpha * 0.6F);

		RaceModels.beginPreview(race);
		try {
			RacePreviews.get(race).render(context, client.player, cx, cy + (1.0F - settle) * 3.0F, size, yaw, pitch, frame);
		} finally {
			RaceModels.endPreview();
		}

		float hint = dragged ? 0.0F : alpha * 0.6F;
		if (hint > 0.02F) {
			Text drag = Text.translatable("screen.datura_origins.select.drag");
			if (textRenderer.getWidth(drag) <= areaWidth) {
				Draw.centered(context, textRenderer, drag, cx, Math.min(areaBottom + 2, ground + 8), Draw.fade(INK_FAINT, hint), 1.0F, false);
			}
		}
	}

	private void drawShadow(DrawContext context, float cx, float cy, float w, float alpha) {
		float h = w * 0.2F;
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		context.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
		context.getMatrices().push();
		context.getMatrices().translate(cx - w / 2.0F, cy - h / 2.0F, 0.0F);
		context.getMatrices().scale(w / 96.0F, h / 96.0F, 1.0F);
		context.drawTexture(SHADOW, 0, 0, 0.0F, 0.0F, 96, 96, 96, 96);
		context.getMatrices().pop();
		context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableBlend();
	}

	private void renderButton(DrawContext context, int mouseX, int mouseY, float alpha) {
		buttonHover = MathHelper.lerp(0.25F, buttonHover, isOverButton(mouseX, mouseY) ? 1.0F : 0.0F);
		drawButton(context, buttonX0, buttonY0, buttonX1, buttonY1, Text.translatable("screen.datura_origins.select.confirm"), buttonHover, alpha, true);
	}

	private void drawButton(DrawContext context, int x0, int y0, int x1, int y1, Text label, float hover, float alpha, boolean primary) {
		int midY = (y0 + y1) / 2;
		int lineY = y1 - 1;
		int accent = primary ? ROSE : INK_SOFT;
		if (hover > 0.01F) {
			context.fill(x0, y0, x1, y1, Draw.fade(0x14FFFFFF, alpha * hover));
		}
		Draw.divider(context, x0, x1, lineY, Draw.fade(accent, alpha * (0.45F + 0.55F * hover)));
		int color = Draw.mix(primary ? INK : INK_SOFT, INK_BRIGHT, hover);
		Draw.centered(context, textRenderer, label, (x0 + x1) / 2.0F, midY - 5 - hover, Draw.fade(color, alpha), 1.0F, false);
	}

	private void renderCurrent(DrawContext context, float alpha) {
		Text label = currentLabel();
		Draw.centered(context, textRenderer, label, width / 2.0F, buttonY0 + 2, Draw.fade(INK, alpha), 1.0F, false);
		Draw.divider(context, buttonX0, buttonX1, buttonY1 - 1, Draw.fade(ROSE_DIM, alpha * 0.6F));
		Text hint = Text.translatable("screen.datura_origins.select.close_hint");
		if (buttonY1 + 5 + 8 <= height) {
			Draw.centered(context, textRenderer, hint, width / 2.0F, buttonY1 + 5, Draw.fade(INK_FAINT, alpha * 0.8F), 1.0F, false);
		}
	}

	private void renderDialog(DrawContext context, int mouseX, int mouseY, long now) {
		float a = Draw.easeOut((now - confirmAt) / 220.0F);
		context.getMatrices().push();
		context.getMatrices().translate(0.0F, 0.0F, 500.0F);
		Draw.rect(context, 0, 0, width, height, Draw.fade(0xB0030806, a));
		int lift = Math.round((1.0F - a) * 5.0F);
		context.getMatrices().translate(0.0F, lift, 0.0F);
		context.fill(dialogX0, dialogY0, dialogX1, dialogY1, Draw.fade(0xF20B1611, a));
		Draw.frame(context, dialogX0, dialogY0, dialogX1, dialogY1, Draw.fade(0x50E3B3C6, a));
		int cx = (dialogX0 + dialogX1) / 2;
		int y = dialogY0 + 18;
		Draw.centered(context, textRenderer, Draw.spaced(Text.translatable("screen.datura_origins.confirm.title").getString()), cx, y, Draw.fade(ROSE, a), 1.0F, false);
		y += 9 + 12;
		for (OrderedText line : dialogLines) {
			Draw.text(context, textRenderer, line, cx - textRenderer.getWidth(line) / 2.0F, y, Draw.fade(INK, a), 1.0F, false);
			y += 11;
		}
		backHover = MathHelper.lerp(0.25F, backHover, isOver(mouseX, mouseY, backX0, dialogButtonY0, backX1, dialogButtonY1) ? 1.0F : 0.0F);
		acceptHover = MathHelper.lerp(0.25F, acceptHover, isOver(mouseX, mouseY, acceptX0, dialogButtonY0, acceptX1, dialogButtonY1) ? 1.0F : 0.0F);
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
		return mouseX >= buttonX0 - 6 && mouseX < buttonX1 + 6 && mouseY >= buttonY0 - 4 && mouseY < buttonY1 + 4;
	}

	private boolean isOverModel(double mouseX, double mouseY) {
		return mouseX >= modelX0 && mouseX < modelX1 && mouseY >= areaTop && mouseY < areaBottom;
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
			if (!viewOnly && isOverButton(mouseX, mouseY)) {
				openConfirm();
				return true;
			}
			if (isOverModel(mouseX, mouseY)) {
				dragging = true;
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
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (dragging && button == 0) {
			dragging = false;
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
		private final List<Rule> rules;
		private final int height;

		private Details(List<Line> lines, List<Rule> rules, int height) {
			this.lines = lines;
			this.rules = rules;
			this.height = height;
		}

		static Details build(TextRenderer font, Race race, int width) {
			List<Line> lines = new ArrayList<>();
			List<Rule> rules = new ArrayList<>();
			if (width <= 20) {
				return EMPTY;
			}
			int y = 0;

			Text name = text(race.name());
			int scale = font.getWidth(name) * 2 <= width ? 2 : 1;
			for (OrderedText part : font.wrapLines(name, width / scale)) {
				lines.add(new Line(part, 0, y, INK_BRIGHT, scale, true));
				y += 9 * scale + 2;
			}
			y += 5;
			rules.add(new Rule(0, y, Math.min(width, 34)));
			y += 9;

			if (!race.description().isEmpty()) {
				for (OrderedText part : font.wrapLines(text(race.description()), width)) {
					lines.add(new Line(part, 0, y, INK_SOFT, 1, false));
					y += 10;
				}
				y += 14;
			}

			y = section(font, lines, y, width, "screen.datura_origins.select.advantages", "+", race.advantages());
			y = section(font, lines, y, width, "screen.datura_origins.select.disadvantages", "−", race.disadvantages());
			if (race.abilityName() != null && !race.abilityName().isEmpty()) {
				y = label(font, lines, y, width, "screen.datura_origins.select.ability");
				for (OrderedText part : font.wrapLines(text(race.abilityName()), width)) {
					lines.add(new Line(part, 0, y, INK_BRIGHT, 1, false));
					y += 10;
				}
				y += 2;
				if (race.ability() != null && !race.ability().isEmpty()) {
					for (OrderedText part : font.wrapLines(text(race.ability()), width)) {
						lines.add(new Line(part, 0, y, INK_SOFT, 1, false));
						y += 10;
					}
				}
				y += 14;
			}
			y = section(font, lines, y, width, "screen.datura_origins.select.passives", "·", race.passives());
			return new Details(lines, rules, Math.max(0, y - 14));
		}

		private static int label(TextRenderer font, List<Line> lines, int y, int width, String key) {
			Text heading = Draw.spaced(Text.translatable(key).getString());
			lines.add(new Line(Language.getInstance().reorder(font.trimToWidth(heading, width)), 0, y, ROSE_DIM, 1, false));
			return y + 14;
		}

		private static int section(TextRenderer font, List<Line> lines, int y, int width, String key, String marker, List<String> entries) {
			if (entries.isEmpty()) {
				return y;
			}
			y = label(font, lines, y, width, key);
			int indent = 10;
			for (String entry : entries) {
				lines.add(new Line(Text.literal(marker).asOrderedText(), 0, y, ROSE_DIM, 1, false));
				for (OrderedText part : font.wrapLines(text(entry), width - indent)) {
					lines.add(new Line(part, indent, y, INK, 1, false));
					y += 10;
				}
				y += 4;
			}
			return y + 10;
		}

		void render(DrawContext context, TextRenderer font, float x, float y, float alpha) {
			for (Rule rule : rules) {
				int x0 = Math.round(x + rule.x);
				int y0 = Math.round(y + rule.y);
				Draw.hGradient(context, x0, y0, x0 + rule.width, y0 + 1, Draw.fade(ROSE, alpha * 0.8F), ROSE & 0x00FFFFFF);
			}
			for (Line line : lines) {
				Draw.text(context, font, line.text, x + line.x, y + line.y, Draw.fade(line.color, alpha), line.scale, line.shadow);
			}
		}

		private record Line(OrderedText text, int x, int y, int color, float scale, boolean shadow) {
		}

		private record Rule(int x, int y, int width) {
		}
	}
}
