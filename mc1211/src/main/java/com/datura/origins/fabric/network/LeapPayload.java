package com.datura.origins.fabric.network;

import com.datura.origins.fabric.DaturaOriginsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record LeapPayload(boolean released, int charge) implements CustomPayload {
	public static final CustomPayload.Id<LeapPayload> ID = new CustomPayload.Id<>(DaturaOriginsMod.id("leap"));
	public static final PacketCodec<RegistryByteBuf, LeapPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.BOOL, LeapPayload::released,
			PacketCodecs.VAR_INT, LeapPayload::charge,
			LeapPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
