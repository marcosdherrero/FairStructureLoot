package net.berkle.fairstructureloot.loot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

import net.berkle.fairstructureloot.FairStructureLootMain;

/** Built-in and mod-registered structure loot groups. */
public final class StructureGroupRegistry {

	private static final Map<Identifier, StructureGroup> GROUPS_BY_ID = new LinkedHashMap<>();
	private static final Map<String, StructureGroup> GROUPS_BY_COMMAND = new LinkedHashMap<>();
	private static final Map<Identifier, StructureGroup> GROUPS_BY_LOOT_TABLE = new HashMap<>();

	static {
		registerBuiltIns();
	}

	private StructureGroupRegistry() {
	}

	public static synchronized void register(StructureGroup group) {
		if (GROUPS_BY_COMMAND.containsKey(group.commandName())) {
			throw new IllegalArgumentException("Duplicate structure group command name: " + group.commandName());
		}
		for (Identifier lootTableId : group.lootTableIds()) {
			if (GROUPS_BY_LOOT_TABLE.containsKey(lootTableId)) {
				throw new IllegalArgumentException("Loot table already registered: " + lootTableId);
			}
		}
		GROUPS_BY_ID.put(group.id(), group);
		GROUPS_BY_COMMAND.put(group.commandName(), group);
		for (Identifier lootTableId : group.lootTableIds()) {
			GROUPS_BY_LOOT_TABLE.put(lootTableId, group);
		}
	}

	public static Optional<StructureGroup> groupForLootTable(Identifier lootTableId) {
		return Optional.ofNullable(GROUPS_BY_LOOT_TABLE.get(lootTableId));
	}

