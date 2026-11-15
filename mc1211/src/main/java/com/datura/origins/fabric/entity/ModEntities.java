package com.datura.origins.fabric.entity;

import com.datura.origins.fabric.DaturaOriginsMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEntities {
	public static final EntityType<FoxCloneEntity> FOX_CLONE = Registry.register(Registries.ENTITY_TYPE,
			DaturaOriginsMod.id("fox_clone"),
			EntityType.Builder.create(FoxCloneEntity::new, SpawnGroup.MISC).dimensions(0.6f, 1.8f).maxTrackingRange(8)
					.build("fox_clone"));

	public static final EntityType<SpellProjectileEntity> SPELL_PROJECTILE = Registry.register(Registries.ENTITY_TYPE,
			DaturaOriginsMod.id("spell_projectile"),
			EntityType.Builder.<SpellProjectileEntity>create(SpellProjectileEntity::new, SpawnGroup.MISC).dimensions(0.6f, 0.6f)
					.maxTrackingRange(8).trackingTickInterval(1).alwaysUpdateVelocity(true).build("spell_projectile"));

	public static final EntityType<UndeadMinionEntity> UNDEAD_MINION = Registry.register(Registries.ENTITY_TYPE,
			DaturaOriginsMod.id("undead_minion"),
			EntityType.Builder.<UndeadMinionEntity>create(UndeadMinionEntity::new, SpawnGroup.MISC).dimensions(0.6f, 1.95f)
					.eyeHeight(1.74f).maxTrackingRange(8).build("undead_minion"));

	public static final EntityType<SoulOrbEntity> SOUL_ORB = Registry.register(Registries.ENTITY_TYPE,
			DaturaOriginsMod.id("soul_orb"),
			EntityType.Builder.<SoulOrbEntity>create(SoulOrbEntity::new, SpawnGroup.MISC).dimensions(0.5f, 0.5f)
					.maxTrackingRange(6).trackingTickInterval(20).build("soul_orb"));

	private ModEntities() {}

	public static void init() {
		FabricDefaultAttributeRegistry.register(FOX_CLONE, FoxCloneEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(UNDEAD_MINION, UndeadMinionEntity.createAttributes());
	}
}
