package com.datura.origins.fabric.power;

import com.datura.origins.DaturaOrigins;
import com.datura.origins.fabric.DaturaOriginsMod;
import com.datura.origins.fabric.power.ability.AreaZones;
import com.datura.origins.fabric.power.ability.BlessingAltarAbility;
import com.datura.origins.fabric.power.ability.BoulderAbility;
import com.datura.origins.fabric.power.ability.CasinoAbility;
import com.datura.origins.fabric.power.ability.Channeling;
import com.datura.origins.fabric.power.ability.DarkCometAbility;
import com.datura.origins.fabric.power.ability.ElfDashAbility;
import com.datura.origins.fabric.power.ability.FireballAbility;
import com.datura.origins.fabric.power.ability.FlowerSanctuaryAbility;
import com.datura.origins.fabric.power.ability.FoxClonesAbility;
import com.datura.origins.fabric.power.ability.HeadbuttAbility;
import com.datura.origins.fabric.power.ability.LegendKit;
import com.datura.origins.fabric.power.ability.MeteorAbility;
import com.datura.origins.fabric.power.ability.MirrorShieldAbility;
import com.datura.origins.fabric.power.ability.MoonfallAbility;
import com.datura.origins.fabric.power.ability.NaturalGrowthAbility;
import com.datura.origins.fabric.power.ability.PhaseAbility;
import com.datura.origins.fabric.power.ability.ShadowStepAbility;
import com.datura.origins.fabric.power.ability.SpiritOrbAbility;
import com.datura.origins.fabric.power.ability.StompAbility;
import com.datura.origins.fabric.power.ability.SummonUndeadAbility;
import com.datura.origins.fabric.power.ability.TempBlocks;
import com.datura.origins.fabric.power.ability.TidalSongAbility;
import com.datura.origins.fabric.power.ability.ToxicCauldronAbility;
import com.datura.origins.fabric.power.ability.VampireBiteAbility;
import com.datura.origins.fabric.power.types.AnimalPeacePower;
import com.datura.origins.fabric.power.types.AquaticPower;
import com.datura.origins.fabric.power.types.ArcheryPower;
import com.datura.origins.fabric.power.types.AttributePower;
import com.datura.origins.fabric.power.types.BatFormPower;
import com.datura.origins.fabric.power.types.BerserkPower;
import com.datura.origins.fabric.power.types.ButcherPower;
import com.datura.origins.fabric.power.types.ClaustrophobiaPower;
import com.datura.origins.fabric.power.types.ConditionalAttributePower;
import com.datura.origins.fabric.power.types.DamageCapPower;
import com.datura.origins.fabric.power.types.DamageImmunityPower;
import com.datura.origins.fabric.power.types.DamageMultiplierPower;
import com.datura.origins.fabric.power.types.DesperateRegenPower;
import com.datura.origins.fabric.power.types.DietPower;
import com.datura.origins.fabric.power.types.EffectImmunityPower;
import com.datura.origins.fabric.power.types.EnchantWeaknessPower;
import com.datura.origins.fabric.power.types.EnvironmentDamagePower;
import com.datura.origins.fabric.power.types.FlagPower;
import com.datura.origins.fabric.power.types.FoodMultiplierPower;
import com.datura.origins.fabric.power.types.FortunePower;
import com.datura.origins.fabric.power.types.FoxFriendPower;
import com.datura.origins.fabric.power.types.FungalEmergencyPower;
import com.datura.origins.fabric.power.types.GoldenWardPower;
import com.datura.origins.fabric.power.types.GreedPower;
import com.datura.origins.fabric.power.types.GrowthAuraPower;
import com.datura.origins.fabric.power.types.HitEffectPower;
import com.datura.origins.fabric.power.types.HungerRatePower;
import com.datura.origins.fabric.power.types.InvertedPotionPower;
import com.datura.origins.fabric.power.types.LilyPadPower;
import com.datura.origins.fabric.power.types.MarkerPower;
import com.datura.origins.fabric.power.types.MaturityPower;
import com.datura.origins.fabric.power.types.MonsterTrucePower;
import com.datura.origins.fabric.power.types.MoonMarkPower;
import com.datura.origins.fabric.power.types.MountainLeapPower;
import com.datura.origins.fabric.power.types.NatureOathPower;
import com.datura.origins.fabric.power.types.NoArmorPower;
import com.datura.origins.fabric.power.types.NoHungerPower;
import com.datura.origins.fabric.power.types.PhotosynthesisPower;
import com.datura.origins.fabric.power.types.RegenerationPower;
import com.datura.origins.fabric.power.types.ScareAnimalsPower;
import com.datura.origins.fabric.power.types.SecondChancePower;
import com.datura.origins.fabric.power.types.SleepAltitudePower;
import com.datura.origins.fabric.power.types.SoulHarvestPower;
import com.datura.origins.fabric.power.types.StatuePower;
import com.datura.origins.fabric.power.types.StatusEffectPower;
import com.datura.origins.fabric.power.types.StealthPower;
import com.datura.origins.fabric.power.types.SunlightPower;
import com.datura.origins.fabric.power.types.ToolRestrictionPower;
import com.datura.origins.fabric.power.types.TranslucentPower;
import com.datura.origins.fabric.power.types.WingFlightPower;
import com.datura.origins.fabric.power.types.XpMultiplierPower;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public final class PowerRegistry {
	@FunctionalInterface
	public interface PowerFactory {
		RacePower create(JsonObject json);
	}

	@FunctionalInterface
	public interface AbilityFactory {
		Ability create(JsonObject json);
	}

	private static final Map<Identifier, PowerFactory> POWER_TYPES = new HashMap<>();
	private static final Map<Identifier, AbilityFactory> ABILITY_TYPES = new HashMap<>();
	private static boolean bootstrapped;

	private PowerRegistry() {}

	public static void registerPowerType(Identifier id, PowerFactory factory) {
		POWER_TYPES.put(id, factory);
	}

	public static void registerAbilityType(Identifier id, AbilityFactory factory) {
		ABILITY_TYPES.put(id, factory);
	}

	public static PowerFactory powerType(Identifier id) {
		return POWER_TYPES.get(id);
	}

	public static AbilityFactory abilityType(Identifier id) {
		return ABILITY_TYPES.get(id);
	}

	public static void bootstrap() {
		if (bootstrapped) return;
		bootstrapped = true;

		registerPowerType(DaturaOriginsMod.id("attribute"), AttributePower::new);
		registerPowerType(DaturaOriginsMod.id("status_effect"), StatusEffectPower::new);
		registerPowerType(DaturaOriginsMod.id("regeneration"), RegenerationPower::new);
		registerPowerType(DaturaOriginsMod.id("sunlight_damage"), SunlightPower::new);
		registerPowerType(DaturaOriginsMod.id("restricted_diet"), DietPower::new);
		registerPowerType(DaturaOriginsMod.id("maturity"), MaturityPower::new);
		registerPowerType(DaturaOriginsMod.id("damage_immunity"), DamageImmunityPower::new);
		registerPowerType(DaturaOriginsMod.id("damage_multiplier"), DamageMultiplierPower::new);
		registerPowerType(DaturaOriginsMod.id("effect_immunity"), EffectImmunityPower::new);
		registerPowerType(DaturaOriginsMod.id("no_armor"), NoArmorPower::new);
		registerPowerType(DaturaOriginsMod.id("claustrophobia"), ClaustrophobiaPower::new);
		registerPowerType(DaturaOriginsMod.id("flag"), FlagPower::new);
		registerPowerType(DaturaOriginsMod.id("hit_effect"), HitEffectPower::new);
		registerPowerType(DaturaOriginsMod.id("stealth"), StealthPower::new);
		registerPowerType(DaturaOriginsMod.id("fox_friend"), FoxFriendPower::new);
		registerPowerType(DaturaOriginsMod.id("no_swim"), MarkerPower::noSwim);
		registerPowerType(DaturaOriginsMod.id("powder_snow_walker"), MarkerPower::snowWalker);
		registerPowerType(DaturaOriginsMod.id("marker"), MarkerPower::generic);
		registerPowerType(DaturaOriginsMod.id("hunger_rate"), HungerRatePower::new);
		registerPowerType(DaturaOriginsMod.id("bat_form"), BatFormPower::new);
		registerPowerType(DaturaOriginsMod.id("conditional_attribute"), ConditionalAttributePower::new);
		registerPowerType(DaturaOriginsMod.id("archery"), ArcheryPower::new);
		registerPowerType(DaturaOriginsMod.id("xp_multiplier"), XpMultiplierPower::new);
		registerPowerType(DaturaOriginsMod.id("golden_ward"), GoldenWardPower::new);
		registerPowerType(DaturaOriginsMod.id("moon_mark"), MoonMarkPower::new);
		registerPowerType(DaturaOriginsMod.id("scare_animals"), ScareAnimalsPower::new);
		registerPowerType(DaturaOriginsMod.id("photosynthesis"), PhotosynthesisPower::new);
		registerPowerType(DaturaOriginsMod.id("growth_aura"), GrowthAuraPower::new);
		registerPowerType(DaturaOriginsMod.id("nature_oath"), NatureOathPower::new);
		registerPowerType(DaturaOriginsMod.id("environment_damage"), EnvironmentDamagePower::new);
		registerPowerType(DaturaOriginsMod.id("no_hunger"), NoHungerPower::new);
		registerPowerType(DaturaOriginsMod.id("monster_truce"), MonsterTrucePower::new);
		registerPowerType(DaturaOriginsMod.id("inverted_potions"), InvertedPotionPower::new);
		registerPowerType(DaturaOriginsMod.id("translucent"), TranslucentPower::new);
		registerPowerType(DaturaOriginsMod.id("berserk"), BerserkPower::new);
		registerPowerType(DaturaOriginsMod.id("food_multiplier"), FoodMultiplierPower::new);
		registerPowerType(DaturaOriginsMod.id("fungal_emergency"), FungalEmergencyPower::new);
		registerPowerType(DaturaOriginsMod.id("animal_peace"), AnimalPeacePower::new);
		registerPowerType(DaturaOriginsMod.id("wing_flight"), WingFlightPower::new);
		registerPowerType(DaturaOriginsMod.id("lily_pad"), LilyPadPower::new);
		registerPowerType(DaturaOriginsMod.id("aquatic"), AquaticPower::new);
		registerPowerType(DaturaOriginsMod.id("butcher"), ButcherPower::new);
		registerPowerType(DaturaOriginsMod.id("sleep_altitude"), SleepAltitudePower::new);
		registerPowerType(DaturaOriginsMod.id("mountain_leap"), MountainLeapPower::new);
		registerPowerType(DaturaOriginsMod.id("soul_harvest"), SoulHarvestPower::new);
		registerPowerType(DaturaOriginsMod.id("desperate_regeneration"), DesperateRegenPower::new);
		registerPowerType(DaturaOriginsMod.id("enchanted_weakness"), EnchantWeaknessPower::new);
		registerPowerType(DaturaOriginsMod.id("statue"), StatuePower::new);
		registerPowerType(DaturaOriginsMod.id("damage_cap"), DamageCapPower::new);
		registerPowerType(DaturaOriginsMod.id("greed"), GreedPower::new);
		registerPowerType(DaturaOriginsMod.id("noble_tools"), ToolRestrictionPower::new);
		registerPowerType(DaturaOriginsMod.id("fortune"), FortunePower::new);
		registerPowerType(DaturaOriginsMod.id("second_chance"), SecondChancePower::new);

		registerAbilityType(DaturaOriginsMod.id("vampire_bite"), VampireBiteAbility::new);
		registerAbilityType(DaturaOriginsMod.id("fireball"), FireballAbility::new);
		registerAbilityType(DaturaOriginsMod.id("fox_clones"), FoxClonesAbility::new);
		registerAbilityType(DaturaOriginsMod.id("dash"), ElfDashAbility::new);
		registerAbilityType(DaturaOriginsMod.id("mirror_shield"), MirrorShieldAbility::new);
		registerAbilityType(DaturaOriginsMod.id("shadow_step"), ShadowStepAbility::new);
		registerAbilityType(DaturaOriginsMod.id("moonfall"), MoonfallAbility::new);
		registerAbilityType(DaturaOriginsMod.id("natural_growth"), NaturalGrowthAbility::new);
		registerAbilityType(DaturaOriginsMod.id("toxic_cauldron"), ToxicCauldronAbility::new);
		registerAbilityType(DaturaOriginsMod.id("phase"), PhaseAbility::new);
		registerAbilityType(DaturaOriginsMod.id("boulder"), BoulderAbility::new);
		registerAbilityType(DaturaOriginsMod.id("flower_sanctuary"), FlowerSanctuaryAbility::new);
		registerAbilityType(DaturaOriginsMod.id("tidal_song"), TidalSongAbility::new);
		registerAbilityType(DaturaOriginsMod.id("blessing_altar"), BlessingAltarAbility::new);
		registerAbilityType(DaturaOriginsMod.id("stomp"), StompAbility::new);
		registerAbilityType(DaturaOriginsMod.id("headbutt"), HeadbuttAbility::new);
		registerAbilityType(DaturaOriginsMod.id("summon_undead"), SummonUndeadAbility::new);
		registerAbilityType(DaturaOriginsMod.id("meteor"), MeteorAbility::new);
		registerAbilityType(DaturaOriginsMod.id("casino"), CasinoAbility::new);
		registerAbilityType(DaturaOriginsMod.id("spirit_orb"), SpiritOrbAbility::new);
		registerAbilityType(DaturaOriginsMod.id("dark_comet"), DarkCometAbility::new);

		MaturityPower.init();
		EntityFlags.bootstrap();
		Control.bootstrap();
		HitEffectPower.bootstrap();
		VampireBiteAbility.bootstrap();
		BatFormPower.bootstrap();
		FireballAbility.bootstrap();
		LegendKit.bootstrap();
		Channeling.bootstrap();
		ArcheryPower.bootstrap();
		GoldenWardPower.bootstrap();
		MoonMarkPower.bootstrap();
		ElfDashAbility.bootstrap();
		MirrorShieldAbility.bootstrap();
		MoonfallAbility.bootstrap();
		TempBlocks.bootstrap();
		AreaZones.bootstrap();
		ToxicCauldronAbility.bootstrap();
		FungalEmergencyPower.bootstrap();
		NatureOathPower.bootstrap();
		MonsterTrucePower.bootstrap();
		BoulderAbility.bootstrap();
		AnimalPeacePower.bootstrap();
		StompAbility.bootstrap();
		ButcherPower.bootstrap();
		SleepAltitudePower.bootstrap();
		SoulHarvestPower.bootstrap();
		EnchantWeaknessPower.bootstrap();
		MeteorAbility.bootstrap();
		GreedPower.bootstrap();
		ToolRestrictionPower.bootstrap();
		FortunePower.bootstrap();
		CasinoAbility.bootstrap();
		SecondChancePower.bootstrap();

		for (DaturaOriginsPlugin plugin : FabricLoader.getInstance()
				.getEntrypoints(DaturaOrigins.MOD_ID + ":powers", DaturaOriginsPlugin.class)) {
			try {
				plugin.registerTypes();
			} catch (Exception e) {
				DaturaOriginsMod.LOGGER.error("Addon failed to register types: {}", plugin.getClass().getName(), e);
			}
		}
	}
}
