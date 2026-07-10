package net.berkle.fairstructureloot.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import net.berkle.fairstructureloot.FairStructureLootMain;

/** S2C: per-structure activation flags for accurate client HUD detection. */
public record SyncStructureActivationPayload(List<Entry> structures) implements CustomPacketPayload {

	public record Entry(String name, boolean enabled) {
	}

	public SyncStructureActivationPayload(Map<String, Boolean> structureStates) {
		this(toEntries(structureStates));
	}

	public static final CustomPacketPayload.Type<SyncStructureActivationPayload> TYPE = new CustomPacketPayload.Type<>(
		Identifier.fromNamespaceAndPath(FairStructureLootMain.MOD_ID, "sync_structure_activation")
	);

	private static final StreamCodec<RegistryFriendlyByteBuf, Entry> ENTRY_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, Entry::name,
		ByteBufCodecs.BOOL, Entry::enabled,
		Entry::new
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, SyncStructureActivationPayload> CODEC = StreamCodec.composite(
		ENTRY_CODEC.apply(ByteBufCodecs.collection(ArrayList::new)), SyncStructureActivationPayload::structures,
		SyncStructureActivationPayload::new
	);

	private static List<Entry> toEntries(Map<String, Boolean> structureStates) {
		List<Entry> entries = new ArrayList<>(structureStates.size());
		for (Map.Entry<String, Boolean> entry : structureStates.entrySet()) {
			entries.add(new Entry(entry.getKey(), entry.getValue()));
		}
		return entries;
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
