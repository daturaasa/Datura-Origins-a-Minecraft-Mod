package com.datura.origins.fabric.network;

import com.datura.origins.fabric.DaturaOriginsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record MaturityPayload(int points) implements CustomPayload {
	public static final CustomPayload.Id<MaturityPayload> ID = new CustomPayload.Id<>(DaturaOriginsMod.id("maturity"));
	public static final PacketCodec<RegistryByteBuf, MaturityPayload> CODEC =
			PacketCodec.tuple(PacketCodecs.VAR_INT, MaturityPayload::points, MaturityPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
