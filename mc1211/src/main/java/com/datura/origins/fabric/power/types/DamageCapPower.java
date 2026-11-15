package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.JsonParams;
import com.datura.origins.fabric.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.block.Blocks;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public final class DamageCapPower implements RacePower {
	private static boolean reentry;
	private final float above, reduce;

	public DamageCapPower(JsonObject json) {
		this.above = JsonParams.num(json, "above", 10f);
		this.reduce = JsonParams.num(json, "reduce", 6f);
	}

	@Override
	public boolean allowDamage(ServerPlayerEntity p, DamageSource source, float amount) {
		if (reentry || amount <= above || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
		reentry = true;
		try {
			p.damage(source, amount - reduce);
		} finally {
			reentry = false;
		}
		p.getServerWorld().spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.STONE.getDefaultState()),
				p.getX(), p.getBodyY(0.6), p.getZ(), 12, 0.3, 0.4, 0.3, 0.1);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 0.8f, 0.6f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_DEEPSLATE_BREAK, SoundCategory.PLAYERS, 0.6f, 0.7f);
		return false;
	}
}
