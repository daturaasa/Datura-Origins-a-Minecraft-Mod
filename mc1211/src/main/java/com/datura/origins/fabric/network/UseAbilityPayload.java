package com.datura.origins.fabric.network;

import com.datura.origins.fabric.DaturaOriginsMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record UseAbilityPayload() implements CustomPayload {
	public static final UseAbilityPayload INSTANCE = new UseAbilityPayload();
	public static final Id<UseAbilityPayload> ID = new Id<>(DaturaOriginsMod.id("use_ability"));
	public static final PacketCodec<ByteBuf, UseAbilityPayload> CODEC = PacketCodec.unit(INSTANCE);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
