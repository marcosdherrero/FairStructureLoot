package net.berkle.fairstructureloot.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import net.berkle.fairstructureloot.FairStructureLootMain;

/** S2C: mark or clear a fair-loot container position on the client (chunk load / join / break). */
public record MarkFairLootChestPayload(
	Identifier dimension,
	BlockPos pos,
	boolean fairLoot,
	boolean perPlayerIndicator
) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<MarkFairLootChestPayload> TYPE = new CustomPacketPayload.Type<>(
		Identifier.fromNamespaceAndPath(FairStructureLootMain.MOD_ID, "mark_fair_loot_chest")
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, MarkFairLootChestPayload> CODEC = StreamCodec.composite(
		Identifier.STREAM_CODEC, MarkFairLootChestPayload::dimension,
		BlockPos.STREAM_CODEC, MarkFairLootChestPayload::pos,
		ByteBufCodecs.BOOL, MarkFairLootChestPayload::fairLoot,
		ByteBufCodecs.BOOL, MarkFairLootChestPayload::perPlayerIndicator,
		MarkFairLootChestPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
