package com.datura.origins.race;

import com.datura.origins.DaturaOrigins;

public final class Races {
	public static final Race HUMANO = RaceRegistry.register(Race.builder(DaturaOrigins.MOD_ID + ":humano")
			.name("Humano")
			.description("Sem particularidades: não tem vantagens, desvantagens nem habilidades. É o Minecraft como ele é.")
			.color(0xD8D2C4)
			.build());

	private Races() {
	}

	public static void bootstrap() {
	}
}
