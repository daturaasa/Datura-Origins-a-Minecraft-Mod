package com.datura.origins.fabric.race;

import com.datura.origins.fabric.power.Ability;
import com.datura.origins.fabric.power.RacePower;
import com.datura.origins.race.Race;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record RaceProfile(Race race, List<RacePower> powers, @Nullable AbilityInfo ability) {
	public static RaceProfile of(Race race) {
		return new RaceProfile(race, List.of(), null);
	}

	public String id() {
		return race.id();
	}

	public MutableText nameText() {
		return Text.translatableWithFallback(race.name(), race.name());
	}

	public record AbilityInfo(String name, int cooldownTicks, @Nullable Ability ability) {
		public MutableText nameText() {
			return Text.translatableWithFallback(name, name);
		}
	}
}
