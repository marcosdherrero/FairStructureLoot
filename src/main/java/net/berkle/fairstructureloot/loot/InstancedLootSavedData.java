package net.berkle.fairstructureloot.loot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.loot.LootTable;

import net.berkle.fairstructureloot.FairStructureLootMain;

/** Per-world storage of instanced loot chest folders and player inventories. */
public class InstancedLootSavedData extends SavedData {

	public static final Identifier DATA_ID = Identifier.fromNamespaceAndPath(FairStructureLootMain.MOD_ID, "instanced_loot");

	private static final Codec<List<ItemStack>> ITEM_LIST_CODEC = SavedInventoryCodecs.itemList();
	private static final Codec<Map<String, LootChestFolder>> CONTAINERS_CODEC = Codec.unboundedMap(Codec.STRING, LootChestFolder.CODEC);

	private static final Codec<InstancedLootSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		CONTAINERS_CODEC.optionalFieldOf("Containers", Map.of()).forGetter(InstancedLootSavedData::containersForCodec),
		Codec.unboundedMap(Codec.STRING, ITEM_LIST_CODEC).optionalFieldOf("Inventories", Map.of()).forGetter(InstancedLootSavedData::legacyInventoriesForCodec)
	).apply(instance, InstancedLootSavedData::fromCodec));

	public static final SavedDataType<InstancedLootSavedData> TYPE = new SavedDataType<>(
		DATA_ID,
		InstancedLootSavedData::new,
		CODEC,
		DataFixTypes.LEVEL
	);

	public record OpenResult(NonNullList<ItemStack> items, boolean causedGlobalRoll) {
	}

	private final Map<String, LootChestFolder> containers = new HashMap<>();

	public InstancedLootSavedData() {
	}

	private InstancedLootSavedData(Map<String, LootChestFolder> containers) {
		this.containers.putAll(containers);
	}

	private static InstancedLootSavedData fromCodec(Map<String, LootChestFolder> containers, Map<String, List<ItemStack>> legacyInventories) {
		InstancedLootSavedData data = new InstancedLootSavedData(containers);
		if (!legacyInventories.isEmpty() && data.containers.isEmpty()) {
			data.migrateLegacyInventories(legacyInventories);
		}
		return data;
	}

	private Map<String, LootChestFolder> containersForCodec() {
		return Map.copyOf(containers);
	}

	private Map<String, List<ItemStack>> legacyInventoriesForCodec() {
		return Map.of();
	}

	private void migrateLegacyInventories(Map<String, List<ItemStack>> legacyInventories) {
		for (Map.Entry<String, List<ItemStack>> entry : legacyInventories.entrySet()) {
			int separator = entry.getKey().lastIndexOf('|');
			if (separator < 0) {
				continue;
			}
			String containerKey = entry.getKey().substring(0, separator);
			UUID playerId = UUID.fromString(entry.getKey().substring(separator + 1));
			LootChestFolder folder = containers.computeIfAbsent(containerKey, key -> new LootChestFolder());
			folder.markOpened(playerId);
			folder.inventories.put(playerId, entry.getValue());
		}
		setDirty(true);
	}

	public static InstancedLootSavedData get(Level level) {
		if (!(level instanceof ServerLevel serverLevel)) {
			throw new IllegalStateException("InstancedLootSavedData only on server");
		}
		return serverLevel.getServer().getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(TYPE);
	}

	public List<String> collectOpenedContainerKeysForPlayer(UUID playerId) {
		List<String> keys = new ArrayList<>();
		for (Map.Entry<String, LootChestFolder> entry : containers.entrySet()) {
			if (entry.getValue().hasOpened(playerId)) {
				keys.add(entry.getKey());
			}
		}
		return keys;
	}

	public List<String> collectGloballyOpenedContainerKeys() {
		List<String> keys = new ArrayList<>();
		for (Map.Entry<String, LootChestFolder> entry : containers.entrySet()) {
			if (entry.getValue().isGloballyRolled()) {
				keys.add(entry.getKey());
			}
		}
		return keys;
	}

	public OpenResult openContainer(
		ServerLevel level,
		ServerPlayer player,
		BlockPos pos,
		ResourceKey<LootTable> lootTable,
		long lootSeed,
		int size
	) {
		String key = ContainerKeys.containerKey(level, pos);
		LootChestFolder folder = containers.computeIfAbsent(key, ignored -> new LootChestFolder());
		UUID playerId = player.getUUID();

		if (!folder.matchesLootTable(lootTable)) {
			folder.clearRollData();
			setDirty(true);
		}

		if (folder.inventories.containsKey(playerId)) {
			List<ItemStack> cached = folder.inventories.get(playerId);
			// Empty is valid: the player already took everything (e.g. End ship elytra).
			// Only discard + re-roll sparse inventories left by the pre-1.0.1 menu bug.
			if (!InstancedLootGenerator.isCorruptedSparseRoll(lootTable, cached)) {
				return new OpenResult(copyToNonNullList(cached, size), false);
			}
			folder.inventories.remove(playerId);
			folder.openedPlayers.remove(playerId);
		}

		boolean perPlayerIndicator = StructureLootTables.usesPerPlayerIndicator(lootTable);
		boolean useSharedRoll = LootRollModeSavedData.get(level).isShared() && !perPlayerIndicator;
		boolean causedGlobalRoll = false;
		NonNullList<ItemStack> generated;

		if (useSharedRoll) {
			if (!folder.hasSharedRollTemplate() || InstancedLootGenerator.isCorruptedSparseRoll(lootTable, folder.sharedRollTemplate())) {
				if (InstancedLootGenerator.isCorruptedSparseRoll(lootTable, folder.sharedRollTemplate())) {
					folder.setSharedRollTemplate(List.of());
					folder.globallyRolled = false;
				}
				generated = InstancedLootGenerator.generate(level, player, pos, lootTable, lootSeed, size, false);
				if (InstancedLootGenerator.hasAnyItems(generated)) {
					folder.setSharedRollTemplate(copyFromNonNullList(generated));
					folder.markGloballyRolled();
					causedGlobalRoll = true;
				}
			} else {
				generated = copyToNonNullList(folder.sharedRollTemplate(), size);
			}
		} else {
			generated = InstancedLootGenerator.generate(level, player, pos, lootTable, lootSeed, size, true);
		}

		if (InstancedLootGenerator.hasAnyItems(generated)) {
			folder.markOpened(playerId);
			folder.setLootTable(lootTable);
			folder.inventories.put(playerId, copyFromNonNullList(generated));
			setDirty(true);
		}
		return new OpenResult(generated, causedGlobalRoll);
	}

	public void updateInventory(ServerLevel level, ServerPlayer player, BlockPos pos, NonNullList<ItemStack> items) {
		String key = ContainerKeys.containerKey(level, pos);
		LootChestFolder folder = containers.get(key);
		if (folder == null) {
			return;
		}
		folder.inventories.put(player.getUUID(), copyFromNonNullList(items));
		setDirty(true);
	}

	public void destroyContainer(
		ServerLevel level,
		BlockPos pos,
		@Nullable ServerPlayer closestPlayer,
		@Nullable ResourceKey<LootTable> lootTable,
		long lootSeed,
		int size
	) {
		String key = ContainerKeys.containerKey(level, pos);
		LootChestFolder folder = containers.get(key);

		if (closestPlayer != null) {
			UUID playerId = closestPlayer.getUUID();
			if (folder != null && folder.inventories.containsKey(playerId)) {
				dropStacks(level, pos, folder.inventories.get(playerId));
			} else if (lootTable != null && StructureLootTables.isInstanced(level, lootTable)) {
				OpenResult open = openContainer(level, closestPlayer, pos, lootTable, lootSeed, size);
				dropStacks(level, pos, open.items());
			}
		}

		containers.remove(key);
		setDirty(true);
	}

	private static void dropStacks(ServerLevel level, BlockPos pos, Iterable<ItemStack> items) {
		for (ItemStack stack : items) {
			if (!stack.isEmpty()) {
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
			}
		}
	}

	public void removeContainer(ServerLevel level, BlockPos pos) {
		String key = ContainerKeys.containerKey(level, pos);
		if (containers.remove(key) != null) {
			setDirty(true);
		}
	}

	private static NonNullList<ItemStack> copyToNonNullList(List<ItemStack> stacks, int size) {
		NonNullList<ItemStack> out = NonNullList.withSize(size, ItemStack.EMPTY);
		for (int i = 0; i < Math.min(size, stacks.size()); i++) {
			ItemStack stack = stacks.get(i);
			if (!stack.isEmpty()) {
				out.set(i, stack.copy());
			}
		}
		return out;
	}

	private static List<ItemStack> copyFromNonNullList(NonNullList<ItemStack> stacks) {
		return stacks.stream().map(ItemStack::copy).toList();
	}
}
