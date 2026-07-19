package net.berkle.fairstructureloot.loot;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Server-side mirror of fair-loot positions sent to clients.
 * Break protection uses this even if the block entity's loot table was cleared after open.
 */
public final class FairLootChestServerTracker {

	private record Entry(boolean perPlayerIndicator, @Nullable Identifier lootTableId) {
	}

	private static final Map<String, Entry> FAIR_LOOT = new ConcurrentHashMap<>();

	private FairLootChestServerTracker() {
	}

	public static void set(
		Identifier dimension,
		BlockPos pos,
		boolean fairLoot,
		boolean perPlayerIndicator,
		@Nullable ResourceKey<LootTable> lootTable
	) {
		String key = ContainerKeys.containerKey(dimension, pos);
		if (fairLoot) {
			FAIR_LOOT.put(key, new Entry(
				perPlayerIndicator,
				lootTable != null ? lootTable.identifier() : null
			));
		} else {
			FAIR_LOOT.remove(key);
		}
	}

	public static void set(
		ServerLevel level,
		BlockPos pos,
		boolean fairLoot,
		boolean perPlayerIndicator,
		@Nullable ResourceKey<LootTable> lootTable
	) {
		set(level.dimension().identifier(), pos, fairLoot, perPlayerIndicator, lootTable);
	}

	public static boolean isFairLoot(Level level, BlockPos pos) {
		return FAIR_LOOT.containsKey(ContainerKeys.containerKey(level, pos));
	}

	@Nullable
	public static ResourceKey<LootTable> lootTable(Level level, BlockPos pos) {
		Entry entry = FAIR_LOOT.get(ContainerKeys.containerKey(level, pos));
		if (entry == null || entry.lootTableId() == null) {
			return null;
		}
		return ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, entry.lootTableId());
	}

	public static void clear() {
		FAIR_LOOT.clear();
	}

	/** Test helper / debug. */
	public static Set<String> keys() {
		return Set.copyOf(FAIR_LOOT.keySet());
	}
}