	public static Optional<StructureGroup> groupByCommandName(String commandName) {
		if (commandName == null) {
			return Optional.empty();
		}
		for (Map.Entry<String, StructureGroup> entry : GROUPS_BY_COMMAND.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(commandName)) {
				return Optional.of(entry.getValue());
			}
		}
		return Optional.empty();
	}

	public static List<StructureGroup> allGroups() {
		return Collections.unmodifiableList(new ArrayList<>(GROUPS_BY_ID.values()));
	}

	public static Set<String> allCommandNames() {
		return Collections.unmodifiableSet(GROUPS_BY_COMMAND.keySet());
	}

	private static void registerBuiltIns() {
		register(group("end_city", true, BuiltInLootTables.END_CITY_TREASURE));
		register(group("end_ship", true, StructureLootTables.END_SHIP_ELYTRA));
		register(group("ancient_city", true,
			BuiltInLootTables.ANCIENT_CITY,
			BuiltInLootTables.ANCIENT_CITY_ICE_BOX,
			StructureLootTables.ANCIENT_CITY_CENTER
		));
		register(group("bastion", true,
			BuiltInLootTables.BASTION_TREASURE,
			BuiltInLootTables.BASTION_OTHER,
			BuiltInLootTables.BASTION_BRIDGE,
			BuiltInLootTables.BASTION_HOGLIN_STABLE
		));
		register(trialChambersGroup());
		register(group("buried_treasure", true, BuiltInLootTables.BURIED_TREASURE));
		register(group("stronghold", true,
			BuiltInLootTables.STRONGHOLD_CORRIDOR,
			BuiltInLootTables.STRONGHOLD_CROSSING,
			BuiltInLootTables.STRONGHOLD_LIBRARY
		));
		register(group("woodland_mansion", true, BuiltInLootTables.WOODLAND_MANSION));
		register(group("nether_fortress", true, BuiltInLootTables.NETHER_BRIDGE));
		register(group("shipwreck", true,
			BuiltInLootTables.SHIPWRECK_MAP,
			BuiltInLootTables.SHIPWRECK_SUPPLY,
			BuiltInLootTables.SHIPWRECK_TREASURE
		));
		register(group("pillager_outpost", true, BuiltInLootTables.PILLAGER_OUTPOST));
		register(group("abandoned_mineshaft", false, BuiltInLootTables.ABANDONED_MINESHAFT));
		register(group("desert_pyramid", false,
			BuiltInLootTables.DESERT_PYRAMID,
			BuiltInLootTables.DESERT_PYRAMID_ARCHAEOLOGY
		));
		register(group("jungle_temple", false,
			BuiltInLootTables.JUNGLE_TEMPLE,
			BuiltInLootTables.JUNGLE_TEMPLE_DISPENSER
		));
		register(group("igloo", false, BuiltInLootTables.IGLOO_CHEST));
		register(group("ruined_portal", false, BuiltInLootTables.RUINED_PORTAL));
		register(group("underwater_ruin", false,
			BuiltInLootTables.UNDERWATER_RUIN_BIG,
			BuiltInLootTables.UNDERWATER_RUIN_SMALL
		));
		register(group("simple_dungeon", false, BuiltInLootTables.SIMPLE_DUNGEON));
		register(group("trail_ruins", false,
			BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_COMMON,
			BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_RARE
		));
		register(group("ocean_ruins_archaeology", false,
			BuiltInLootTables.OCEAN_RUIN_WARM_ARCHAEOLOGY,
			BuiltInLootTables.OCEAN_RUIN_COLD_ARCHAEOLOGY
		));
		register(group("desert_well", false, BuiltInLootTables.DESERT_WELL_ARCHAEOLOGY));
		register(group("village", false,
			BuiltInLootTables.VILLAGE_WEAPONSMITH,
			BuiltInLootTables.VILLAGE_TOOLSMITH,
			BuiltInLootTables.VILLAGE_ARMORER,
			BuiltInLootTables.VILLAGE_CARTOGRAPHER,
			BuiltInLootTables.VILLAGE_MASON,
			BuiltInLootTables.VILLAGE_SHEPHERD,
			BuiltInLootTables.VILLAGE_BUTCHER,
			BuiltInLootTables.VILLAGE_FLETCHER,
			BuiltInLootTables.VILLAGE_FISHER,
			BuiltInLootTables.VILLAGE_TANNERY,
			BuiltInLootTables.VILLAGE_TEMPLE,
			BuiltInLootTables.VILLAGE_DESERT_HOUSE,
			BuiltInLootTables.VILLAGE_PLAINS_HOUSE,
			BuiltInLootTables.VILLAGE_TAIGA_HOUSE,
			BuiltInLootTables.VILLAGE_SNOWY_HOUSE,
			BuiltInLootTables.VILLAGE_SAVANNA_HOUSE
		));
	}

	private static StructureGroup group(String commandName, boolean defaultEnabled, ResourceKey<LootTable>... tables) {
		Identifier[] ids = new Identifier[tables.length];
		for (int i = 0; i < tables.length; i++) {
			ids[i] = tables[i].identifier();
		}
		return group(commandName, defaultEnabled, ids);
	}

	private static StructureGroup trialChambersGroup() {
		return new StructureGroup(
			Identifier.fromNamespaceAndPath(FairStructureLootMain.MOD_ID, "trial_chambers"),
			"trial_chambers",
			true,
			Set.of(
				BuiltInLootTables.TRIAL_CHAMBERS_REWARD.identifier(),
				BuiltInLootTables.TRIAL_CHAMBERS_REWARD_COMMON.identifier(),
				BuiltInLootTables.TRIAL_CHAMBERS_REWARD_RARE.identifier(),
				BuiltInLootTables.TRIAL_CHAMBERS_REWARD_UNIQUE.identifier(),
				BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS.identifier(),
				BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_COMMON.identifier(),
				BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_RARE.identifier(),
				BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS_UNIQUE.identifier(),
				BuiltInLootTables.TRIAL_CHAMBERS_SUPPLY.identifier(),
				BuiltInLootTables.TRIAL_CHAMBERS_CORRIDOR.identifier(),
				BuiltInLootTables.TRIAL_CHAMBERS_INTERSECTION.identifier(),
				BuiltInLootTables.TRIAL_CHAMBERS_INTERSECTION_BARREL.identifier(),
				BuiltInLootTables.TRIAL_CHAMBERS_ENTRANCE.identifier(),
				vanilla("chests/trial_chambers/vault"),
				BuiltInLootTables.TRIAL_CHAMBERS_CORRIDOR_POT.identifier(),
				vanilla("pots/trial_chambers/entrance"),
				vanilla("pots/trial_chambers/intersection"),
				vanilla("pots/trial_chambers/reward"),
				BuiltInLootTables.SPAWNER_TRIAL_CHAMBER_CONSUMABLES.identifier(),
				BuiltInLootTables.SPAWNER_TRIAL_CHAMBER_KEY.identifier(),
				BuiltInLootTables.SPAWNER_TRIAL_ITEMS_TO_DROP_WHEN_OMINOUS.identifier(),
				BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_KEY.identifier(),
				BuiltInLootTables.SPAWNER_OMINOUS_TRIAL_CHAMBER_CONSUMABLES.identifier(),
				vanilla("spawners/trial_chambers/ominous/items_to_drop_when_ominous"),
				BuiltInLootTables.TRIAL_CHAMBERS_CORRIDOR_DISPENSER.identifier(),
				BuiltInLootTables.TRIAL_CHAMBERS_CHAMBER_DISPENSER.identifier(),
				BuiltInLootTables.TRIAL_CHAMBERS_WATER_DISPENSER.identifier()
			)
		);
	}

	private static Identifier vanilla(String path) {
		return Identifier.parse("minecraft:" + path);
	}

	private static StructureGroup group(String commandName, boolean defaultEnabled, Identifier... tables) {
		return new StructureGroup(
			Identifier.fromNamespaceAndPath(FairStructureLootMain.MOD_ID, commandName),
			commandName,
			defaultEnabled,
			Set.of(tables)
		);
	}
}
