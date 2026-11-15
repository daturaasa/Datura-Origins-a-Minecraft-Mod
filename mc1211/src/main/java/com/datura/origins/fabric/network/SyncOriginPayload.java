package com.datura.origins.fabric.network;

import com.datura.origins.DaturaOrigins;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SyncOriginPayload(String race) implements CustomPayload {
	public static final Id<SyncOriginPayload> ID = new Id<>(Identifier.of(DaturaOrigins.MOD_ID, "sync_origin"));
	public static final PacketCodec<ByteBuf, SyncOriginPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, SyncOriginPayload::race,
			SyncOriginPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
