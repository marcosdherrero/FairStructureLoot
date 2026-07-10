package net.berkle.fairstructureloot.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import net.berkle.fairstructureloot.FairStructureLootMain;

/** S2C: shared-roll containers that have already been opened by someone (sent on join). */
public record SyncGloballyOpenedChestsPayload(List<String> containerKeys) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SyncGloballyOpenedChestsPayload> TYPE = new CustomPacketPayload.Type<>(
		Identifier.fromNamespaceAndPath(FairStructureLootMain.MOD_ID, "sync_globally_opened_chests")
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, SyncGloballyOpenedChestsPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8), SyncGloballyOpenedChestsPayload::containerKeys,
		SyncGloballyOpenedChestsPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
