package com.datura.origins.fabric.power.ability;

import com.datura.origins.fabric.network.AnimPayload;
import com.datura.origins.fabric.network.FxPayload;
import com.datura.origins.fabric.network.Networking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class LegendKit {
		public interface Task {
		boolean tick(ServerPlayerEntity p, int age);

		default void end(ServerPlayerEntity p) {}
	}

	private record Running(UUID owner, String key, Task task, int[] age, int max) {}

	private static final List<Running> TASKS = new ArrayList<>();

	private LegendKit() {}

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(LegendKit::tickAll);
	}

		public static void run(ServerPlayerEntity p, String key, int maxTicks, Task task) {
		stop(p, key);
		TASKS.add(new Running(p.getUuid(), key, task, new int[]{0}, maxTicks));
	}

		public static void run(ServerPlayerEntity p, String key, int maxTicks, Task task, java.util.function.Consumer<ServerPlayerEntity> onEnd) {
		run(p, key, maxTicks, new Task() {
			@Override
			public boolean tick(ServerPlayerEntity pl, int age) {
				return task.tick(pl, age);
			}

			@Override
			public void end(ServerPlayerEntity pl) {
				onEnd.accept(pl);
			}
		});
	}

	public static boolean running(ServerPlayerEntity p, String key) {
		for (Running r : TASKS) if (r.owner.equals(p.getUuid()) && r.key.equals(key)) return true;
		return false;
	}

	public static void stop(ServerPlayerEntity p, String key) {
		for (Iterator<Running> it = TASKS.iterator(); it.hasNext(); ) {
			Running r = it.next();
			if (r.owner.equals(p.getUuid()) && r.key.equals(key)) {
				it.remove();
				r.task.end(p);
			}
		}
	}

		public static void stopPrefix(ServerPlayerEntity p, String prefix) {
		List<Running> gone = new ArrayList<>();
		for (Running r : TASKS) if (r.owner.equals(p.getUuid()) && r.key.startsWith(prefix)) gone.add(r);
		for (Running r : gone) if (TASKS.remove(r)) r.task.end(p);
	}

	private static void tickAll(MinecraftServer server) {
		if (TASKS.isEmpty()) return;
		List<Running> copy = new ArrayList<>(TASKS);
		for (Running r : copy) {
			ServerPlayerEntity p = server.getPlayerManager().getPlayer(r.owner);
			boolean keep;
			if (p == null || !p.isAlive()) keep = false;
			else {
				int a = r.age[0]++;
				keep = a < r.max && r.task.tick(p, a);
			}
			if (!keep && TASKS.remove(r) && p != null) r.task.end(p);
		}
	}

		public static boolean enemy(ServerPlayerEntity p, Entity e) {
		if (!(e instanceof LivingEntity l) || !l.isAlive() || e == p || e instanceof PlayerEntity) return false;
		return e instanceof Monster || (e instanceof MobEntity m && m.getTarget() == p);
	}

	public static List<LivingEntity> enemies(ServerPlayerEntity p, Vec3d c, double r) {
		return p.getWorld().getEntitiesByClass(LivingEntity.class, new net.minecraft.util.math.Box(c, c).expand(r),
				e -> enemy(p, e) && e.getPos().squaredDistanceTo(c) <= r * r);
	}

	public static List<PlayerEntity> allies(ServerPlayerEntity p, Vec3d c, double r) {
		return p.getWorld().getEntitiesByClass(PlayerEntity.class, new net.minecraft.util.math.Box(c, c).expand(r),
				e -> e.isAlive() && !e.isSpectator() && e.getPos().squaredDistanceTo(c) <= r * r);
	}

		public static LivingEntity aim(ServerPlayerEntity p, double range, double cone) {
		Vec3d eye = p.getEyePos(), look = p.getRotationVec(1f);
		LivingEntity best = null;
		double bestDot = cone;
		for (LivingEntity e : p.getWorld().getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(range),
				x -> x.isAlive() && x != p && !x.isSpectator())) {
			Vec3d to = e.getPos().add(0, e.getHeight() * 0.5, 0).subtract(eye);
			double len = to.length();
			if (len > range || len < 1e-3) continue;
			double dot = to.multiply(1 / len).dotProduct(look);
			if (dot > bestDot && p.canSee(e)) {
				bestDot = dot;
				best = e;
			}
		}
		return best;
	}

	public static void hurt(ServerPlayerEntity p, LivingEntity t, float dmg, boolean melee) {
		t.timeUntilRegen = 0;
		t.damage(melee ? p.getDamageSources().playerAttack(p) : p.getDamageSources().indirectMagic(p, p), dmg);
	}

	public static void snd(Entity e, SoundEvent s, float vol, float pitch) {
		e.getWorld().playSound(null, e.getX(), e.getY(), e.getZ(), s, SoundCategory.PLAYERS, vol, pitch);
	}

	public static void snd(Entity e, RegistryEntry<SoundEvent> s, float vol, float pitch) {
		snd(e, s.value(), vol, pitch);
	}

	public static void sndAt(ServerWorld w, Vec3d v, SoundEvent s, float vol, float pitch) {
		w.playSound(null, v.x, v.y, v.z, s, SoundCategory.PLAYERS, vol, pitch);
	}

	public static void sndAt(ServerWorld w, Vec3d v, RegistryEntry<SoundEvent> s, float vol, float pitch) {
		sndAt(w, v, s.value(), vol, pitch);
	}

	public static void anim(Entity e, String channel, int param) {
		Networking.sendAround(new AnimPayload(e.getId(), channel, param), e);
	}

	public static void fx(Entity e, String name, int param) {
		Networking.sendAround(new AnimPayload(e.getId(), "fx:" + name, param), e);
	}

	public static void fxAt(ServerWorld w, String name, Vec3d v, float a, float b, float c) {
		Networking.sendNear(w, v, 80, new FxPayload(name, v.x, v.y, v.z, a, b, c));
	}

	public static Vec3d flat(Vec3d v) {
		Vec3d f = new Vec3d(v.x, 0, v.z);
		return f.lengthSquared() < 1e-6 ? new Vec3d(0, 0, 1) : f.normalize();
	}
}
