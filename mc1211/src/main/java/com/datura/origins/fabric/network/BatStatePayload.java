package com.datura.origins.fabric.network;

import com.datura.origins.fabric.DaturaOriginsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record BatStatePayload(int entityId, boolean bat) implements CustomPayload {
	public static final CustomPayload.Id<BatStatePayload> ID = new CustomPayload.Id<>(DaturaOriginsMod.id("bat_state"));
	public static final PacketCodec<RegistryByteBuf, BatStatePayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, BatStatePayload::entityId,
			PacketCodecs.BOOL, BatStatePayload::bat,
			BatStatePayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
