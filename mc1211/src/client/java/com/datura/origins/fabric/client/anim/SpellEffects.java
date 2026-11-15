package com.datura.origins.fabric.client.anim;

import com.datura.origins.fabric.power.EntityFlags;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class SpellEffects {
	private static final DustParticleEffect GOLD = dust(0xFFD24A), BLUE = dust(0x6FB8FF);
	private static final DustParticleEffect GOLD_LINE = new DustParticleEffect(new Vector3f(1.0f, 0.82f, 0.3f), 1.1f);
	private static final DustParticleEffect SHADOW = new DustParticleEffect(new Vector3f(0.16f, 0.05f, 0.26f), 1.7f);
	private static final DustParticleEffect SHADOW_EDGE = new DustParticleEffect(new Vector3f(0.55f, 0.3f, 0.85f), 0.8f);
	private static final DustParticleEffect LEAF = new DustParticleEffect(new Vector3f(0.42f, 0.72f, 0.36f), 1.0f);

	private enum Kind { BOOM_GOLD, BOOM_ARCANE, FLASH }

	private static final class Shape {
		final Kind kind;
		final Vec3d pos;
		final float radius;
		final int life;
		final Entity follow;
		int age;

		Shape(Kind kind, Vec3d pos, float radius, int life, Entity follow) {
			this.kind = kind;
			this.pos = pos;
			this.radius = radius;
			this.life = life;
			this.follow = follow;
		}
	}

	private static final List<Shape> SHAPES = new ArrayList<>();

	private SpellEffects() {
	}

	private static DustParticleEffect dust(int rgb) {
		return new DustParticleEffect(new Vector3f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f), 1.2f);
	}

	public static void clear() {
		SHAPES.clear();
	}

	public static void fx(ClientWorld world, String name, Entity e, int param) {
		Random rnd = world.random;
		switch (name) {
			case "dash" -> {
				Vec3d b = e.getPos();
				for (int i = 0; i < 10; i++) world.addParticle(ParticleTypes.CLOUD, b.x + rnd.nextGaussian() * 0.25, b.y + 0.1,
						b.z + rnd.nextGaussian() * 0.25, rnd.nextGaussian() * 0.03, 0.02, rnd.nextGaussian() * 0.03);
				RaceEffects.ring(world, b.add(0, 0.05, 0), 0.6, 14, LEAF);
			}
			case "mirror_hit" -> {
				Vec3d c = e.getEyePos().add(e.getRotationVec(1f).multiply(1.0)).add(0, -0.3, 0);
				firework(world, c, 0xFFE27A, 0xFFFFFF);
				Vec3d[] basis = RaceEffects.basis(e.getRotationVec(1f));
				for (int i = 0; i < 6; i++) {
					double a = i * Math.PI / 3;
					Vec3d q = c.add(basis[0].multiply(Math.cos(a))).add(basis[1].multiply(Math.sin(a)));
					world.addParticle(ParticleTypes.ELECTRIC_SPARK, q.x, q.y, q.z, 0, 0, 0);
				}
				Entity attacker = world.getEntityById(param);
				if (attacker != null) {
					Vec3d to = attacker.getPos().add(0, attacker.getHeight() * 0.6, 0);
					for (int i = 0; i <= 10; i++) {
						Vec3d q = c.lerp(to, i / 10.0);
						world.addParticle(GOLD_LINE, q.x, q.y, q.z, 0, 0, 0);
					}
					world.addParticle(ParticleTypes.FLASH, to.x, to.y, to.z, 0, 0, 0);
				}
			}
			case "shield_flash" -> SHAPES.add(new Shape(Kind.FLASH, e.getPos(), 1.8f, 10, e));
			case "ward_melee", "ward_blink" -> SHAPES.add(new Shape(Kind.FLASH, e.getPos(), name.equals("ward_melee") ? 3.5f : 2f, 14, e));
			default -> {
			}
		}
	}

	public static void fxAt(ClientWorld world, String name, Vec3d p, float a, float b, float c) {
		Random rnd = world.random;
		switch (name) {
			case "boom_gold" -> SHAPES.add(new Shape(Kind.BOOM_GOLD, p, a, 14, null));
			case "boom_arcane" -> SHAPES.add(new Shape(Kind.BOOM_ARCANE, p, a, 16, null));
			case "shadow_step" -> {
				Vec3d to = new Vec3d(a, b, c);
				for (Vec3d end : new Vec3d[]{p, to}) {
					for (int i = 0; i < 14; i++) world.addParticle(SHADOW, end.x + rnd.nextGaussian() * 0.25, end.y + rnd.nextDouble() * 1.8,
							end.z + rnd.nextGaussian() * 0.25, 0, 0, 0);
					for (int i = 0; i < 5; i++) world.addParticle(ParticleTypes.LARGE_SMOKE, end.x, end.y + 0.8, end.z,
							rnd.nextGaussian() * 0.04, 0.03, rnd.nextGaussian() * 0.04);
				}
				int n = (int) Math.min(30, p.distanceTo(to) * 3);
				for (int i = 0; i <= n; i++) {
					Vec3d q = p.lerp(to, i / (double) Math.max(1, n)).add(0, 1.0, 0);
					world.addParticle(SHADOW_EDGE, q.x, q.y, q.z, 0, 0, 0);
				}
			}
			default -> {
			}
		}
	}

	public static void tick(MinecraftClient mc) {
		ClientWorld world = mc.world;
		if (world == null) {
			SHAPES.clear();
			return;
		}
		if (mc.isPaused()) return;
		for (Iterator<Shape> it = SHAPES.iterator(); it.hasNext(); ) {
			Shape s = it.next();
			if (++s.age > s.life || (s.follow != null && s.follow.isRemoved())) {
				it.remove();
				continue;
			}
			Vec3d p = s.follow != null ? s.follow.getPos() : s.pos;
			switch (s.kind) {
				case BOOM_GOLD -> boom(world, p, s.radius, s.age, GOLD);
				case BOOM_ARCANE -> boom(world, p, s.radius, s.age, BLUE);
				case FLASH -> {
					if (s.age == 1) firework(world, p.add(0, 1, 0), 0xFFE27A, 0xFFFFFF);
				}
			}
		}
		for (PlayerEntity player : world.getPlayers()) flagged(world, player);
	}

	private static void flagged(ClientWorld w, PlayerEntity pl) {
		long t = w.getTime();
		Random r = w.random;
		if (EntityFlags.clientHas(pl, "mirror_shield")) {
			Vec3d look = pl.getRotationVec(1f);
			Vec3d c = pl.getEyePos().add(look.multiply(1.4)).add(0, -0.35, 0);
			Vec3d side = new Vec3d(-look.z, 0, look.x);
			side = side.lengthSquared() < 1e-4 ? new Vec3d(1, 0, 0) : side.normalize();
			Vec3d up = side.crossProduct(look).normalize();
			double rad = 1.3 * pl.getScale();
			for (int i = 0; i < 10; i++) {
				double a = t * 0.15 + i * Math.PI * 2 / 10;
				Vec3d q = c.add(side.multiply(Math.cos(a) * rad)).add(up.multiply(Math.sin(a) * rad));
				w.addParticle(i % 2 == 0 ? ParticleTypes.END_ROD : ParticleTypes.WAX_ON, q.x, q.y, q.z, 0, 0, 0);
			}
			if (t % 3 == 0) w.addParticle(ParticleTypes.ENCHANT, c.x + r.nextGaussian() * 0.4, c.y + r.nextGaussian() * 0.4, c.z + r.nextGaussian() * 0.4, 0, 0, 0);
		}
		if (EntityFlags.clientHas(pl, "gold_ward") && t % 2 == 0) {
			double a = t * 0.2;
			for (int i = 0; i < 3; i++) {
				double b = a + i * Math.PI * 2 / 3;
				w.addParticle(GOLD, pl.getX() + Math.cos(b) * 0.9, pl.getY() + 0.2 + (t % 30) / 15.0, pl.getZ() + Math.sin(b) * 0.9, 0, 0, 0);
			}
		}
	}

	private static void firework(ClientWorld w, Vec3d at, int color, int fade) {
		w.addFireworkParticle(at.x, at.y, at.z, 0, 0, 0, List.of(new FireworkExplosionComponent(
				FireworkExplosionComponent.Type.SMALL_BALL, IntList.of(color), IntList.of(fade), false, false)));
	}

	private static void boom(ClientWorld w, Vec3d p, float radius, int age, ParticleEffect color) {
		Random r = w.random;
		if (age == 1) {
			w.addParticle(radius > 2 ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION, p.x, p.y + 0.5, p.z, 0, 0, 0);
			for (int i = 0; i < 24; i++) w.addParticle(color, p.x, p.y + 0.5, p.z, r.nextGaussian() * 0.3 * radius, r.nextGaussian() * 0.2 * radius, r.nextGaussian() * 0.3 * radius);
		}
		if (age <= 4) {
			double rad = radius * age / 4f;
			for (int i = 0; i < 16; i++) {
				double a = i * Math.PI * 2 / 16 + r.nextDouble() * 0.1;
				w.addParticle(color, p.x + Math.cos(a) * rad, p.y + 0.2, p.z + Math.sin(a) * rad, 0, 0.01, 0);
			}
		}
	}
}
