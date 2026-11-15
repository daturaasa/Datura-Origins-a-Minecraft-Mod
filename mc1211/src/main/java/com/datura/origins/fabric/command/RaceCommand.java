package com.datura.origins.fabric.command;

import com.datura.origins.fabric.OriginData;
import com.datura.origins.fabric.power.types.MaturityPower;
import com.datura.origins.fabric.race.RaceManager;
import com.datura.origins.race.Race;
import com.datura.origins.race.RaceRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Optional;

public final class RaceCommand {
	private RaceCommand() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
			CommandRegistryAccess registryAccess,
			CommandManager.RegistrationEnvironment environment) {
		dispatcher.register(CommandManager.literal("do")
				.then(build("race", "change", "set", "list", "player", "race"))
				.then(CommandManager.literal("full").requires(RaceCommand::cheats).then(CommandManager.literal("maturity")
						.executes(ctx -> fullMaturity(ctx.getSource(), ctx.getSource().getPlayerOrThrow()))))
				.then(CommandManager.literal("reset").requires(RaceCommand::cheats).then(CommandManager.literal("cooldown")
						.executes(ctx -> resetCooldown(ctx.getSource(), ctx.getSource().getPlayerOrThrow()))))
				.then(CommandManager.literal("no").requires(RaceCommand::cheats).then(CommandManager.literal("cooldown")
						.executes(ctx -> noCooldown(ctx.getSource(), ctx.getSource().getPlayerOrThrow())))));
	}

	private static boolean cheats(ServerCommandSource src) {
		return src.hasPermissionLevel(2) || src.getServer().isSingleplayer();
	}

	private static int fullMaturity(ServerCommandSource source, ServerPlayerEntity player) {
		if (!MaturityPower.maxOut(player)) {
			source.sendError(Text.translatable("datura_origins.command.no_maturity"));
			return 0;
		}
		RaceManager.snapAttributes(player);
		player.calculateDimensions();
		source.sendFeedback(() -> Text.translatable("datura_origins.command.full_maturity").formatted(Formatting.GOLD), false);
		return 1;
	}

	private static int resetCooldown(ServerCommandSource source, ServerPlayerEntity player) {
		RaceManager.resetCooldowns(player);
		source.sendFeedback(() -> Text.translatable("datura_origins.command.reset_cooldown").formatted(Formatting.AQUA), false);
		return 1;
	}

	private static int noCooldown(ServerCommandSource source, ServerPlayerEntity player) {
		boolean on = OriginData.NO_COOLDOWN.add(player.getUuid());
		if (!on) {
			OriginData.NO_COOLDOWN.remove(player.getUuid());
		} else {
			RaceManager.resetCooldowns(player);
		}
		source.sendFeedback(() -> Text.translatable(on ? "datura_origins.command.no_cooldown_on" : "datura_origins.command.no_cooldown_off")
				.formatted(on ? Formatting.GREEN : Formatting.GRAY), false);
		return 1;
	}

	private static LiteralArgumentBuilder<ServerCommandSource> build(String root, String change, String set, String list,
			String playerArg, String raceArg) {
		return CommandManager.literal(root)
				.executes(ctx -> show(ctx.getSource(), ctx.getSource().getPlayerOrThrow()))
				.then(CommandManager.literal(change)
						.executes(ctx -> change(ctx.getSource(), ctx.getSource().getPlayerOrThrow()))
						.then(CommandManager.argument(playerArg, EntityArgumentType.player())
								.requires(src -> src.hasPermissionLevel(2))
								.executes(ctx -> change(ctx.getSource(), EntityArgumentType.getPlayer(ctx, playerArg)))))
				.then(CommandManager.literal(set)
						.requires(src -> src.hasPermissionLevel(2))
						.then(CommandManager.argument(playerArg, EntityArgumentType.player())
								.then(CommandManager.argument(raceArg, StringArgumentType.word())
										.suggests((ctx, builder) -> CommandSource.suggestMatching(RaceRegistry.ids(), builder))
										.executes(ctx -> define(ctx.getSource(),
												EntityArgumentType.getPlayer(ctx, playerArg),
												StringArgumentType.getString(ctx, raceArg))))))
				.then(CommandManager.literal(list)
						.executes(ctx -> list(ctx.getSource())));
	}

	private static Text name(Race race) {
		return Text.translatableWithFallback(race.name(), race.name());
	}

	private static int show(ServerCommandSource source, ServerPlayerEntity player) {
		Optional<Race> race = OriginData.get(player);
		if (race.isPresent()) {
			source.sendFeedback(() -> Text.translatable("datura_origins.command.current",
							name(race.get()).copy().formatted(Formatting.GOLD)).formatted(Formatting.GRAY)
					.append(Text.translatable("datura_origins.command.change_hint").formatted(Formatting.DARK_GRAY)), false);
		} else {
			source.sendFeedback(() -> Text.translatable("datura_origins.command.none").formatted(Formatting.GRAY), false);
		}
		return 1;
	}

	private static int change(ServerCommandSource source, ServerPlayerEntity target) {
		OriginData.restartSelection(target);
		if (source.getPlayer() != target) {
			source.sendFeedback(() -> Text.translatable("datura_origins.command.other_choosing", target.getDisplayName()), true);
		}
		return 1;
	}

	private static int define(ServerCommandSource source, ServerPlayerEntity target, String id) {
		Optional<Race> race = RaceRegistry.get(id);
		if (race.isEmpty()) {
			source.sendError(Text.translatable("datura_origins.command.unknown", id));
			return 0;
		}
		OriginData.set(target, race.get());
		source.sendFeedback(() -> Text.translatable("datura_origins.command.defined",
				target.getDisplayName(), name(race.get())), true);
		return 1;
	}

	private static int list(ServerCommandSource source) {
		MutableText names = Text.empty();
		boolean first = true;
		for (Race race : RaceRegistry.all()) {
			if (!first) {
				names.append(Text.literal(", ").formatted(Formatting.DARK_GRAY));
			}
			names.append(name(race).copy().formatted(Formatting.GOLD))
					.append(Text.literal(" (" + race.id() + ")").formatted(Formatting.DARK_GRAY));
			first = false;
		}
		source.sendFeedback(() -> Text.translatable("datura_origins.command.list", names), false);
		return RaceRegistry.all().size();
	}
}
