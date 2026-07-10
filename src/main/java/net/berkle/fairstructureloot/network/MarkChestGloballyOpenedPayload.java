package net.berkle.fairstructureloot.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import net.berkle.fairstructureloot.FairStructureLootMain;

/** S2C: a shared-roll container was opened by someone else — HUD shows orange until you open it. */
public record MarkChestGloballyOpenedPayload(Identifier dimension, BlockPos pos) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<MarkChestGloballyOpenedPayload> TYPE = new CustomPacketPayload.Type<>(
		Identifier.fromNamespaceAndPath(FairStructureLootMain.MOD_ID, "mark_chest_globally_opened")
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, MarkChestGloballyOpenedPayload> CODEC = StreamCodec.composite(
		Identifier.STREAM_CODEC, MarkChestGloballyOpenedPayload::dimension,
		BlockPos.STREAM_CODEC, MarkChestGloballyOpenedPayload::pos,
		MarkChestGloballyOpenedPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
