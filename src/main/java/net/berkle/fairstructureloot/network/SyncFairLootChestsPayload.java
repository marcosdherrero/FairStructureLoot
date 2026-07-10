package net.berkle.fairstructureloot.network;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import net.berkle.fairstructureloot.FairStructureLootMain;

/** S2C: batch fair-loot container markers for join sync. */
public record SyncFairLootChestsPayload(List<Entry> entries) implements CustomPacketPayload {

	public record Entry(Identifier dimension, BlockPos pos, boolean perPlayerIndicator) {
	}

	public static final CustomPacketPayload.Type<SyncFairLootChestsPayload> TYPE = new CustomPacketPayload.Type<>(
		Identifier.fromNamespaceAndPath(FairStructureLootMain.MOD_ID, "sync_fair_loot_chests")
	);

	private static final StreamCodec<RegistryFriendlyByteBuf, Entry> ENTRY_CODEC = StreamCodec.composite(
		Identifier.STREAM_CODEC, Entry::dimension,
		BlockPos.STREAM_CODEC, Entry::pos,
		ByteBufCodecs.BOOL, Entry::perPlayerIndicator,
		Entry::new
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, SyncFairLootChestsPayload> CODEC = StreamCodec.composite(
		ENTRY_CODEC.apply(ByteBufCodecs.list()), SyncFairLootChestsPayload::entries,
		SyncFairLootChestsPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
