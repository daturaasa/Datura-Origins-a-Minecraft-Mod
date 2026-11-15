package com.datura.origins.fabric.power.ability;

import com.datura.origins.fabric.network.FxPayload;
import com.datura.origins.fabric.network.Networking;
import com.datura.origins.fabric.power.Ability;
import com.datura.origins.fabric.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public final class ShadowStepAbility implements Ability {
	private final double range;
	private final int invis;

	public ShadowStepAbility(JsonObject json) {
		this.range = JsonParams.num(json, "range", 10f);
		this.invis = JsonParams.integer(json, "invis", 60);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		Vec3d eye = p.getEyePos(), look = p.getRotationVec(1f), end = eye.add(look.multiply(range));
		var hit = p.getWorld().raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
		if (hit.getType() == HitResult.Type.MISS) {
			p.sendMessage(net.minecraft.text.Text.translatable("datura_origins.message.no_surface").formatted(net.minecraft.util.Formatting.GRAY), true);
			return false;
		}
		Vec3d dest;
		if (hit.getSide() == net.minecraft.util.math.Direction.UP) dest = hit.getPos();
		else {
			Vec3d n = Vec3d.of(hit.getSide().getVector());
			dest = hit.getPos().add(n.multiply(0.45));
			var down = p.getWorld().raycast(new RaycastContext(dest, dest.add(0, -5, 0), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
			if (down.getType() == HitResult.Type.MISS) {
				p.sendMessage(net.minecraft.text.Text.translatable("datura_origins.message.no_surface").formatted(net.minecraft.util.Formatting.GRAY), true);
				return false;
			}
			dest = down.getPos();
		}
		Box box = p.getDimensions(p.getPose()).getBoxAt(dest);
		if (!p.getWorld().isSpaceEmpty(p, box) || p.getWorld().containsFluid(box) && p.getWorld().getBlockState(net.minecraft.util.math.BlockPos.ofFloored(dest)).isOf(net.minecraft.block.Blocks.LAVA)) {
			p.sendMessage(net.minecraft.text.Text.translatable("datura_origins.message.no_surface").formatted(net.minecraft.util.Formatting.GRAY), true);
			return false;
		}
		Vec3d from = p.getPos();
		Networking.sendNear(p.getServerWorld(), from, 64, new FxPayload("shadow_step", from.x, from.y, from.z,
				(float) dest.x, (float) dest.y, (float) dest.z));
		p.requestTeleport(dest.x, dest.y, dest.z);
		p.fallDistance = 0;
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, invis, 0, false, false, true));
		p.getWorld().playSound(null, from.x, from.y, from.z, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.6f, 0.6f);
		p.getWorld().playSound(null, dest.x, dest.y, dest.z, SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.PLAYERS, 0.8f, 0.7f);
		return true;
	}
}
