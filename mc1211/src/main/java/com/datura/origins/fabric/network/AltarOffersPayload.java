package com.datura.origins.fabric.network;

import com.datura.origins.fabric.DaturaOriginsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.List;

public record AltarOffersPayload(List<String> enchantments, List<Integer> levels, int seed) implements CustomPayload {
	public static final CustomPayload.Id<AltarOffersPayload> ID = new CustomPayload.Id<>(DaturaOriginsMod.id("altar_offers"));
	public static final PacketCodec<RegistryByteBuf, AltarOffersPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING.collect(PacketCodecs.toList()), AltarOffersPayload::enchantments,
			PacketCodecs.VAR_INT.collect(PacketCodecs.toList()), AltarOffersPayload::levels,
			PacketCodecs.VAR_INT, AltarOffersPayload::seed,
			AltarOffersPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
