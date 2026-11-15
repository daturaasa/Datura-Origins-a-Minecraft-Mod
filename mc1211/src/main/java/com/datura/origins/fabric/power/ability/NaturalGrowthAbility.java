package com.datura.origins.fabric.power.ability;

import com.datura.origins.fabric.power.Ability;
import com.datura.origins.fabric.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.GrassBlock;
import net.minecraft.block.MossBlock;
import net.minecraft.block.NyliumBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class NaturalGrowthAbility implements Ability {
	private final int radius;
	private final int height;
	private final int maxSteps;
	private final double pushRadius;
	private final double pushPower;

	public NaturalGrowthAbility(JsonObject json) {
		radius = JsonParams.integer(json, "radius", 20);
		height = JsonParams.integer(json, "height", 8);
		maxSteps = JsonParams.integer(json, "max_steps", 12);
		pushRadius = JsonParams.num(json, "push_radius", 8.0F);
		pushPower = JsonParams.num(json, "push_power", 1.6F);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		ServerWorld w = p.getServerWorld();
		BlockPos c = p.getBlockPos();
		int grown = 0;
		BlockPos.Mutable m = new BlockPos.Mutable();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (dx * dx + dz * dz > radius * radius) continue;
				for (int dy = -height; dy <= height; dy++) {
					m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
					BlockState st = w.getBlockState(m);
					if (!(st.getBlock() instanceof Fertilizable) || st.getBlock() instanceof GrassBlock
							|| st.getBlock() instanceof MossBlock || st.getBlock() instanceof NyliumBlock) continue;
					BlockPos pos = m.toImmutable();
					if (!p.canModifyAt(w, pos)) continue;
					boolean any = false;
					for (int k = 0; k < maxSteps; k++) {
						BlockState cur = w.getBlockState(pos);
						if (!(cur.getBlock() instanceof Fertilizable f) || !f.isFertilizable(w, pos, cur)) break;
						f.grow(w, w.random, pos, cur);
						any = true;
					}
					if (any && ++grown % 2 == 0) {
						w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 6, 0.4, 0.4, 0.4, 0);
						w.spawnParticles(ParticleTypes.CHERRY_LEAVES, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 2, 0.5, 0.3, 0.5, 0);
					}
				}
			}
		}
		Vec3d o = p.getPos();
		push(p, w, o);
		for (int ring = 1; ring <= 4; ring++) {
			double r = radius * ring / 4.0;
			int n = 24 * ring;
			for (int i = 0; i < n; i++) {
				double a = i * Math.PI * 2 / n;
				w.spawnParticles(i % 3 == 0 ? ParticleTypes.SPORE_BLOSSOM_AIR : ParticleTypes.COMPOSTER,
						o.x + Math.cos(a) * r, o.y + 0.4, o.z + Math.sin(a) * r, 1, 0.1, 0.2, 0.1, 0);
			}
		}
		w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, o.x, o.y + 1, o.z, 60, 1.5, 1, 1.5, 0.1);
		w.spawnParticles(ParticleTypes.CHERRY_LEAVES, o.x, o.y + 1, o.z, 50, 2.5, 1, 2.5, 0.05);
		LegendKit.anim(p, "cast", 14);
		LegendKit.snd(p, SoundEvents.BLOCK_AZALEA_LEAVES_PLACE, 1.4F, 0.8F);
		LegendKit.snd(p, SoundEvents.BLOCK_CHERRY_LEAVES_BREAK, 1.2F, 0.6F);
		return true;
	}

	private void push(ServerPlayerEntity p, ServerWorld w, Vec3d o) {
		Box box = new Box(o, o).expand(pushRadius, 3, pushRadius);
		for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, box, x -> x != p && x.isAlive() && !x.isSpectator())) {
			if (e instanceof Tameable pet && p.getUuid().equals(pet.getOwnerUuid())) continue;
			Vec3d away = e.getPos().subtract(o).multiply(1, 0, 1);
			double dist = away.length();
			if (dist > pushRadius) continue;
			away = dist < 1.0E-3 ? new Vec3d(1, 0, 0) : away.multiply(1.0 / dist);
			double force = pushPower * (1.0 - dist / pushRadius * 0.6) * (1.0 - e.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE) * 0.5);
			e.addVelocity(away.x * force, 0.35 + force * 0.15, away.z * force);
			e.velocityModified = true;
		}
	}
}
