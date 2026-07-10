package net.berkle.fairstructureloot.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import net.berkle.fairstructureloot.FairStructureLootMain;

/** S2C: full list of container keys this player has already opened (sent on join). */
public record SyncOpenedChestsPayload(List<String> containerKeys) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SyncOpenedChestsPayload> TYPE = new CustomPacketPayload.Type<>(
		Identifier.fromNamespaceAndPath(FairStructureLootMain.MOD_ID, "sync_opened_chests")
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, SyncOpenedChestsPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8), SyncOpenedChestsPayload::containerKeys,
		SyncOpenedChestsPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
