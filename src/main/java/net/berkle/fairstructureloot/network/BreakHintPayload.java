package net.berkle.fairstructureloot.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import net.berkle.fairstructureloot.FairStructureLootMain;

/** S2C: show the fair-loot chest break hint toast. */
public record BreakHintPayload() implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<BreakHintPayload> TYPE = new CustomPacketPayload.Type<>(
		Identifier.fromNamespaceAndPath(FairStructureLootMain.MOD_ID, "break_hint")
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, BreakHintPayload> CODEC = StreamCodec.unit(new BreakHintPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
