package com.datura.origins.fabric.power.ability;

import com.datura.origins.fabric.entity.ModEntities;
import com.datura.origins.fabric.entity.UndeadMinionEntity;
import com.datura.origins.fabric.power.Ability;
import com.datura.origins.fabric.power.JsonParams;
import com.datura.origins.fabric.power.types.MaturityPower;
import com.google.gson.JsonObject;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

public final class SummonUndeadAbility implements Ability {
	private final int count, duration;

	public SummonUndeadAbility(JsonObject json) {
		this.count = JsonParams.integer(json, "count", 3);
		this.duration = JsonParams.integer(json, "duration", 600);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		ServerWorld w = p.getServerWorld();
		for (UndeadMinionEntity old : w.getEntitiesByClass(UndeadMinionEntity.class, p.getBoundingBox().expand(64),
				m -> m.ownerUuid().map(u -> u.equals(p.getUuid())).orElse(false))) old.vanish();
		int stage = MaturityPower.stage(p);
		for (int i = 0; i < count; i++) {
			double a = Math.toRadians(p.getYaw()) + Math.PI / 2 + (i - (count - 1) / 2.0) * 0.7;
			double r = 2.2;
			double x = p.getX() + Math.cos(a) * r, z = p.getZ() + Math.sin(a) * r;
			BlockPos bp = BlockPos.ofFloored(x, p.getY(), z);
			double y = p.getY();
			for (int dy = 2; dy >= -3; dy--) {
				BlockPos c = bp.up(dy);
				if (w.getBlockState(c.down()).isSolidBlock(w, c.down()) && w.isAir(c) && w.isAir(c.up())) {
					y = c.getY();
					break;
				}
			}
			UndeadMinionEntity m = ModEntities.UNDEAD_MINION.create(w);
			if (m == null) continue;
			m.refreshPositionAndAngles(x, y, z, p.getYaw(), 0);
			m.setup(p, stage, duration + i * 3);
			w.spawnEntity(m);
			w.spawnParticles(ParticleTypes.SOUL, x, y + 0.2, z, 12, 0.35, 0.2, 0.35, 0.02);
			w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, w.getBlockState(BlockPos.ofFloored(x, y - 0.5, z))),
					x, y + 0.1, z, 18, 0.4, 0.1, 0.4, 0.1);
		}
		LegendKit.anim(p, "cast", 14);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ZOMBIE_AMBIENT, SoundCategory.PLAYERS, 0.8f, 0.6f);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_ROOTED_DIRT_BREAK, SoundCategory.PLAYERS, 1.0f, 0.5f);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_SOUL_SOIL_BREAK, SoundCategory.PLAYERS, 1.0f, 0.6f);
		return true;
	}
}
