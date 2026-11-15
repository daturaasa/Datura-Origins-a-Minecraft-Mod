package com.datura.origins.fabric;

import com.datura.origins.DaturaOrigins;
import com.datura.origins.fabric.command.RaceCommand;
import com.datura.origins.fabric.entity.ModEntities;
import com.datura.origins.fabric.network.Networking;
import com.datura.origins.fabric.power.PowerRegistry;
import com.datura.origins.fabric.race.RaceEvents;
import com.datura.origins.fabric.race.RaceLoader;
import com.datura.origins.fabric.race.RaceManager;
import com.datura.origins.race.RaceProvider;
import com.datura.origins.race.Races;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DaturaOriginsMod implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(DaturaOrigins.MOD_NAME);

	public static Identifier id(String path) {
		return Identifier.of(DaturaOrigins.MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		Races.bootstrap();
		PowerRegistry.bootstrap();
		RaceLoader.load();
		FabricLoader.getInstance()
				.getEntrypoints(DaturaOrigins.MOD_ID, RaceProvider.class)
				.forEach(RaceProvider::registerRaces);
		ModEntities.init();
		OriginData.init();
		SelectionGuard.init();
		Networking.init();
		RaceEvents.register();
		ServerTickEvents.END_SERVER_TICK.register(RaceManager::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> RaceManager.onLeave(handler.getPlayer()));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> RaceManager.onRespawn(newPlayer));
		CommandRegistrationCallback.EVENT.register(RaceCommand::register);
	}
}
