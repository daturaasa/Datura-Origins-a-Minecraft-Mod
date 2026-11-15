package com.datura.origins.fabric.power.ability;

import com.datura.origins.fabric.entity.SpellProjectileEntity;
import com.datura.origins.fabric.power.Ability;
import com.datura.origins.fabric.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public final class ToxicCauldronAbility implements Ability {
	private static float radius = 8.0F;
	private static float damage = 1.0F;
	private static float impact = 5.0F;
	private static int duration = 200;
	private final double speed;

	public ToxicCauldronAbility(JsonObject json) {
		this.speed = JsonParams.num(json, "speed", 1.15F);
		radius = JsonParams.num(json, "radius", 8.0F);
		duration = JsonParams.integer(json, "duration", 200);
		damage = JsonParams.num(json, "damage", 1.0F);
		impact = JsonParams.num(json, "impact", 5.0F);
	}

	public static void bootstrap() {
		SpellProjectileEntity.onEntityHit(SpellProjectileEntity.CAULDRON, (projectile, hit) -> {
			if (hit instanceof LivingEntity living && projectile.getWorld() instanceof ServerWorld world) {
				living.timeUntilRegen = 0;
				living.damage(AreaZones.poison(world, projectile.getOwner()), impact);
			}
		});
		SpellProjectileEntity.onImpact(SpellProjectileEntity.CAULDRON, (projectile, at) -> {
			ServerWorld world = (ServerWorld) projectile.getWorld();
			Entity owner = projectile.getOwner();
			BlockPos pos = BlockPos.ofFloored(at);
			for (int i = 0; i < 6 && world.getBlockState(pos.down()).isAir(); i++) pos = pos.down();
			if (!world.getBlockState(pos).isAir() && !world.getBlockState(pos).isReplaceable()) pos = pos.up();
			List<BlockPos> placed = TempBlocks.place(world, pos, Blocks.CAULDRON.getDefaultState()) ? List.of(pos) : List.of();
			Vec3d center = Vec3d.ofBottomCenter(pos);
			AreaZones.create(world, center, radius, 40, duration, owner, damage, placed);
			world.spawnParticles(ParticleTypes.WITCH, center.x, center.y + 0.6, center.z, 40, 1.2, 0.6, 1.2, 0.1);
			world.spawnParticles(ParticleTypes.SNEEZE, center.x, center.y + 0.5, center.z, 30, 1.5, 0.3, 1.5, 0.05);
			LegendKit.sndAt(world, center, SoundEvents.BLOCK_ANVIL_LAND, 0.6F, 1.4F);
			LegendKit.sndAt(world, center, SoundEvents.ENTITY_SPLASH_POTION_BREAK, 1.2F, 0.7F);
		});
	}

	@Override
	public boolean activate(ServerPlayerEntity player) {
		SpellProjectileEntity cauldron = new SpellProjectileEntity(player.getWorld(), player, SpellProjectileEntity.CAULDRON, 0.045, 0.99, 120);
		Vec3d look = player.getRotationVec(1.0F);
		cauldron.setVelocity(look.x * speed, look.y * speed + 0.25, look.z * speed);
		player.getWorld().spawnEntity(cauldron);
		LegendKit.anim(player, "throw", 12);
		LegendKit.snd(player, SoundEvents.ENTITY_WITCH_THROW, 1.0F, 0.8F);
		return true;
	}
}
