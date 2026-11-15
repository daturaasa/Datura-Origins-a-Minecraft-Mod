package com.datura.origins.fabric.network;

import com.datura.origins.DaturaOrigins;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ChooseOriginPayload(String race) implements CustomPayload {
	public static final Id<ChooseOriginPayload> ID = new Id<>(Identifier.of(DaturaOrigins.MOD_ID, "choose_origin"));
	public static final PacketCodec<ByteBuf, ChooseOriginPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, ChooseOriginPayload::race,
			ChooseOriginPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
