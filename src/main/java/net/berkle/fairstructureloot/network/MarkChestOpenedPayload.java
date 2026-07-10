package net.berkle.fairstructureloot.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import net.berkle.fairstructureloot.FairStructureLootMain;

/** S2C: a container was opened by this player — HUD indicator switches to black. */
public record MarkChestOpenedPayload(Identifier dimension, BlockPos pos) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<MarkChestOpenedPayload> TYPE = new CustomPacketPayload.Type<>(
		Identifier.fromNamespaceAndPath(FairStructureLootMain.MOD_ID, "mark_chest_opened")
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, MarkChestOpenedPayload> CODEC = StreamCodec.composite(
		Identifier.STREAM_CODEC, MarkChestOpenedPayload::dimension,
		BlockPos.STREAM_CODEC, MarkChestOpenedPayload::pos,
		MarkChestOpenedPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
