package com.datura.origins.fabric.client.mixin;

import com.datura.origins.fabric.client.anim.AbilityAnimations;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class)
public abstract class BipedEntityModelMixin {
	@Shadow @Final public ModelPart head;
	@Shadow @Final public ModelPart hat;
	@Shadow @Final public ModelPart rightArm;
	@Shadow @Final public ModelPart leftArm;
	@Shadow @Final public ModelPart rightLeg;
	@Shadow @Final public ModelPart leftLeg;
	@Shadow @Final public ModelPart body;

	@Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
	private void datura$abilityPoses(LivingEntity entity, float limbAngle, float limbDistance, float animationProgress,
									 float headYaw, float headPitch, CallbackInfo ci) {
		float p = AbilityAnimations.progress(entity, "bite", animationProgress);
		if (p >= 0f) poseBite(p, animationProgress);

		float v = AbilityAnimations.progress(entity, "bitten", animationProgress);
		if (v >= 0f) poseBitten(v, animationProgress);

		float c = AbilityAnimations.progress(entity, "cast", animationProgress);
		if (c >= 0f) poseCast(c);

		float reach = AbilityAnimations.progress(entity, "reach", animationProgress);
		if (reach >= 0f) poseReach(reach, animationProgress);

		float summon = AbilityAnimations.progress(entity, "summon_up", animationProgress);
		if (summon >= 0f) poseSummonUp(summon);

		float thrown = AbilityAnimations.progress(entity, "throw", animationProgress);
		if (thrown >= 0f) poseThrow(thrown);

		float stomp = AbilityAnimations.progress(entity, "stomp", animationProgress);
		if (stomp >= 0f) poseStomp(stomp);

		float crouch = AbilityAnimations.progress(entity, "crouch", animationProgress);
		if (crouch >= 0f) poseCrouch(crouch);

		float ram = AbilityAnimations.progress(entity, "ram", animationProgress);
		if (ram >= 0f) poseRam(ram);
	}

