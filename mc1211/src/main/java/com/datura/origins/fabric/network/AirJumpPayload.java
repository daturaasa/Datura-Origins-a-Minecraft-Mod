package com.datura.origins.fabric.network;

import com.datura.origins.fabric.DaturaOriginsMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record AirJumpPayload() implements CustomPayload {
	public static final AirJumpPayload INSTANCE = new AirJumpPayload();
	public static final Id<AirJumpPayload> ID = new Id<>(DaturaOriginsMod.id("air_jump"));
	public static final PacketCodec<ByteBuf, AirJumpPayload> CODEC = PacketCodec.unit(INSTANCE);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
