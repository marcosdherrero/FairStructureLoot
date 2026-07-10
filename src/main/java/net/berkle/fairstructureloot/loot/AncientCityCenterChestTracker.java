package net.berkle.fairstructureloot.loot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import net.berkle.fairstructureloot.FairStructureLootMain;

/** Persists ancient city center reward chest positions (they have no loot table in structure data). */
public final class AncientCityCenterChestTracker extends SavedData {

	public static final Identifier DATA_ID = Identifier.fromNamespaceAndPath(
		FairStructureLootMain.MOD_ID, "ancient_city_center_chests"
	);

	private static final Codec<AncientCityCenterChestTracker> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.STRING.listOf().optionalFieldOf("Chests", List.of()).forGetter(data -> new ArrayList<>(data.chests))
	).apply(instance, list -> {
		var tracker = new AncientCityCenterChestTracker();
		tracker.chests.addAll(list);
		return tracker;
	}));

	public static final SavedDataType<AncientCityCenterChestTracker> TYPE = new SavedDataType<>(
		DATA_ID,
		AncientCityCenterChestTracker::new,
		CODEC,
		DataFixTypes.LEVEL
	);

	private final Set<String> chests = new HashSet<>();

	public AncientCityCenterChestTracker() {
	}

	public static AncientCityCenterChestTracker get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	public boolean isRegistered(Level level, BlockPos pos) {
		return chests.contains(ContainerKeys.containerKey(level, pos));
	}

	public void register(Level level, BlockPos pos) {
		if (chests.add(ContainerKeys.containerKey(level, pos))) {
			setDirty();
		}
	}

	public void unregister(Level level, BlockPos pos) {
		if (chests.remove(ContainerKeys.containerKey(level, pos))) {
			setDirty();
		}
	}
}