	private void poseStomp(float p) {
		float raise = AbilityAnimations.window(p, 0.00f, 0.42f, 0.42f, 0.54f);
		float slam = AbilityAnimations.window(p, 0.48f, 0.56f, 0.66f, 1.00f);
		rightLeg.pitch = MathHelper.lerp(raise, rightLeg.pitch, -1.15f);
		body.pitch = MathHelper.lerp(Math.max(raise, slam), body.pitch, raise * -0.12f + slam * 0.32f);
		float armPitch = raise * -0.55f + slam * -0.95f;
		float armRoll = raise * 0.7f + slam * 0.25f;
		float w = Math.max(raise, slam);
		rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, armPitch);
		rightArm.roll = MathHelper.lerp(w, rightArm.roll, armRoll);
		leftArm.pitch = MathHelper.lerp(w, leftArm.pitch, armPitch);
		leftArm.roll = MathHelper.lerp(w, leftArm.roll, -armRoll);
		head.pitch = MathHelper.lerp(slam, head.pitch, head.pitch + 0.25f);
		hat.copyTransform(head);
	}

	private void poseCrouch(float p) {
		float w = AbilityAnimations.window(p, 0.00f, 0.15f, 0.97f, 1.00f);
		body.pitch = MathHelper.lerp(w, body.pitch, 0.3f);
		rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, 0.55f);
		leftArm.pitch = MathHelper.lerp(w, leftArm.pitch, 0.55f);
		head.pitch = MathHelper.lerp(w, head.pitch, head.pitch * 0.5f - 0.1f);
		hat.copyTransform(head);
	}

	private void poseRam(float p) {
		float w = AbilityAnimations.window(p, 0.00f, 0.15f, 0.55f, 1.00f);
		head.pitch = MathHelper.lerp(w, head.pitch, 0.75f);
		hat.copyTransform(head);
		body.pitch = MathHelper.lerp(w, body.pitch, 0.4f);
		rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, 0.8f);
		leftArm.pitch = MathHelper.lerp(w, leftArm.pitch, 0.8f);
		rightArm.roll = MathHelper.lerp(w, rightArm.roll, 0.15f);
		leftArm.roll = MathHelper.lerp(w, leftArm.roll, -0.15f);
	}

	private void poseReach(float p, float t) {
		float w = AbilityAnimations.window(p, 0.00f, 0.08f, 0.94f, 1.00f);
		float shake = (float) Math.sin(t * 1.3f) * 0.025f;
		leftArm.pitch = MathHelper.lerp(w, leftArm.pitch, head.pitch - 1.5f + shake);
		leftArm.yaw = MathHelper.lerp(w, leftArm.yaw, head.yaw + 0.1f);
		leftArm.roll = MathHelper.lerp(w, leftArm.roll, 0f);
		rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, 0.25f);
		rightArm.roll = MathHelper.lerp(w, rightArm.roll, 0.15f);
		body.yaw = MathHelper.lerp(w, body.yaw, -0.12f);
	}

	private void poseSummonUp(float p) {
		float w = AbilityAnimations.window(p, 0.00f, 0.25f, 0.70f, 1.00f);
		rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, -2.9f);
		leftArm.pitch = MathHelper.lerp(w, leftArm.pitch, -2.9f);
		rightArm.roll = MathHelper.lerp(w, rightArm.roll, 0.35f);
		leftArm.roll = MathHelper.lerp(w, leftArm.roll, -0.35f);
		head.pitch = MathHelper.lerp(w, head.pitch, -0.35f);
		hat.copyTransform(head);
	}

	private void poseThrow(float p) {
		float back = AbilityAnimations.window(p, 0.00f, 0.25f, 0.30f, 0.45f);
		float fwd = AbilityAnimations.window(p, 0.30f, 0.50f, 0.70f, 1.00f);
		rightArm.pitch = MathHelper.lerp(Math.max(back, fwd), rightArm.pitch, back > fwd ? -2.9f : -1.2f);
		rightArm.roll = MathHelper.lerp(back, rightArm.roll, -0.2f);
		body.yaw = MathHelper.lerp(Math.max(back, fwd), body.yaw, back > fwd ? 0.35f : -0.25f);
		leftArm.pitch = MathHelper.lerp(fwd, leftArm.pitch, -0.6f);
	}


	private void poseBite(float p, float t) {
		float arms = AbilityAnimations.window(p, 0.00f, 0.18f, 0.82f, 1.00f);
		float grip = AbilityAnimations.window(p, 0.12f, 0.30f, 0.82f, 1.00f);
		float bite = AbilityAnimations.window(p, 0.26f, 0.34f, 0.78f, 0.92f);
		float gulp = (float) Math.sin(t * 0.95f) * bite;

		float pitch = MathHelper.lerp(grip, -1.55f, -1.12f) + gulp * 0.03f;
		float yaw = MathHelper.lerp(grip, -0.12f, -0.62f) + gulp * 0.025f;
		float roll = 0.08f * grip;

		rightArm.pitch = MathHelper.lerp(arms, rightArm.pitch, pitch);
		rightArm.yaw = MathHelper.lerp(arms, rightArm.yaw, yaw);
		rightArm.roll = MathHelper.lerp(arms, rightArm.roll, roll);
		leftArm.pitch = MathHelper.lerp(arms, leftArm.pitch, pitch);
		leftArm.yaw = MathHelper.lerp(arms, leftArm.yaw, -yaw);
		leftArm.roll = MathHelper.lerp(arms, leftArm.roll, -roll);

		head.pitch = MathHelper.lerp(bite, head.pitch, 0.52f + gulp * 0.07f);
		head.yaw = MathHelper.lerp(bite, head.yaw, 0.32f);
		head.roll = MathHelper.lerp(bite, head.roll, 0.18f);
		hat.copyTransform(head);
	}

	private void poseBitten(float p, float t) {
		float held = AbilityAnimations.window(p, 0.05f, 0.25f, 0.85f, 1.00f);
		float tremble = (float) Math.sin(t * 2.3f) * 0.05f * held;

		head.roll = MathHelper.lerp(held, head.roll, -0.48f);
		head.pitch = MathHelper.lerp(held, head.pitch, -0.28f);
		head.yaw = MathHelper.lerp(held, head.yaw, -0.22f);
		hat.copyTransform(head);

		rightArm.pitch = MathHelper.lerp(held, rightArm.pitch, 0.18f + tremble);
		rightArm.roll = MathHelper.lerp(held, rightArm.roll, 0.14f);
		leftArm.pitch = MathHelper.lerp(held, leftArm.pitch, 0.12f - tremble);
		leftArm.roll = MathHelper.lerp(held, leftArm.roll, -0.14f);
	}

	private void poseCast(float p) {
		float w = AbilityAnimations.window(p, 0.00f, 0.22f, 0.45f, 1.00f);
		rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, head.pitch - 1.45f);
		rightArm.yaw = MathHelper.lerp(w, rightArm.yaw, head.yaw - 0.12f);
		rightArm.roll = MathHelper.lerp(w, rightArm.roll, 0.0f);
		leftArm.pitch = MathHelper.lerp(w * 0.6f, leftArm.pitch, 0.35f);
		leftArm.roll = MathHelper.lerp(w * 0.6f, leftArm.roll, -0.25f);
	}
}
