package net.berkle.fairstructureloot.loot;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;

import net.berkle.fairstructureloot.network.FairStructureLootNetworking;

/** Per-player trial spawner reward rolls (completion eject + ominous mid-fight drops). */
public final class InstancedTrialSpawnerLoot {

	private static final int COMPLETION_EJECT_SLOTS = 1;

	private InstancedTrialSpawnerLoot() {
	}

	/** Resolves the player whose reward vanilla is ejecting on this cycle. */
	public static ServerPlayer resolveNextEjectionPlayer(ServerLevel level, TrialSpawner spawner) {
		Set<UUID> detected = TrialSpawnerPlayers.detectedPlayerIds(spawner);
		if (detected.isEmpty()) {
			return null;
		}
		UUID next = detected.iterator().next();
		return level.getServer().getPlayerList().getPlayer(next);
	}

	public static void ejectReward(
		ServerLevel level,
		BlockPos pos,
		ResourceKey<LootTable> lootTable,
		ServerPlayer player
	) {
		long lootSeed = spawnerSeed(pos, lootTable);
		ItemStack item = rollPersistentSpawnerItem(level, player, pos, lootTable, lootSeed);
		if (item.isEmpty()) {
			return;
		}

		Vec3 ejectPos = Vec3.atBottomCenterOf(pos).relative(Direction.UP, 1.2D);
		DefaultDispenseItemBehavior.spawnItem(level, item.copy(), 2, Direction.UP, ejectPos);
		level.levelEvent(3014, pos, 0);
	}

	public static ItemStack rollOminousDispensedItem(
		ServerLevel level,
		BlockPos spawnerPos,
		ServerPlayer player,
		ResourceKey<LootTable> lootTable
	) {
		long lootSeed = spawnerSeed(spawnerPos, lootTable);
		boolean shared = LootRollModeSavedData.get(level).isShared()
			&& !StructureLootTables.usesPerPlayerIndicator(lootTable);
		if (shared) {
			return rollPersistentSpawnerItem(level, player, spawnerPos, lootTable, lootSeed);
		}
		NonNullList<ItemStack> rolled = InstancedLootGenerator.generate(
			level,
			player,
			spawnerPos,
			lootTable,
			lootSeed ^ level.getGameTime(),
			COMPLETION_EJECT_SLOTS,
			true
		);
		return firstNonEmpty(rolled);
	}

	private static ItemStack rollPersistentSpawnerItem(
		ServerLevel level,
		ServerPlayer player,
		BlockPos pos,
		ResourceKey<LootTable> lootTable,
		long lootSeed
	) {
		var open = InstancedLootSavedData.get(level).openContainer(
			level,
			player,
			pos,
			lootTable,
			lootSeed,
			COMPLETION_EJECT_SLOTS
		);
		FairStructureLootNetworking.notifyChestOpened(player, level, pos, lootTable, open.causedGlobalRoll());
		return firstNonEmpty(open.items());
	}

	private static long spawnerSeed(BlockPos pos, ResourceKey<LootTable> lootTable) {
		return pos.asLong() ^ lootTable.identifier().hashCode();
	}

	private static ItemStack firstNonEmpty(List<ItemStack> items) {
		for (ItemStack stack : items) {
			if (!stack.isEmpty()) {
				return stack.copy();
			}
		}
		return ItemStack.EMPTY;
	}

	private static ItemStack firstNonEmpty(NonNullList<ItemStack> items) {
		List<ItemStack> list = new ArrayList<>(items.size());
		list.addAll(items);
		return firstNonEmpty(list);
	}
}
