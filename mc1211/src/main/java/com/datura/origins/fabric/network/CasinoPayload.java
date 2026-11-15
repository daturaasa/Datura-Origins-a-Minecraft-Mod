package com.datura.origins.fabric.network;

import com.datura.origins.fabric.DaturaOriginsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record CasinoPayload(int multiplier, int ticks) implements CustomPayload {
	public static final CustomPayload.Id<CasinoPayload> ID = new CustomPayload.Id<>(DaturaOriginsMod.id("casino"));
	public static final PacketCodec<RegistryByteBuf, CasinoPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, CasinoPayload::multiplier,
			PacketCodecs.VAR_INT, CasinoPayload::ticks,
			CasinoPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
