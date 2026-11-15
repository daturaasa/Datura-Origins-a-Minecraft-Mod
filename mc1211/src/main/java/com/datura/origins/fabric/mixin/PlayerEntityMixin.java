package com.datura.origins.fabric.mixin;

import com.datura.origins.fabric.power.EntityFlags;
import com.datura.origins.fabric.power.ability.BatFormState;
import com.datura.origins.fabric.power.types.WingFlightPower;
import com.datura.origins.fabric.power.types.FoodMultiplierPower;
import com.datura.origins.fabric.power.types.LilyPadPower;
import com.datura.origins.fabric.power.types.MarkerPower;
import com.datura.origins.fabric.power.types.MaturityPower;
import com.datura.origins.fabric.power.types.XpMultiplierPower;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
	@Unique
	private static final String MERMAID = "mermaid";
	@Unique
	private static final String CURSE_IMMUNE = "curse_immune";

	@Inject(method = "getBaseDimensions", at = @At("HEAD"), cancellable = true, require = 0)
	private void datura$batDimensions(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
		if (BatFormState.isBat((PlayerEntity) (Object) this)) {
			cir.setReturnValue(EntityType.BAT.getDimensions());
		}
	}

	@ModifyVariable(method = "addExperience", at = @At("HEAD"), argsOnly = true, require = 0)
	private int datura$xp(int amount) {
		if (amount <= 0) return amount;
		float f = XpMultiplierPower.factor((PlayerEntity) (Object) this);
		return f == 1f ? amount : Math.max(1, Math.round(amount * f));
	}

	@ModifyVariable(method = "applyEnchantmentCosts", at = @At("HEAD"), argsOnly = true, require = 0)
	private int datura$cheapEnchant(int levels) {
		return levels > 1 && MarkerPower.has((PlayerEntity) (Object) this, "cheap_enchant") ? 1 : levels;
	}

	@Inject(method = "addExperience", at = @At("HEAD"), cancellable = true)
	private void datura$blockExperience(int amount, CallbackInfo ci) {
		if (MaturityPower.blocksExperience((PlayerEntity) (Object) this)) {
			MaturityPower.gainXp((PlayerEntity) (Object) this, amount);
			ci.cancel();
		}
	}

	@Inject(method = "addExperienceLevels", at = @At("HEAD"), cancellable = true)
	private void datura$blockLevels(int levels, CallbackInfo ci) {
		if (MaturityPower.blocksExperience((PlayerEntity) (Object) this)) ci.cancel();
	}

	@Unique
	private int datura$foodBefore;
	@Unique
	private float datura$saturationBefore;

	@Inject(method = "eatFood", at = @At("HEAD"), require = 0)
	private void datura$foodBefore(World world, ItemStack stack, FoodComponent food, CallbackInfoReturnable<ItemStack> cir) {
		HungerManager hunger = ((PlayerEntity) (Object) this).getHungerManager();
		datura$foodBefore = hunger.getFoodLevel();
		datura$saturationBefore = hunger.getSaturationLevel();
	}

	@Inject(method = "eatFood", at = @At("RETURN"), require = 0)
	private void datura$foodAfter(World world, ItemStack stack, FoodComponent food, CallbackInfoReturnable<ItemStack> cir) {
		PlayerEntity player = (PlayerEntity) (Object) this;
		float factor = FoodMultiplierPower.factor(player);
		if (factor == 1.0F) return;
		HungerManager hunger = player.getHungerManager();
		int gained = hunger.getFoodLevel() - datura$foodBefore;
		float saturation = hunger.getSaturationLevel() - datura$saturationBefore;
		if (gained > 0) hunger.setFoodLevel(datura$foodBefore + Math.max(1, Math.round(gained * factor)));
		if (saturation > 0) hunger.setSaturationLevel(Math.min(hunger.getFoodLevel(), datura$saturationBefore + saturation * factor));
	}

	@Shadow
	protected abstract boolean canChangeIntoPose(EntityPose pose);

	@Unique
	private int datura$stillTicks;
	@Unique
	private double datura$lastX;
	@Unique
	private double datura$lastZ;

	@Inject(method = "checkFallFlying", at = @At("HEAD"), cancellable = true)
	private void datura$fairyTakeOff(CallbackInfoReturnable<Boolean> cir) {
		PlayerEntity player = (PlayerEntity) (Object) this;
		if (player.isFallFlying() || !WingFlightPower.canKeepFlying(player) || WingFlightPower.of(player).isEmpty()) return;
		player.startFallFlying();
		cir.setReturnValue(true);
	}

	@Inject(method = "travel", at = @At("HEAD"))
	private void datura$lilyPad(Vec3d movement, CallbackInfo ci) {
		PlayerEntity player = (PlayerEntity) (Object) this;
		if (player.isTouchingWater() || player.getWorld().getFluidState(player.getBlockPos()).isIn(FluidTags.WATER)) {
			if (LilyPadPower.has(player)) LilyPadPower.support(player);
		}
	}

	@Inject(method = "updatePose", at = @At("HEAD"), cancellable = true)
	private void datura$mermaidPose(CallbackInfo ci) {
		PlayerEntity player = (PlayerEntity) (Object) this;
		boolean mermaid = player instanceof ServerPlayerEntity sp ? EntityFlags.has(sp, MERMAID) : EntityFlags.clientHas(player, MERMAID);
		if (!mermaid || player.isTouchingWater() || player.isFallFlying() || player.hasVehicle() || player.isSleeping()
				|| player.getAbilities().flying || player.isSpectator()) {
			datura$stillTicks = 0;
			return;
		}
		double dx = player.getX() - datura$lastX, dz = player.getZ() - datura$lastZ;
		datura$lastX = player.getX();
		datura$lastZ = player.getZ();
		if (dx * dx + dz * dz > 1.0E-4 || !player.isOnGround()) datura$stillTicks = 0;
		else datura$stillTicks++;
		boolean sit = datura$stillTicks >= 60 && canChangeIntoPose(EntityPose.STANDING);
		player.setPose(sit ? EntityPose.STANDING : EntityPose.SWIMMING);
		ci.cancel();
	}

	@Inject(method = "vanishCursedItems", at = @At("HEAD"), cancellable = true)
	private void datura$keepCursed(CallbackInfo ci) {
		if ((Object) this instanceof ServerPlayerEntity player && EntityFlags.has(player, CURSE_IMMUNE)) ci.cancel();
	}
}
