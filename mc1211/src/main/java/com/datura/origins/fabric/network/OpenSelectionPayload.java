package com.datura.origins.fabric.network;

import com.datura.origins.DaturaOrigins;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenSelectionPayload() implements CustomPayload {
	public static final OpenSelectionPayload INSTANCE = new OpenSelectionPayload();
	public static final Id<OpenSelectionPayload> ID = new Id<>(Identifier.of(DaturaOrigins.MOD_ID, "open_selection"));
	public static final PacketCodec<ByteBuf, OpenSelectionPayload> CODEC = PacketCodec.unit(INSTANCE);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
