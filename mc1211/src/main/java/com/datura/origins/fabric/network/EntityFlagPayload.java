package com.datura.origins.fabric.network;

import com.datura.origins.fabric.DaturaOriginsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record EntityFlagPayload(int entityId, String flag, boolean value) implements CustomPayload {
	public static final CustomPayload.Id<EntityFlagPayload> ID = new CustomPayload.Id<>(DaturaOriginsMod.id("entity_flag"));
	public static final PacketCodec<RegistryByteBuf, EntityFlagPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, EntityFlagPayload::entityId,
			PacketCodecs.STRING, EntityFlagPayload::flag,
			PacketCodecs.BOOL, EntityFlagPayload::value,
			EntityFlagPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
