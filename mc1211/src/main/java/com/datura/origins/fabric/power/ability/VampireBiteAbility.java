package com.datura.origins.fabric.power.ability;

import com.datura.origins.fabric.DaturaOriginsMod;
import com.datura.origins.fabric.network.BiteAnimPayload;
import com.datura.origins.fabric.network.Networking;
import com.datura.origins.fabric.power.Ability;
import com.datura.origins.fabric.power.JsonParams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class VampireBiteAbility implements Ability {
	public static final TagKey<EntityType<?>> IMMUNE = TagKey.of(RegistryKeys.ENTITY_TYPE, DaturaOriginsMod.id("bite_immune"));
	private static final Identifier ROOT_ID = DaturaOriginsMod.id("bite_root");
		private static final int PULL_TICKS = 5;
		private static final int FIRST_HIT_TICK = 8;

	private record MonsterEffect(RegistryEntry<StatusEffect> effect, int duration, int amplifier) {}

	private final double range;
	private final int duration;
	private final int hits;
	private final float damagePerHit;
	private final float healRatio;
	private final float foodRatio;
	private final float saturationRatio;
	private final List<MonsterEffect> monsterEffects = new ArrayList<>();

	public VampireBiteAbility(JsonObject json) {
		this.range = JsonParams.num(json, "range", 3f);
		this.duration = Math.max(FIRST_HIT_TICK + 4, JsonParams.integer(json, "duration", 30));
		this.hits = Math.max(1, JsonParams.integer(json, "hits", 5));
		this.damagePerHit = JsonParams.num(json, "damage_per_hit", 1.2f);
		this.healRatio = JsonParams.num(json, "heal_ratio", 1f);
		this.foodRatio = JsonParams.num(json, "food_ratio", 1f);
		this.saturationRatio = JsonParams.num(json, "saturation_ratio", 0.4f);
		if (json.has("monster_effects")) {
			for (JsonElement e : json.getAsJsonArray("monster_effects")) {
				JsonObject o = e.getAsJsonObject();
				Identifier id = Identifier.of(o.get("effect").getAsString());
				Registries.STATUS_EFFECT.getEntry(id).ifPresentOrElse(
						entry -> monsterEffects.add(new MonsterEffect(entry,
								JsonParams.integer(o, "duration", 80), JsonParams.integer(o, "amplifier", 0))),
						() -> DaturaOriginsMod.LOGGER.error("Bite: unknown effect {}", id));
			}
		}
	}

	private static final class Session {
		final VampireBiteAbility config;
		final ServerPlayerEntity vampire;
		final LivingEntity victim;
		final Vec3d start;
		int age = 0;
		int hitsDone = 0;
		boolean poisoned = false;

		Session(VampireBiteAbility config, ServerPlayerEntity vampire, LivingEntity victim) {
			this.config = config;
			this.vampire = vampire;
			this.victim = victim;
			this.start = victim.getPos();
		}
	}

		private static final Map<UUID, Session> SESSIONS = new HashMap<>();

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(VampireBiteAbility::tickAll);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			Session s = SESSIONS.get(entity.getUuid());
			return s == null || source.getAttacker() != s.victim;
		});
	}

	public static boolean isBiting(PlayerEntity player) {
		return SESSIONS.containsKey(player.getUuid());
	}

	public static boolean isHeld(Entity entity) {
		for (Session s : SESSIONS.values()) if (s.victim == entity) return true;
		return false;
	}

	@Override
	public boolean activate(ServerPlayerEntity player) {
		if (SESSIONS.containsKey(player.getUuid())) return false;
		if (BatFormState.isBat(player)) {
			player.sendMessage(Text.translatable("datura_origins.message.bite_in_bat").formatted(Formatting.GRAY), true);
			return false;
		}
		LivingEntity target = findTarget(player);
		if (target == null) {
			player.sendMessage(Text.translatable("datura_origins.message.bite_no_prey").formatted(Formatting.GRAY), true);
			return false;
		}

		Session s = new Session(this, player, target);
		SESSIONS.put(player.getUuid(), s);
		setRooted(player, true);
		Networking.sendAround(new BiteAnimPayload(player.getId(), target.getId(), duration), player, target);
		player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ENTITY_PHANTOM_SWOOP, SoundCategory.PLAYERS, 0.6f, 1.4f);
		Networking.sendAround(new com.datura.origins.fabric.network.AnimPayload(target.getId(), "fx:bite_grab", player.getId()), player, target);
		return true;
	}

	private LivingEntity findTarget(ServerPlayerEntity player) {
		Vec3d eye = player.getEyePos();
		Vec3d look = player.getRotationVec(1f);
		LivingEntity best = null;
		double bestScore = Double.MAX_VALUE;
		List<Entity> candidates = player.getWorld().getOtherEntities(player,
				player.getBoundingBox().expand(range + 1), e -> e instanceof LivingEntity);
		for (Entity e : candidates) {
			LivingEntity le = (LivingEntity) e;
			if (!canBite(player, le)) continue;
			Vec3d center = le.getBoundingBox().getCenter();
			Vec3d to = center.subtract(eye);
			double dist = to.length() - le.getWidth() / 2.0;
			if (dist > range) continue;
			double dot = to.normalize().dotProduct(look);
			if (dot < 0.55) continue;
			double score = dist * (2.0 - dot);
			if (score < bestScore) {
				bestScore = score;
				best = le;
			}
		}
		return best;
	}

	private static boolean canBite(ServerPlayerEntity player, LivingEntity le) {
		if (!le.isAlive() || le.isSpectator() || le.isInvulnerable()) return false;
		if (le.getType().isIn(IMMUNE)) return false;
		if (le.getWidth() > 2.0f || le.getHeight() > 3.0f) return false;
		if (isHeld(le) || (le instanceof PlayerEntity p && (p.isCreative() || isBiting(p)))) return false;
		if (le instanceof PlayerEntity p && !player.shouldDamagePlayer(p)) return false;
		return player.canSee(le);
	}

	private static void tickAll(MinecraftServer server) {
		if (SESSIONS.isEmpty()) return;
		Iterator<Session> it = SESSIONS.values().iterator();
		while (it.hasNext()) {
			Session s = it.next();
			if (!s.config.tick(s)) {
				it.remove();
				s.config.finish(s);
			}
		}
	}

		private boolean tick(Session s) {
		ServerPlayerEntity v = s.vampire;
		LivingEntity prey = s.victim;
		if (v.isRemoved() || !v.isAlive() || prey.isRemoved() || !prey.isAlive()) return false;
		if (prey.getWorld() != v.getWorld() || prey.squaredDistanceTo(v) > (range + 3) * (range + 3)) return false;
		if (BatFormState.isBat(v)) return false;

		s.age++;
		holdInPlace(s);

		if (s.age >= FIRST_HIT_TICK && s.hitsDone < hits) {
			int span = Math.max(1, (duration - 4 - FIRST_HIT_TICK) / hits);
			if ((s.age - FIRST_HIT_TICK) % span == 0) drain(s);
		}
		return s.age < duration;
	}

		private void holdInPlace(Session s) {
		ServerPlayerEntity v = s.vampire;
		LivingEntity prey = s.victim;
		com.datura.origins.fabric.power.Control.lock(prey, 3);
		float yaw = v.getYaw();
		Vec3d forward = Vec3d.fromPolar(0f, yaw);
		double gap = 0.45 + prey.getWidth() * 0.5;
		Vec3d hold = v.getPos().add(forward.multiply(gap));

		Vec3d pos;
		if (s.age <= PULL_TICKS) {
			float t = s.age / (float) PULL_TICKS;
			t = t * t * (3 - 2 * t);
			pos = s.start.lerp(hold, t);
		} else {
			pos = hold;
		}
		float faceYaw = MathHelper.wrapDegrees(yaw + 180f);

		if (prey instanceof ServerPlayerEntity sp) {
			sp.networkHandler.requestTeleport(pos.x, pos.y, pos.z, faceYaw, sp.getPitch());
		} else {
			prey.refreshPositionAndAngles(pos.x, pos.y, pos.z, faceYaw, prey.getPitch());
			prey.setHeadYaw(faceYaw);
			prey.setBodyYaw(faceYaw);
			if (prey instanceof MobEntity mob) mob.getNavigation().stop();
		}
		prey.setVelocity(Vec3d.ZERO);
		prey.velocityModified = true;
		prey.fallDistance = 0;
		v.setVelocity(Vec3d.ZERO);
	}

	private void drain(Session s) {
		ServerPlayerEntity v = s.vampire;
		LivingEntity prey = s.victim;
		s.hitsDone++;

		float before = prey.getHealth();
		prey.timeUntilRegen = 0;
		prey.damage(v.getDamageSources().playerAttack(v), damagePerHit);
		prey.setVelocity(Vec3d.ZERO);
		prey.velocityModified = true;
		float drained = Math.max(0f, before - prey.getHealth());

		if (drained > 0f) {
			v.heal(drained * healRatio);
			int food = Math.max(1, Math.round(drained * foodRatio));
			v.getHungerManager().add(food, drained * saturationRatio);
		}

		if (prey instanceof Monster && !s.poisoned) {
			s.poisoned = true;
			for (MonsterEffect me : monsterEffects) {
				v.addStatusEffect(new StatusEffectInstance(me.effect(), me.duration(), me.amplifier()));
			}
		}

		if (v.getWorld() instanceof ServerWorld sw) {
			Vec3d neck = prey.getPos().add(0, prey.getStandingEyeHeight() * 0.85, 0);
			sw.spawnParticles(new DustParticleEffect(new Vector3f(0.55f, 0.02f, 0.06f), 1.1f),
					neck.x, neck.y, neck.z, 10, 0.15, 0.12, 0.15, 0.0);
			sw.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, neck.x, neck.y, neck.z, 1, 0.1, 0.1, 0.1, 0.0);
			sw.playSound(null, v.getX(), v.getY(), v.getZ(), SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS,
					0.5f, 0.75f + v.getRandom().nextFloat() * 0.15f);
			sw.playSound(null, prey.getX(), prey.getY(), prey.getZ(), SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.PLAYERS,
					0.45f, 1.1f);
			Networking.sendAround(new com.datura.origins.fabric.network.AnimPayload(prey.getId(), "fx:bite_drain", v.getId()), v, prey);
			if (s.hitsDone == 1) {
				sw.playSound(null, prey.getX(), prey.getY(), prey.getZ(), SoundEvents.ENTITY_PHANTOM_BITE,
						SoundCategory.PLAYERS, 0.8f, 0.9f);
			}
		}
	}

	private void finish(Session s) {
		setRooted(s.vampire, false);
		Networking.sendAround(new BiteAnimPayload(s.vampire.getId(), s.victim.getId(), 0), s.vampire, s.victim);
		if (s.victim.isAlive() && !s.victim.isRemoved()) {
			Vec3d push = Vec3d.fromPolar(0f, s.vampire.getYaw()).multiply(0.35).add(0, 0.15, 0);
			s.victim.setVelocity(push);
			s.victim.velocityModified = true;
		}
	}

		private static void setRooted(ServerPlayerEntity player, boolean rooted) {
		EntityAttributeInstance speed = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
		if (speed == null) return;
		if (rooted && !speed.hasModifier(ROOT_ID)) {
			speed.addTemporaryModifier(new EntityAttributeModifier(ROOT_ID, -1.0,
					EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		} else if (!rooted) {
			speed.removeModifier(ROOT_ID);
		}
	}

	@Override
	public void onRemove(ServerPlayerEntity player) {
		Session s = SESSIONS.remove(player.getUuid());
		if (s != null) finish(s);
		else setRooted(player, false);
	}
}
