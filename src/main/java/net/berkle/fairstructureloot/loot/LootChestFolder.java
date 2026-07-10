package net.berkle.fairstructureloot.loot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;

/** Per-container record of opened players, per-player inventories, and optional shared-roll template. */
final class LootChestFolder {

	private static final Codec<List<ItemStack>> ITEM_LIST_CODEC = SavedInventoryCodecs.itemList();
	private static final Codec<List<UUID>> OPENED_PLAYERS_CODEC = Codec.STRING.listOf().xmap(
		strings -> strings.stream().map(UUID::fromString).toList(),
		uuids -> uuids.stream().map(UUID::toString).toList()
	);

	static final Codec<LootChestFolder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		OPENED_PLAYERS_CODEC.optionalFieldOf("OpenedPlayers", List.of()).forGetter(LootChestFolder::openedPlayersForCodec),
		Codec.unboundedMap(Codec.STRING, ITEM_LIST_CODEC).optionalFieldOf("Inventories", Map.of()).forGetter(LootChestFolder::inventoriesForCodec),
		Codec.BOOL.optionalFieldOf("GloballyRolled", false).forGetter(LootChestFolder::globallyRolledForCodec),
		ITEM_LIST_CODEC.optionalFieldOf("SharedRoll", List.of()).forGetter(LootChestFolder::sharedRollForCodec),
		Identifier.CODEC.optionalFieldOf("LootTable").forGetter(LootChestFolder::lootTableForCodec)
	).apply(instance, LootChestFolder::fromCodec));

	final List<UUID> openedPlayers = new ArrayList<>();
	final Map<UUID, List<ItemStack>> inventories = new HashMap<>();
	boolean globallyRolled;
	List<ItemStack> sharedRollTemplate = List.of();
	@Nullable
	Identifier lootTableId;

	LootChestFolder() {
	}

	private LootChestFolder(
		List<UUID> openedPlayers,
		Map<UUID, List<ItemStack>> inventories,
		boolean globallyRolled,
		List<ItemStack> sharedRollTemplate,
		@Nullable Identifier lootTableId
	) {
		this.openedPlayers.addAll(openedPlayers);
		this.inventories.putAll(inventories);
		this.globallyRolled = globallyRolled;
		this.sharedRollTemplate = sharedRollTemplate;
		this.lootTableId = lootTableId;
	}

	private static LootChestFolder fromCodec(
		List<UUID> openedPlayers,
		Map<String, List<ItemStack>> inventories,
		boolean globallyRolled,
		List<ItemStack> sharedRollTemplate,
		Optional<Identifier> lootTableId
	) {
		Map<UUID, List<ItemStack>> parsed = new HashMap<>();
		for (Map.Entry<String, List<ItemStack>> entry : inventories.entrySet()) {
			parsed.put(UUID.fromString(entry.getKey()), entry.getValue());
		}
		return new LootChestFolder(openedPlayers, parsed, globallyRolled, sharedRollTemplate, lootTableId.orElse(null));
	}

	private List<UUID> openedPlayersForCodec() {
		return List.copyOf(openedPlayers);
	}

	private Map<String, List<ItemStack>> inventoriesForCodec() {
		Map<String, List<ItemStack>> out = new HashMap<>();
		for (Map.Entry<UUID, List<ItemStack>> entry : inventories.entrySet()) {
			out.put(entry.getKey().toString(), entry.getValue());
		}
		return out;
	}

	private boolean globallyRolledForCodec() {
		return globallyRolled;
	}

	private List<ItemStack> sharedRollForCodec() {
		return sharedRollTemplate;
	}

	private Optional<Identifier> lootTableForCodec() {
		return Optional.ofNullable(lootTableId);
	}

	boolean matchesLootTable(ResourceKey<LootTable> lootTable) {
		return lootTableId != null && lootTableId.equals(lootTable.identifier());
	}

	void setLootTable(ResourceKey<LootTable> lootTable) {
		lootTableId = lootTable.identifier();
	}

	void clearRollData() {
		openedPlayers.clear();
		inventories.clear();
		globallyRolled = false;
		sharedRollTemplate = List.of();
		lootTableId = null;
	}

	boolean hasOpened(UUID playerId) {
		return openedPlayers.contains(playerId);
	}

	void markOpened(UUID playerId) {
		if (!openedPlayers.contains(playerId)) {
			openedPlayers.add(playerId);
		}
	}

	boolean isGloballyRolled() {
		return globallyRolled;
	}

	void markGloballyRolled() {
		globallyRolled = true;
	}

	boolean hasSharedRollTemplate() {
		return InstancedLootGenerator.hasAnyItems(sharedRollTemplate);
	}

	void setSharedRollTemplate(List<ItemStack> template) {
		sharedRollTemplate = template;
	}

	List<ItemStack> sharedRollTemplate() {
		return sharedRollTemplate;
	}
}
