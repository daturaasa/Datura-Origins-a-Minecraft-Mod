package com.datura.origins.fabric.network;

import com.datura.origins.fabric.DaturaOriginsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record FireballPayload(int id, double x, double y, double z, double vx, double vy, double vz, int state)
		implements CustomPayload {
	public static final int FIZZLED = 0, FLYING = 1, EXPLODED = 2;

	public static final CustomPayload.Id<FireballPayload> ID = new CustomPayload.Id<>(DaturaOriginsMod.id("fireball"));
	public static final PacketCodec<RegistryByteBuf, FireballPayload> CODEC = PacketCodec.of(
			(p, buf) -> {
				buf.writeVarInt(p.id);
				buf.writeDouble(p.x);
				buf.writeDouble(p.y);
				buf.writeDouble(p.z);
				buf.writeDouble(p.vx);
				buf.writeDouble(p.vy);
				buf.writeDouble(p.vz);
				buf.writeByte(p.state);
			},
			buf -> new FireballPayload(buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
					buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readByte()));

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
