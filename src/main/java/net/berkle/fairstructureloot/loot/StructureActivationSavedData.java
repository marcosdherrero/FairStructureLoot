package net.berkle.fairstructureloot.loot;



import java.util.HashMap;

import java.util.Map;



import com.mojang.serialization.Codec;

import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

import net.minecraft.server.level.ServerLevel;

import net.minecraft.util.datafix.DataFixTypes;

import net.minecraft.world.level.Level;

import net.minecraft.world.level.saveddata.SavedData;

import net.minecraft.world.level.saveddata.SavedDataType;



import net.berkle.fairstructureloot.FairStructureLootMain;



/** Per-world enabled/disabled state for each supported structure group. */

public final class StructureActivationSavedData extends SavedData {



	public static final Identifier DATA_ID = Identifier.fromNamespaceAndPath(FairStructureLootMain.MOD_ID, "structure_activation");



	private static final Codec<Map<String, Boolean>> FLAGS_CODEC = Codec.unboundedMap(Codec.STRING, Codec.BOOL);

	private static final Codec<StructureActivationSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(

		FLAGS_CODEC.optionalFieldOf("Structures", Map.of()).forGetter(StructureActivationSavedData::asSerializedMap)

	).apply(instance, StructureActivationSavedData::fromCodec));



	public static final SavedDataType<StructureActivationSavedData> TYPE = new SavedDataType<>(

		DATA_ID,

		StructureActivationSavedData::new,

		CODEC,

		DataFixTypes.LEVEL

	);



	private final Map<String, Boolean> structureStates = new HashMap<>();



	public StructureActivationSavedData() {

		resetToDefaults();

	}



	private static StructureActivationSavedData fromCodec(Map<String, Boolean> loadedStates) {

		StructureActivationSavedData data = new StructureActivationSavedData();

		for (Map.Entry<String, Boolean> entry : loadedStates.entrySet()) {

			StructureGroupRegistry.groupByCommandName(entry.getKey())

				.ifPresent(group -> data.structureStates.put(group.commandName(), entry.getValue()));

		}

		return data;

	}



	private Map<String, Boolean> asSerializedMap() {

		Map<String, Boolean> out = new HashMap<>();

		for (StructureGroup group : StructureGroupRegistry.allGroups()) {

			out.put(group.commandName(), structureStates.getOrDefault(group.commandName(), group.defaultEnabled()));

		}

		return out;

	}



	public static StructureActivationSavedData get(Level level) {

		if (!(level instanceof ServerLevel serverLevel)) {

			throw new IllegalStateException("StructureActivationSavedData only on server");

		}

		return serverLevel.getServer().getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(TYPE);

	}



	public boolean isEnabled(String commandName) {

		return structureStates.getOrDefault(

			commandName,

			StructureGroupRegistry.groupByCommandName(commandName).map(StructureGroup::defaultEnabled).orElse(false)

		);

	}



	public void setEnabled(String commandName, boolean enabled) {

		if (StructureGroupRegistry.groupByCommandName(commandName).isEmpty()) {

			return;

		}

		structureStates.put(commandName, enabled);

		setDirty(true);

	}



	public void setAll(boolean enabled) {

		for (StructureGroup group : StructureGroupRegistry.allGroups()) {

			structureStates.put(group.commandName(), enabled);

		}

		setDirty(true);

	}



	public Map<String, Boolean> exportStates() {

		return asSerializedMap();

	}



	private void resetToDefaults() {

		structureStates.clear();

		for (StructureGroup group : StructureGroupRegistry.allGroups()) {

			structureStates.put(group.commandName(), group.defaultEnabled());

		}

	}

}

