package com.datura.origins.fabric.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class SpellProjectileEntity extends ProjectileEntity {
	public static final int ARCANE = 0, MOON = 1, CAULDRON = 2, MANA = 3;
	private static final TrackedData<Integer> KIND = DataTracker.registerData(SpellProjectileEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Float> SIZE = DataTracker.registerData(SpellProjectileEntity.class, TrackedDataHandlerRegistry.FLOAT);
		private static final Map<Integer, BiConsumer<SpellProjectileEntity, Vec3d>> IMPACT = new HashMap<>();
	private static final Map<Integer, BiConsumer<SpellProjectileEntity, Entity>> HIT_ENTITY = new HashMap<>();

	private double gravity = 0.0, drag = 1.0;
	private int maxAge = 100;
	public float power = 1f;
		public boolean scripted = false;

	public SpellProjectileEntity(EntityType<? extends SpellProjectileEntity> type, World world) {
		super(type, world);
	}

	public SpellProjectileEntity(World world, LivingEntity owner, int kind, double gravity, double drag, int maxAge) {
		this(ModEntities.SPELL_PROJECTILE, world);
		setOwner(owner);
		this.dataTracker.set(KIND, kind);
		this.gravity = gravity;
		this.drag = drag;
		this.maxAge = maxAge;
		Vec3d eye = owner.getEyePos().add(owner.getRotationVec(1f).multiply(0.8));
		setPosition(eye.x, eye.y - 0.3, eye.z);
	}

	public static void onImpact(int kind, BiConsumer<SpellProjectileEntity, Vec3d> fn) {
		IMPACT.put(kind, fn);
	}

	public static void onEntityHit(int kind, BiConsumer<SpellProjectileEntity, Entity> fn) {
		HIT_ENTITY.put(kind, fn);
	}

	public int kind() {
		return dataTracker.get(KIND);
	}

	public float size() {
		return dataTracker.get(SIZE);
	}

	public void setSize(float s) {
		dataTracker.set(SIZE, s);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(KIND, 0);
		builder.add(SIZE, 1f);
	}

	@Override
	public void tick() {
		super.tick();
		if (!getWorld().isClient && scripted) {
			if (age > 600) discard();
		} else if (!getWorld().isClient) {
			HitResult hit = ProjectileUtil.getCollision(this, this::canHit);
			if (hit.getType() != HitResult.Type.MISS) {
				impact(hit);
				return;
			}
			if (age > maxAge) {
				impact(null);
				return;
			}
		}
		Vec3d v = getVelocity();
		setPosition(getX() + v.x, getY() + v.y, getZ() + v.z);
		setVelocity(v.multiply(drag).add(0, -gravity, 0));
		ProjectileUtil.setRotationFromVelocity(this, 0.4f);
	}

	private void impact(HitResult hit) {
		Vec3d at = hit != null ? hit.getPos() : getPos();
		if (hit instanceof EntityHitResult eh) {
			BiConsumer<SpellProjectileEntity, Entity> fn = HIT_ENTITY.get(kind());
			if (fn != null) fn.accept(this, eh.getEntity());
		}
		BiConsumer<SpellProjectileEntity, Vec3d> fn = IMPACT.get(kind());
		if (fn != null && getWorld() instanceof ServerWorld) fn.accept(this, at);
		discard();
	}

	@Override
	protected boolean canHit(Entity entity) {
		return super.canHit(entity) && entity != getOwner();
	}

	@Override
	public boolean shouldRender(double distance) {
		return distance < 128 * 128;
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt("Kind", kind());
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		dataTracker.set(KIND, nbt.getInt("Kind"));
		discard();
	}
}
