package com.datura.origins.fabric.network;

import com.datura.origins.fabric.DaturaOriginsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record FxPayload(String name, double x, double y, double z, float a, float b, float c) implements CustomPayload {
	public static final CustomPayload.Id<FxPayload> ID = new CustomPayload.Id<>(DaturaOriginsMod.id("fx"));
	public static final PacketCodec<RegistryByteBuf, FxPayload> CODEC = PacketCodec.of((p, buf) -> {
		buf.writeString(p.name);
		buf.writeDouble(p.x);
		buf.writeDouble(p.y);
		buf.writeDouble(p.z);
		buf.writeFloat(p.a);
		buf.writeFloat(p.b);
		buf.writeFloat(p.c);
	}, buf -> new FxPayload(buf.readString(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
			buf.readFloat(), buf.readFloat(), buf.readFloat()));

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
