package com.datura.origins.fabric.network;

import com.datura.origins.fabric.DaturaOriginsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record AnimPayload(int entityId, String channel, int durationTicks) implements CustomPayload {
	public static final CustomPayload.Id<AnimPayload> ID = new CustomPayload.Id<>(DaturaOriginsMod.id("anim"));
	public static final PacketCodec<RegistryByteBuf, AnimPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, AnimPayload::entityId,
			PacketCodecs.STRING, AnimPayload::channel,
			PacketCodecs.VAR_INT, AnimPayload::durationTicks,
			AnimPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
