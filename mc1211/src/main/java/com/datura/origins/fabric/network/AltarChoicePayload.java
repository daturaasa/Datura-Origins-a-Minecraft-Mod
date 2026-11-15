package com.datura.origins.fabric.network;

import com.datura.origins.fabric.DaturaOriginsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record AltarChoicePayload(int option) implements CustomPayload {
	public static final CustomPayload.Id<AltarChoicePayload> ID = new CustomPayload.Id<>(DaturaOriginsMod.id("altar_choice"));
	public static final PacketCodec<RegistryByteBuf, AltarChoicePayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, AltarChoicePayload::option,
			AltarChoicePayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
