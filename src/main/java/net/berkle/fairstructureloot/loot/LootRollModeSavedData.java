package net.berkle.fairstructureloot.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import net.berkle.fairstructureloot.FairStructureLootMain;

/** Per-world setting for per-player random loot vs one shared roll per container. */
public final class LootRollModeSavedData extends SavedData {

	public static final Identifier DATA_ID = Identifier.fromNamespaceAndPath(
		FairStructureLootMain.MOD_ID, "loot_roll_mode"
	);

	public enum Mode implements StringRepresentable {
		RANDOM("random"),
		SHARED("shared");

		public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);

		private final String commandName;

		Mode(String commandName) {
			this.commandName = commandName;
		}

		public String commandName() {
			return commandName;
		}

		@Override
		public String getSerializedName() {
			return commandName;
		}
	}

	private static final Codec<LootRollModeSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Mode.CODEC.optionalFieldOf("Mode", Mode.RANDOM).forGetter(LootRollModeSavedData::mode)
	).apply(instance, LootRollModeSavedData::new));

	public static final SavedDataType<LootRollModeSavedData> TYPE = new SavedDataType<>(
		DATA_ID,
		LootRollModeSavedData::new,
		CODEC,
		DataFixTypes.LEVEL
	);

	private Mode mode = Mode.RANDOM;

	public LootRollModeSavedData() {
	}

	private LootRollModeSavedData(Mode mode) {
		this.mode = mode;
	}

	public static LootRollModeSavedData get(Level level) {
		if (!(level instanceof ServerLevel serverLevel)) {
			throw new IllegalStateException("LootRollModeSavedData only on server");
		}
		return serverLevel.getServer().getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(TYPE);
	}

	public Mode mode() {
		return mode;
	}

	public boolean isShared() {
		return mode == Mode.SHARED;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
		setDirty(true);
	}
}
