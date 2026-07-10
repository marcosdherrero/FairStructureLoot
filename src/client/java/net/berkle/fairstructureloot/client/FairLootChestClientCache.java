package net.berkle.fairstructureloot.client;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import net.berkle.fairstructureloot.loot.ContainerKeys;
import net.berkle.fairstructureloot.network.SyncFairLootChestsPayload;

/** Server-synced fair-loot container positions and per-player-indicator flags (for HUD). */
public final class FairLootChestClientCache {

	private static final Set<String> FAIR_LOOT_CHESTS = ConcurrentHashMap.newKeySet();
	private static final Set<String> PER_PLAYER_INDICATOR_CHESTS = ConcurrentHashMap.newKeySet();

	private FairLootChestClientCache() {
	}

	public static void replaceAll(List<SyncFairLootChestsPayload.Entry> entries) {
		FAIR_LOOT_CHESTS.clear();
		PER_PLAYER_INDICATOR_CHESTS.clear();
		for (SyncFairLootChestsPayload.Entry entry : entries) {
			setFairLoot(entry.dimension(), entry.pos(), true, entry.perPlayerIndicator());
		}
	}

	public static void setFairLoot(Identifier dimension, BlockPos pos, boolean fairLoot, boolean perPlayerIndicator) {
		String key = ContainerKeys.containerKey(dimension, pos);
		if (fairLoot) {
			FAIR_LOOT_CHESTS.add(key);
			if (perPlayerIndicator) {
				PER_PLAYER_INDICATOR_CHESTS.add(key);
			} else {
				PER_PLAYER_INDICATOR_CHESTS.remove(key);
			}
		} else {
			FAIR_LOOT_CHESTS.remove(key);
			PER_PLAYER_INDICATOR_CHESTS.remove(key);
		}
	}

	public static boolean isFairLoot(Level level, BlockPos pos) {
		return FAIR_LOOT_CHESTS.contains(ContainerKeys.containerKey(level, pos));
	}

	public static boolean isPerPlayerIndicator(Level level, BlockPos pos) {
		return PER_PLAYER_INDICATOR_CHESTS.contains(ContainerKeys.containerKey(level, pos));
	}

	public static void clear() {
		FAIR_LOOT_CHESTS.clear();
		PER_PLAYER_INDICATOR_CHESTS.clear();
	}
}
