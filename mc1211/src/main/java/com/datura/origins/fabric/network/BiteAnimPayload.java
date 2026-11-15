package com.datura.origins.fabric.network;

import com.datura.origins.fabric.DaturaOriginsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record BiteAnimPayload(int vampireId, int victimId, int durationTicks) implements CustomPayload {
	public static final CustomPayload.Id<BiteAnimPayload> ID = new CustomPayload.Id<>(DaturaOriginsMod.id("bite_anim"));
	public static final PacketCodec<RegistryByteBuf, BiteAnimPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, BiteAnimPayload::vampireId,
			PacketCodecs.VAR_INT, BiteAnimPayload::victimId,
			PacketCodecs.VAR_INT, BiteAnimPayload::durationTicks,
			BiteAnimPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
