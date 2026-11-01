package com.workshoputilities.network;

import com.workshoputilities.WorkshopUtilitiesMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Client -> server: pemain menekan tombol "buka backpack yang dipakai" (tanpa data tambahan). */
public record OpenBackpackPayload() implements CustomPayload {
	public static final CustomPayload.Id<OpenBackpackPayload> ID =
			new CustomPayload.Id<>(Identifier.of(WorkshopUtilitiesMod.MOD_ID, "open_backpack"));
	public static final PacketCodec<RegistryByteBuf, OpenBackpackPayload> CODEC =
			PacketCodec.unit(new OpenBackpackPayload());

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
