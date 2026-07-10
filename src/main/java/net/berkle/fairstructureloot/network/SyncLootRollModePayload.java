package net.berkle.fairstructureloot.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import net.berkle.fairstructureloot.FairStructureLootMain;

/** S2C: whether containers use one shared loot roll for all players. */
public record SyncLootRollModePayload(boolean sharedRoll) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SyncLootRollModePayload> TYPE = new CustomPacketPayload.Type<>(
		Identifier.fromNamespaceAndPath(FairStructureLootMain.MOD_ID, "sync_loot_roll_mode")
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, SyncLootRollModePayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, SyncLootRollModePayload::sharedRoll,
		SyncLootRollModePayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
