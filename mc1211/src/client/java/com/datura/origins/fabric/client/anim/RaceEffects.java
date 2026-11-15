package com.datura.origins.fabric.client.anim;

import com.datura.origins.fabric.network.FireballPayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class RaceEffects {
	private static final double MAX_DIST_SQ = 64 * 64;
	private static final double BURST_DIST_SQ = 96 * 96;

	private static final Vector3f GOLD = new Vector3f(1.0f, 0.78f, 0.28f);
	private static final Vector3f ORANGE = new Vector3f(1.0f, 0.55f, 0.12f);
	private static final Vector3f VIOLET = new Vector3f(0.62f, 0.32f, 0.95f);
	private static final Vector3f DEEP_VIOLET = new Vector3f(0.36f, 0.14f, 0.62f);

	static final DustParticleEffect CORE = new DustParticleEffect(new Vector3f(1.0f, 0.95f, 0.7f), 1.9f);
	static final DustParticleEffect CORE_SMALL = new DustParticleEffect(new Vector3f(1.0f, 0.88f, 0.5f), 1.2f);
	private static final DustColorTransitionParticleEffect EMBER = new DustColorTransitionParticleEffect(ORANGE, VIOLET, 1.1f);
	private static final DustColorTransitionParticleEffect EMBER_BIG = new DustColorTransitionParticleEffect(GOLD, DEEP_VIOLET, 1.7f);

	static final DustParticleEffect BLOOD = new DustParticleEffect(new Vector3f(0.62f, 0.03f, 0.08f), 0.9f);
	static final DustParticleEffect BLOOD_DARK = new DustParticleEffect(new Vector3f(0.35f, 0.0f, 0.05f), 1.2f);
	static final DustParticleEffect NIGHT = new DustParticleEffect(new Vector3f(0.28f, 0.12f, 0.36f), 1.3f);
	static final DustParticleEffect FROST = new DustParticleEffect(new Vector3f(0.82f, 0.93f, 1.0f), 1.0f);
	static final DustParticleEffect EMBER_SOFT = new DustParticleEffect(new Vector3f(1.0f, 0.58f, 0.22f), 1.0f);

	private static final class Ball {
		Vec3d pos;
		final Vec3d vel;
		int age;

		Ball(Vec3d pos, Vec3d vel) {
			this.pos = pos;
			this.vel = vel;
		}
	}

	private static final Map<Integer, Ball> BALLS = new HashMap<>();

	private RaceEffects() {
	}

	public static void fxAt(String name, Vec3d p, float a, float b, float c) {
		ClientWorld world = MinecraftClient.getInstance().world;
		if (world == null || !near(p)) return;
		Random rnd = world.random;
		if (!name.equals("clone_poof")) {
			SpellEffects.fxAt(world, name, p, a, b, c);
			return;
		}
		{
			boolean snow = a > 0.5f;
			spiral(world, p, 0.45, 1.9, 2, 16, snow ? FROST : EMBER_SOFT, snow ? ParticleTypes.SNOWFLAKE : ParticleTypes.FLAME, rnd);
			for (int i = 0; i < 6; i++) world.addParticle(ParticleTypes.POOF, p.x + rnd.nextGaussian() * 0.2,
					p.y + rnd.nextDouble() * 1.6, p.z + rnd.nextGaussian() * 0.2, 0, 0.02, 0);
		}
	}

	public static void fx(String name, int entityId, int param) {
		ClientWorld world = MinecraftClient.getInstance().world;
		if (world == null) return;
		Entity e = world.getEntityById(entityId);
		if (e == null || !near(e.getPos())) return;
		Random rnd = world.random;
		switch (name) {
			case "bite_grab" -> ring(world, e.getPos().add(0, e.getStandingEyeHeight() * 0.85, 0), 0.45, 16, BLOOD_DARK);
			case "bite_drain" -> {
				Entity v = world.getEntityById(param);
				if (v != null) strand(world, e.getPos().add(0, e.getStandingEyeHeight() * 0.85, 0), v.getEyePos().add(0, -0.15, 0), rnd);
			}
			case "bat" -> spiral(world, e.getPos(), 0.55, 1.8, 2, 22, NIGHT, param == 1 ? ParticleTypes.SMOKE : ParticleTypes.ASH, rnd);
			default -> SpellEffects.fx(world, name, e, param);
		}
	}

	static void ring(ClientWorld world, Vec3d c, double r, int n, ParticleEffect fx) {
		for (int i = 0; i < n; i++) {
			double a = i * Math.PI * 2 / n;
			world.addParticle(fx, c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r, 0, 0, 0);
		}
	}

	private static void strand(ClientWorld world, Vec3d from, Vec3d to, Random rnd) {
		for (int i = 0; i <= 7; i++) {
			Vec3d p = from.lerp(to, i / 7.0).add(rnd.nextGaussian() * 0.03, Math.sin(i / 7.0 * Math.PI) * 0.12, rnd.nextGaussian() * 0.03);
			world.addParticle(i % 3 == 0 ? BLOOD_DARK : BLOOD, p.x, p.y, p.z, 0, 0, 0);
		}
		world.addParticle(ParticleTypes.FALLING_DRIPSTONE_LAVA, from.x, from.y - 0.1, from.z, 0, 0, 0);
	}

	static void spiral(ClientWorld world, Vec3d base, double r, double h, int arms, int steps,
							   ParticleEffect fx, ParticleEffect extra, Random rnd) {
		for (int s = 0; s < steps; s++) {
			double t = s / (double) steps;
			for (int k = 0; k < arms; k++) {
				double a = t * Math.PI * 3 + k * Math.PI * 2 / arms;
				double rr = r * (1.0 - t * 0.4);
				world.addParticle(fx, base.x + Math.cos(a) * rr, base.y + t * h, base.z + Math.sin(a) * rr, 0, 0, 0);
			}
		}
		for (int i = 0; i < 6; i++) {
			world.addParticle(extra, base.x + rnd.nextGaussian() * 0.3, base.y + rnd.nextDouble() * h, base.z + rnd.nextGaussian() * 0.3,
					0, 0.03, 0);
		}
	}

	public static void fireball(int id, Vec3d pos, Vec3d vel, int state) {
		ClientWorld world = MinecraftClient.getInstance().world;
		if (state == FireballPayload.FLYING) {
			BALLS.put(id, new Ball(pos, vel));
			if (world != null && near(pos)) muzzle(world, pos, vel, world.random);
			return;
		}
		BALLS.remove(id);
		if (world != null && near(pos)) {
			if (state == FireballPayload.EXPLODED) impact(world, pos, world.random);
			else fizzle(world, pos, world.random);
		}
	}

	public static void clear() {
		BALLS.clear();
	}

	private static boolean near(Vec3d pos) {
		return MinecraftClient.getInstance().gameRenderer.getCamera().getPos().squaredDistanceTo(pos) < BURST_DIST_SQ;
	}

	public static void tick(MinecraftClient client) {
		ClientWorld world = client.world;
		if (world == null || client.isPaused() || BALLS.isEmpty()) return;
		Vec3d cam = client.gameRenderer.getCamera().getPos();
		Random rnd = world.random;
		Iterator<Ball> it = BALLS.values().iterator();
		while (it.hasNext()) {
			Ball b = it.next();
			b.age++;
			if (b.age > 80) {
				it.remove();
				continue;
			}
			Vec3d prev = b.pos;
			b.pos = b.pos.add(b.vel);
			if (b.pos.squaredDistanceTo(cam) > MAX_DIST_SQ) continue;
			trail(world, b, prev, rnd);
		}
	}

	private static void muzzle(ClientWorld world, Vec3d pos, Vec3d vel, Random rnd) {
		Vec3d dir = vel.normalize();
		Vec3d[] basis = basis(dir);
		for (int i = 0; i < 12; i++) {
			double a = i * Math.PI * 2 / 12;
			Vec3d r = basis[0].multiply(Math.cos(a)).add(basis[1].multiply(Math.sin(a)));
			Vec3d v = r.multiply(0.13).add(dir.multiply(0.08));
			add(world, ParticleTypes.FLAME, pos.x, pos.y, pos.z, v.x, v.y, v.z);
		}
		for (int i = 0; i < 6; i++) {
			add(world, ParticleTypes.DRAGON_BREATH, pos.x, pos.y, pos.z,
					dir.x * 0.18 + rnd.nextGaussian() * 0.04, dir.y * 0.18 + rnd.nextGaussian() * 0.04,
					dir.z * 0.18 + rnd.nextGaussian() * 0.04);
		}
		add(world, CORE, pos.x, pos.y, pos.z, 0, 0, 0);
	}

	private static void trail(ClientWorld world, Ball b, Vec3d prev, Random rnd) {
		for (int k = 0; k < 3; k++) {
			Vec3d p = prev.lerp(b.pos, k / 3.0);
			add(world, k == 0 ? CORE : CORE_SMALL, p.x, p.y, p.z, 0, 0, 0);
			add(world, ParticleTypes.FLAME, p.x + rnd.nextGaussian() * 0.07, p.y + rnd.nextGaussian() * 0.07,
					p.z + rnd.nextGaussian() * 0.07, 0, 0, 0);
		}
		for (int k = 0; k < 2; k++) {
			add(world, ParticleTypes.FLAME, b.pos.x, b.pos.y, b.pos.z,
					-b.vel.x * 0.07 + rnd.nextGaussian() * 0.035,
					-b.vel.y * 0.07 + rnd.nextGaussian() * 0.035,
					-b.vel.z * 0.07 + rnd.nextGaussian() * 0.035);
		}
		Vec3d dir = b.vel.normalize();
		Vec3d[] basis = basis(dir);
		double ang = b.age * 0.9;
		for (int h = 0; h < 2; h++) {
			double a = ang + h * Math.PI;
			Vec3d off = basis[0].multiply(Math.cos(a) * 0.34).add(basis[1].multiply(Math.sin(a) * 0.34));
			if (h == 0) add(world, EMBER, b.pos.x + off.x, b.pos.y + off.y, b.pos.z + off.z, 0, 0, 0);
			else add(world, ParticleTypes.DRAGON_BREATH, b.pos.x + off.x, b.pos.y + off.y, b.pos.z + off.z, 0, 0, 0);
		}
		if (rnd.nextInt(2) == 0) {
			add(world, ParticleTypes.END_ROD, b.pos.x, b.pos.y, b.pos.z,
					rnd.nextGaussian() * 0.03, rnd.nextGaussian() * 0.03, rnd.nextGaussian() * 0.03);
		}
		if (b.age % 2 == 0) add(world, ParticleTypes.SMOKE, prev.x, prev.y, prev.z, 0, 0.01, 0);
		if (b.age % 3 == 0) add(world, ParticleTypes.LARGE_SMOKE, prev.x, prev.y, prev.z, 0, 0.015, 0);
		if (rnd.nextInt(4) == 0) add(world, ParticleTypes.LAVA, b.pos.x, b.pos.y, b.pos.z, 0, 0, 0);
	}

	private static void impact(ClientWorld world, Vec3d at, Random rnd) {
		add(world, ParticleTypes.FLASH, at.x, at.y, at.z, 0, 0, 0);
		for (int i = 0; i < 32; i++) {
			double a = i * Math.PI * 2 / 32 + rnd.nextDouble() * 0.1;
			double sp = 0.42 + rnd.nextDouble() * 0.12;
			add(world, ParticleTypes.FLAME, at.x, at.y + 0.15, at.z, Math.cos(a) * sp, 0.015, Math.sin(a) * sp);
		}
		for (int i = 0; i < 20; i++) {
			double a = i * Math.PI * 2 / 20;
			add(world, ParticleTypes.DRAGON_BREATH, at.x, at.y + 0.3, at.z, Math.cos(a) * 0.28, 0.02, Math.sin(a) * 0.28);
		}
		for (int i = 0; i < 16; i++) {
			add(world, ParticleTypes.FLAME, at.x + rnd.nextGaussian() * 0.35, at.y, at.z + rnd.nextGaussian() * 0.35,
					rnd.nextGaussian() * 0.03, 0.14 + rnd.nextDouble() * 0.28, rnd.nextGaussian() * 0.03);
		}
		for (int i = 0; i < 26; i++) {
			Vec3d p = at.add(rnd.nextGaussian() * 1.3, Math.abs(rnd.nextGaussian()) * 1.2, rnd.nextGaussian() * 1.3);
			if (i % 3 == 0) add(world, EMBER_BIG, p.x, p.y, p.z, 0, 0, 0);
			else add(world, EMBER, p.x, p.y, p.z, 0, 0, 0);
		}
		for (int i = 0; i < 12; i++) {
			add(world, ParticleTypes.LAVA, at.x, at.y + 0.2, at.z, 0, 0, 0);
		}
		for (int i = 0; i < 16; i++) {
			add(world, ParticleTypes.LARGE_SMOKE, at.x + rnd.nextGaussian() * 1.1, at.y + 0.4 + rnd.nextDouble(),
					at.z + rnd.nextGaussian() * 1.1, 0, 0.04 + rnd.nextDouble() * 0.04, 0);
		}
	}

	private static void fizzle(ClientWorld world, Vec3d at, Random rnd) {
		for (int i = 0; i < 12; i++) {
			add(world, ParticleTypes.LARGE_SMOKE, at.x + rnd.nextGaussian() * 0.3, at.y + rnd.nextGaussian() * 0.3,
					at.z + rnd.nextGaussian() * 0.3, 0, 0.03, 0);
		}
		for (int i = 0; i < 8; i++) {
			add(world, EMBER, at.x + rnd.nextGaussian() * 0.4, at.y + rnd.nextGaussian() * 0.4,
					at.z + rnd.nextGaussian() * 0.4, 0, 0, 0);
		}
	}

	private static void add(ClientWorld w, ParticleEffect fx, double x, double y, double z, double vx, double vy, double vz) {
		w.addParticle(fx, x, y, z, vx, vy, vz);
	}

	static Vec3d[] basis(Vec3d dir) {
		Vec3d side = dir.crossProduct(new Vec3d(0, 1, 0));
		if (side.lengthSquared() < 1e-4) side = new Vec3d(1, 0, 0);
		side = side.normalize();
		Vec3d up = side.crossProduct(dir).normalize();
		return new Vec3d[]{side, up};
	}
}
