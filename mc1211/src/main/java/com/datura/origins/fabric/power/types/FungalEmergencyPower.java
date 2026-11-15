package com.datura.origins.fabric.power.types;

import com.datura.origins.fabric.power.JsonParams;
import com.datura.origins.fabric.power.RacePower;
import com.datura.origins.fabric.power.ability.AreaZones;
import com.datura.origins.fabric.power.ability.LegendKit;
import com.datura.origins.fabric.power.ability.TempBlocks;
import com.datura.origins.fabric.race.RaceAccess;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FungalEmergencyPower implements RacePower {
	private static final Map<UUID, Long> READY_AT = new HashMap<>();

	private final float threshold;
	private final float damage;
	private final int cooldown;
	private final int stay;
	private final double radius;

	public FungalEmergencyPower(JsonObject json) {
		this.threshold = JsonParams.num(json, "threshold", 8.0F);
		this.cooldown = JsonParams.integer(json, "cooldown", 6000);
		this.radius = JsonParams.num(json, "radius", 6.0F);
		this.damage = JsonParams.num(json, "damage", 6.0F);
		this.stay = JsonParams.integer(json, "stay", 600);
	}

	public static void bootstrap() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			if (!(entity instanceof ServerPlayerEntity p) || !p.isAlive()) return;
			RaceAccess.getRace(p).flatMap(r -> r.powers().stream().filter(x -> x instanceof FungalEmergencyPower)
					.map(x -> (FungalEmergencyPower) x).findFirst()).ifPresent(power -> power.trigger(p));
		});
	}

	public static void resetCooldown(ServerPlayerEntity player) {
		READY_AT.remove(player.getUuid());
	}

	private void trigger(ServerPlayerEntity p) {
		if (p.getHealth() > threshold) return;
		ServerWorld w = p.getServerWorld();
		long now = w.getTime();
		Long ready = READY_AT.get(p.getUuid());
		if (ready != null && now < ready) return;
		READY_AT.put(p.getUuid(), now + cooldown);
		Vec3d c = p.getPos();
		for (LivingEntity e : LegendKit.enemies(p, c, radius)) {
			e.timeUntilRegen = 0;
			e.damage(AreaZones.poison(w, p), damage);
			Vec3d push = LegendKit.flat(e.getPos().subtract(c));
			e.addVelocity(push.x * 0.9, 0.5, push.z * 0.9);
			e.velocityModified = true;
		}
		w.spawnParticles(ParticleTypes.WITCH, c.x, c.y + 1, c.z, 60, radius * 0.4, 0.8, radius * 0.4, 0.15);
		w.spawnParticles(ParticleTypes.SPORE_BLOSSOM_AIR, c.x, c.y + 1, c.z, 40, 2, 1, 2, 0);
		LegendKit.sndAt(w, c, SoundEvents.ENTITY_SPLASH_POTION_BREAK, 1.2F, 0.6F);
		p.sendMessage(Text.translatable("datura_origins.message.fungal_emergency").formatted(Formatting.LIGHT_PURPLE), true);

		BlockPos base = groundBelow(w, p.getBlockPos());
		List<List<Object[]>> layers = layers(base);
		int topY = base.getY();
		for (List<Object[]> layer : layers) {
			for (Object[] o : layer) topY = Math.max(topY, ((BlockPos) o[0]).getY());
		}
		BlockPos top = new BlockPos(base.getX(), topY + 1, base.getZ());
		for (int i = 0; i < 6 && !(w.getBlockState(top).isAir() && w.getBlockState(top.up()).isAir()); i++) top = top.up();
		p.stopRiding();
		p.setVelocity(Vec3d.ZERO);
		p.requestTeleport(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
		p.fallDistance = 0;
		List<BlockPos> placed = new ArrayList<>();
		for (List<Object[]> layer : layers) {
			for (Object[] o : layer) {
				BlockPos pos = (BlockPos) o[0];
				if (TempBlocks.place(w, pos, (BlockState) o[1])) placed.add(pos);
			}
		}
		Vec3d foot = Vec3d.ofBottomCenter(base);
		LegendKit.sndAt(w, Vec3d.ofCenter(base), SoundEvents.BLOCK_FUNGUS_PLACE, 1.3F, 0.6F);
		LegendKit.sndAt(w, Vec3d.ofCenter(base), SoundEvents.BLOCK_ROOTED_DIRT_BREAK, 1.0F, 0.5F);
		AreaZones.create(w, foot, 7.0F, 6, stay, p, 1.0F, placed);
		LegendKit.run(p, "fungal_regen", 200, (pp, age) -> {
			if (age % 10 == 0) pp.heal(1.0F);
			pp.fallDistance = 0;
			return true;
		});
	}

	private static BlockPos groundBelow(ServerWorld w, BlockPos from) {
		BlockPos p = from;
		for (int i = 0; i < 8 && w.getBlockState(p.down()).isAir(); i++) p = p.down();
		return p;
	}

	private static List<List<Object[]>> layers(BlockPos base) {
		BlockState stem = Blocks.MUSHROOM_STEM.getDefaultState();
		BlockState cap = Blocks.RED_MUSHROOM_BLOCK.getDefaultState();
		List<List<Object[]>> out = new ArrayList<>();
		for (int y = 0; y <= 9; y++) {
			List<Object[]> layer = new ArrayList<>();
			int r = y < 2 ? 2 : 1;
			for (int x = -r; x <= r; x++) {
				for (int z = -r; z <= r; z++) {
					if (!(Math.abs(x) == 2 && Math.abs(z) == 2)) layer.add(new Object[]{base.add(x, y, z), stem});
				}
			}
			out.add(layer);
		}
		double[] rad = {4.6, 5.3, 4.6, 3.4, 1.9};
		for (int k = 0; k < rad.length; k++) {
			List<Object[]> layer = new ArrayList<>();
			int y = 10 + k;
			int r = (int) Math.ceil(rad[k]);
			for (int x = -r; x <= r; x++) {
				for (int z = -r; z <= r; z++) {
					double d = Math.sqrt(x * x + z * z);
					if (d > rad[k]) continue;
					if (k > 0 && k < rad.length - 1 && d < rad[k + 1] - 0.6) continue;
					layer.add(new Object[]{base.add(x, y, z), k == 0 && d < 1.6 ? stem : cap});
				}
			}
			out.add(layer);
		}
		return out;
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		READY_AT.remove(p.getUuid());
	}
}
