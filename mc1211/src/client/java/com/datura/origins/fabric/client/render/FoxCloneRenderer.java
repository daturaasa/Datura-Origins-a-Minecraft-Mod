package com.datura.origins.fabric.client.render;

import com.datura.origins.fabric.entity.FoxCloneEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.ArmorEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;

public class FoxCloneRenderer extends BipedEntityRenderer<FoxCloneEntity, PlayerEntityModel<FoxCloneEntity>> {
	public FoxCloneRenderer(EntityRendererFactory.Context ctx) {
		super(ctx, new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER_SLIM), true), 0.4f);
		addFeature(new ArmorFeatureRenderer<>(this,
				new ArmorEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER_SLIM_INNER_ARMOR)),
				new ArmorEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER_SLIM_OUTER_ARMOR)), ctx.getModelManager()));
	}

	@Override
	public Identifier getTexture(FoxCloneEntity e) {
		var net = MinecraftClient.getInstance().getNetworkHandler();
		if (net != null && e.ownerUuid().isPresent()) {
			PlayerListEntry entry = net.getPlayerListEntry(e.ownerUuid().get());
			if (entry != null) return entry.getSkinTextures().texture();
		}
		return DefaultSkinHelper.getTexture();
	}
}
