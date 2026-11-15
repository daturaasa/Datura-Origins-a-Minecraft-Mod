package com.datura.origins.fabric.client.gui;

import com.datura.origins.fabric.network.AltarChoicePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.EnchantingPhrases;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.BookModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

public class AltarScreen extends Screen {
	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/enchanting_table.png");
	private static final Identifier BOOK_TEXTURE = Identifier.ofVanilla("textures/entity/enchanting_table_book.png");
	private static final Identifier SLOT = Identifier.ofVanilla("container/enchanting_table/enchantment_slot");
	private static final Identifier SLOT_HIGHLIGHTED = Identifier.ofVanilla("container/enchanting_table/enchantment_slot_highlighted");
	private static final Identifier[] LEVELS = {
			Identifier.ofVanilla("container/enchanting_table/level_1"),
			Identifier.ofVanilla("container/enchanting_table/level_2"),
			Identifier.ofVanilla("container/enchanting_table/level_3")};
	private static final int WIDTH = 176;
	private static final int TOP = 80;
	private static final int BOTTOM = 7;

	private final List<Text> names = new ArrayList<>();
	private final int seed;
	private final Random random = Random.create();
	private BookModel book;
	private int ticks;
	private float pageAngle;
	private float lastPageAngle;
	private float pageTarget;
	private float pageSpeed;
	private float open;
	private float lastOpen;
	private int hover = -1;
	private boolean chosen;

	public AltarScreen(List<String> enchantments, List<Integer> levels, int seed) {
		super(Text.translatable("screen.datura_origins.altar"));
		this.seed = seed;
		MinecraftClient minecraft = MinecraftClient.getInstance();
		if (minecraft.world != null) {
			Registry<Enchantment> registry = minecraft.world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
			for (int i = 0; i < enchantments.size(); i++) {
				Identifier id = Identifier.tryParse(enchantments.get(i));
				RegistryEntry<Enchantment> entry = id == null ? null : registry.getEntry(id).orElse(null);
				names.add(entry == null ? Text.literal("?") : Enchantment.getName(entry, levels.get(i)));
			}
		}
	}

	@Override
	protected void init() {
		book = new BookModel(client.getEntityModelLoader().getModelPart(EntityModelLayers.BOOK));
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private int left() {
		return (width - WIDTH) / 2;
	}

	private int top() {
		return (height - TOP - BOTTOM) / 2;
	}

	@Override
	public void tick() {
		ticks++;
		lastOpen = open;
		lastPageAngle = pageAngle;
		open = MathHelper.clamp(open + 0.2F, 0.0F, 1.0F);
		if (ticks % 30 == 0) pageTarget += random.nextInt(4) - random.nextInt(4);
		float diff = MathHelper.clamp((pageTarget - pageAngle) * 0.4F, -0.2F, 0.2F);
		pageSpeed += (diff - pageSpeed) * 0.9F;
		pageAngle += pageSpeed;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		int x = left(), y = top();
		context.drawTexture(TEXTURE, x, y, 0, 0, WIDTH, TOP);
		context.drawTexture(TEXTURE, x, y + TOP, 0, 166 - BOTTOM, WIDTH, BOTTOM);
		context.drawText(textRenderer, title, x + 12, y + 5, 0x404040, false);
		drawBook(context, x, y, delta);
		ItemStack hand = client.player != null ? client.player.getMainHandStack() : ItemStack.EMPTY;
		context.drawItem(hand, x + 15, y + 47);

		EnchantingPhrases.getInstance().setSeed(seed);
		hover = -1;
		for (int i = 0; i < 3; i++) {
			int sx = x + 60, sy = y + 14 + 19 * i;
			if (i >= names.size()) continue;
			boolean over = !chosen && mouseX >= sx && mouseX < sx + 108 && mouseY >= sy && mouseY < sy + 19;
			if (over) hover = i;
			context.drawGuiTexture(over ? SLOT_HIGHLIGHTED : SLOT, sx, sy, 108, 19);
			context.drawGuiTexture(LEVELS[i], sx + 1, sy + 1, 16, 16);
			Text free = Text.translatable("screen.datura_origins.altar.free");
			int freeWidth = textRenderer.getWidth(free);
			StringVisitable phrase = EnchantingPhrases.getInstance().generatePhrase(textRenderer, 86 - freeWidth);
			context.drawTextWrapped(textRenderer, phrase, sx + 20, sy + 2, 86 - freeWidth, over ? 0xFFFF80 : 0x685E4A);
			context.drawTextWithShadow(textRenderer, free, sx + 106 - freeWidth, sy + 9, 0x80FF20);
		}
		if (hover >= 0) {
			context.drawTooltip(textRenderer, List.of(names.get(hover).copy().formatted(Formatting.AQUA),
					Text.translatable("screen.datura_origins.altar.hint").formatted(Formatting.GRAY)), mouseX, mouseY);
		}
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		renderInGameBackground(context);
	}

	private void drawBook(DrawContext context, int x, int y, float delta) {
		float opened = MathHelper.lerp(delta, lastOpen, open);
		float angle = MathHelper.lerp(delta, lastPageAngle, pageAngle);
		DiffuseLighting.method_34742();
		context.getMatrices().push();
		context.getMatrices().translate(x + 33.0F, y + 31.0F, 100.0F);
		context.getMatrices().scale(-40.0F, 40.0F, 40.0F);
		context.getMatrices().multiply(RotationAxis.POSITIVE_X.rotationDegrees(25.0F));
		context.getMatrices().translate((1.0F - opened) * 0.2F, (1.0F - opened) * 0.1F, (1.0F - opened) * 0.25F);
		context.getMatrices().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-(1.0F - opened) * 90.0F - 90.0F));
		context.getMatrices().multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0F));
		float left = MathHelper.clamp(MathHelper.fractionalPart(angle + 0.25F) * 1.6F - 0.3F, 0.0F, 1.0F);
		float right = MathHelper.clamp(MathHelper.fractionalPart(angle + 0.75F) * 1.6F - 0.3F, 0.0F, 1.0F);
		book.setPageAngles(0.0F, left, right, opened);
		VertexConsumer buffer = context.getVertexConsumers().getBuffer(book.getLayer(BOOK_TEXTURE));
		book.render(context.getMatrices(), buffer, 0xF000F0, OverlayTexture.DEFAULT_UV);
		context.draw();
		context.getMatrices().pop();
		DiffuseLighting.enableGuiDepthLighting();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && hover >= 0 && !chosen) {
			chosen = true;
			if (ClientPlayNetworking.canSend(AltarChoicePayload.ID)) ClientPlayNetworking.send(new AltarChoicePayload(hover));
			client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 1.0F));
			close();
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public void close() {
		if (!chosen && ClientPlayNetworking.canSend(AltarChoicePayload.ID)) ClientPlayNetworking.send(new AltarChoicePayload(-1));
		chosen = true;
		super.close();
	}
}
