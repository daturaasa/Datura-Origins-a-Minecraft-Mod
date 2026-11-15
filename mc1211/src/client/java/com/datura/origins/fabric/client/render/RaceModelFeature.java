package com.datura.origins.fabric.client.render;

import com.datura.origins.fabric.client.model.ModelAnimator;
import com.datura.origins.fabric.client.model.PartModel;
import com.datura.origins.fabric.client.model.RaceModels;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RaceModelFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
	private static final float[][] CANDIDATES = {
			{0, 0, 0}, {-12, 0, 0}, {-25, 0, 0}, {-40, 0, 0}, {-55, 0, 0}, {-70, 0, 0},
			{-25, 0, -10}, {-40, 0, -20}, {-55, 0, -30},
			{-20, 35, 0}, {-20, -35, 0}, {-35, 60, -10}, {-35, -60, -10},
			{-70, 0, -30}, {-80, 0, -45}
	};
	private static final Map<Integer, float[]> MOTION = new HashMap<>();
	private static final Map<Integer, float[]> FIT = new HashMap<>();
	private static final Map<Integer, float[]> WAVE = new HashMap<>();

	public RaceModelFeature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
		super(context);
	}

	public static void forget(int entityId) {
		MOTION.remove(entityId);
		FIT.remove(entityId);
		WAVE.remove(entityId);
		ModelAnimator.forget(entityId);
	}

	public static void clear() {
		MOTION.clear();
		FIT.clear();
		WAVE.clear();
		ModelAnimator.clear();
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player,
			float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
		if (player.isInvisible()) return;
		for (Identifier id : RaceModels.modelsOf(player)) {
			renderModel(id, matrices, vertexConsumers, light, player, limbAngle, limbDistance, tickDelta, animationProgress);
		}
	}

	private void renderModel(Identifier id, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
			AbstractClientPlayerEntity player, float limbAngle, float limbDistance, float tickDelta, float animationProgress) {
		PartModel model = PartModel.get(id);
		if (model.isEmpty() || model.liftBone() == null) return;

		Identifier texture = Identifier.of(id.getNamespace(), "textures/entity/part/" + id.getPath() + ".png");
		int overlay = LivingEntityRenderer.getOverlay(player, 0.0F);
		boolean preview = RaceModels.inPreview(player);
		if (model.physics()) {
			float[] motion = preview ? new float[7] : motion(player, tickDelta);
			pose(model, player, limbAngle, limbDistance, animationProgress, motion);
			if (!preview) fit(model, player, tickDelta);
		} else {
			ModelAnimator.pose(id, model, player, animationProgress, preview);
		}

		matrices.push();
		if (model.onHead()) getContextModel().head.rotate(matrices);
		else getContextModel().body.rotate(matrices);
		PartModel.toModelSpace(matrices);
		model.render(matrices, vertexConsumers.getBuffer(layer(player, texture)), light, overlay, color(player));
		if (model.glows()) {
			model.renderGlow(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(texture)), overlay,
					model.pulses() ? pulse(animationProgress) : color(player));
		}
		matrices.pop();
	}

	private static RenderLayer layer(AbstractClientPlayerEntity player, Identifier texture) {
		return RaceModels.isGhost(player) ? RenderLayer.getItemEntityTranslucentCull(texture) : RenderLayer.getEntityCutoutNoCull(texture);
	}

	private static int pulse(float animationProgress) {
		float wave = 0.5F - 0.5F * MathHelper.cos(animationProgress / 20.0F * MathHelper.TAU / 4.5F);
		int alpha = Math.round(255.0F * (0.18F + 0.62F * wave * wave * (3.0F - 2.0F * wave)));
		return (alpha << 24) | 0xFFFFFF;
	}

	private static int color(AbstractClientPlayerEntity player) {
		return RaceModels.isGhost(player) ? 0x99FFFFFF : 0xFFFFFFFF;
	}

	private static void pose(PartModel model, AbstractClientPlayerEntity player, float limbAngle, float limbDistance,
			float animationProgress, float[] motion) {
		model.resetPose();
		List<PartModel.Bone> chain = model.chain();
		float speed = Math.min(0.35F, motion[6]);
		if (model.hasAnimation("idle") || model.hasAnimation("walk")) {
			float walk = MathHelper.clamp(limbDistance * 1.5F, 0.0F, 1.0F);
			model.animate("idle", animationProgress / 20.0F, 1.0F - walk);
			model.animate("walk", limbAngle / 9.4248F, walk);
		} else {
			float stretch = MathHelper.clamp(speed * 3.2F, 0.0F, 1.0F);
			float[] wave = WAVE.computeIfAbsent(player.getId(), k -> new float[]{0.0F, animationProgress});
			float elapsed = MathHelper.clamp(animationProgress - wave[1], 0.0F, 4.0F);
			wave[1] = animationProgress;
			wave[0] = (wave[0] + elapsed * (0.09F + speed * 0.6F)) % (MathHelper.TAU * 64.0F);
			float phase = wave[0];
			float amplitude = 5.0F + 16.0F * speed;
			for (int i = 0; i < chain.size(); i++) {
				float swing = MathHelper.sin(phase - i * 0.8F) * amplitude * (0.6F + i * 0.45F) * (1.0F - stretch * 0.4F);
				chain.get(i).add(i == 0 ? 18.0F * stretch : swing * 0.15F, swing, 0.0F);
			}
		}
		PartModel.Bone root = model.liftBone();
		root.add(motion[0] * 10.0F + motion[4] * 10.0F, motion[2] * 12.0F + motion[5] * 15.0F, 0.0F);
		if (player.isSleeping()) root.add(10.0F, 0.0F, 0.0F);
	}

	private static float[] motion(AbstractClientPlayerEntity player, float tickDelta) {
		float[] st = MOTION.computeIfAbsent(player.getId(), k -> new float[16]);
		if (st[12] != player.age) {
			System.arraycopy(st, 0, st, 6, 6);
			st[15] = st[13];
			st[12] = player.age;
			step(player, st);
		}
		float[] out = new float[7];
		for (int i = 0; i < 6; i++) out[i] = MathHelper.lerp(tickDelta, st[6 + i], st[i]);
		out[6] = MathHelper.lerp(tickDelta, st[15], st[13]);
		return out;
	}

	private static void step(AbstractClientPlayerEntity e, float[] st) {
		double dx = e.getX() - e.prevX, dz = e.getZ() - e.prevZ, dy = e.getY() - e.prevY;
		st[13] += ((float) Math.sqrt(dx * dx + dz * dz) - st[13]) * 0.35F;
		double yaw = Math.toRadians(e.bodyYaw);
		double fwd = -dx * Math.sin(yaw) + dz * Math.cos(yaw), side = dx * Math.cos(yaw) + dz * Math.sin(yaw);
		float tx = (float) MathHelper.clamp(fwd * 2.4, -0.4, 0.9), ty = (float) MathHelper.clamp(side * 2.0, -0.6, 0.6);
		st[1] += ((tx - st[0]) * 0.3F - st[1] * 0.2F) * 0.5F;
		st[0] += st[1] * 0.5F;
		st[3] += ((ty - st[2]) * 0.3F - st[3] * 0.2F) * 0.5F;
		st[2] += st[3] * 0.5F;
		float tb = (float) MathHelper.clamp(-dy * 2.0, -0.6, 0.6);
		st[4] += (tb - st[4]) * 0.25F;
		float turn = MathHelper.wrapDegrees(e.bodyYaw - e.prevBodyYaw) * 0.02F;
		st[5] += (MathHelper.clamp(turn, -0.5F, 0.5F) - st[5]) * 0.2F;
	}

	private void fit(PartModel model, AbstractClientPlayerEntity player, float tickDelta) {
		float[] fit = FIT.computeIfAbsent(player.getId(), k -> new float[]{-1, 0, 0, 0, 0, 0, 0});
		boolean free = player.isSleeping() || player.isSwimming() || player.isFallFlying() || player.isSpectator();
		if (fit[0] != player.age) {
			fit[0] = player.age;
			float[] target = free ? CANDIDATES[0] : search(model, player, tickDelta);
			fit[4] = target[0];
			fit[5] = target[1];
			fit[6] = target[2];
		}
		float frame = MinecraftClient.getInstance().getRenderTickCounter().getLastFrameDuration();
		for (int i = 1; i <= 3; i++) {
			float target = fit[i + 3];
			float rate = Math.abs(target) > Math.abs(fit[i]) ? 0.5F : 0.15F;
			fit[i] = MathHelper.lerp(1.0F - (float) Math.pow(1.0F - rate, Math.min(3.0F, frame)), fit[i], target);
		}
		apply(model, fit[1], fit[2], fit[3], 1.0F);
	}

	private static void apply(PartModel model, float lift, float yaw, float curl, float sign) {
		List<PartModel.Bone> chain = model.chain();
		chain.get(0).add(lift * sign, yaw * sign, 0.0F);
		for (int i = 1; i < chain.size(); i++) chain.get(i).add(curl * sign / (chain.size() - 1), 0.0F, 0.0F);
	}

	private float[] search(PartModel model, AbstractClientPlayerEntity player, float tickDelta) {
		World world = player.getWorld();
		MatrixStack base = worldMatrix(player, tickDelta);
		Vec3d origin = player.getLerpedPos(tickDelta);
		float[] best = CANDIDATES[0];
		int bestHits = Integer.MAX_VALUE;
		for (float[] candidate : CANDIDATES) {
			apply(model, candidate[0], candidate[1], candidate[2], 1.0F);
			int hits = collisions(model, base, origin, world, bestHits);
			apply(model, candidate[0], candidate[1], candidate[2], -1.0F);
			if (hits < bestHits) {
				bestHits = hits;
				best = candidate;
				if (hits == 0) break;
			}
		}
		return best;
	}

	private MatrixStack worldMatrix(AbstractClientPlayerEntity player, float tickDelta) {
		MatrixStack matrices = new MatrixStack();
		float scale = player.getScale();
		if (player.isInSneakingPose()) matrices.translate(0.0F, -0.125F * scale, 0.0F);
		matrices.scale(scale, scale, scale);
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - MathHelper.lerpAngleDegrees(tickDelta, player.prevBodyYaw, player.bodyYaw)));
		matrices.scale(-1.0F, -1.0F, 1.0F);
		matrices.scale(0.9375F, 0.9375F, 0.9375F);
		matrices.translate(0.0F, -1.501F, 0.0F);
		getContextModel().body.rotate(matrices);
		PartModel.toModelSpace(matrices);
		return matrices;
	}

	private static int collisions(PartModel model, MatrixStack base, Vec3d origin, World world, int limit) {
		int[] hits = {0};
		BlockPos.Mutable pos = new BlockPos.Mutable();
		model.collectPoints(base, (Vector3f v) -> {
			if (hits[0] >= limit) return;
			double x = origin.x + v.x, y = origin.y + v.y, z = origin.z + v.z;
			pos.set(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
			BlockState state = world.getBlockState(pos);
			if (state.isAir()) return;
			VoxelShape shape = state.getCollisionShape(world, pos);
			if (shape.isEmpty()) return;
			for (Box box : shape.getBoundingBoxes()) {
				if (box.contains(x - pos.getX(), y - pos.getY(), z - pos.getZ())) {
					hits[0]++;
					return;
				}
			}
		});
		return hits[0];
	}
}
