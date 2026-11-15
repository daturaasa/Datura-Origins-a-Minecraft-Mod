package com.datura.origins.fabric.mixin;

import com.datura.origins.fabric.power.types.MarkerPower;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.village.TradeOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin {
	@Inject(method = "prepareOffersFor", at = @At("TAIL"))
	private void datura$markup(PlayerEntity player, CallbackInfo ci) {
		VillagerEntity villager = (VillagerEntity) (Object) this;
		if (MarkerPower.has(player, "villager_markup")) {
			for (TradeOffer offer : villager.getOffers()) {
				offer.increaseSpecialPrice(Math.max(1, offer.getOriginalFirstBuyItem().getCount() / 4));
			}
		} else if (MarkerPower.has(player, "villager_discount")) {
			for (TradeOffer offer : villager.getOffers()) {
				offer.clearSpecialPrice();
				int cut = (int) Math.floor(0.3 * offer.getOriginalFirstBuyItem().getCount());
				if (cut > 0) offer.increaseSpecialPrice(-cut);
			}
		}
	}
}
