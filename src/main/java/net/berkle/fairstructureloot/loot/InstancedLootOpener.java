package net.berkle.fairstructureloot.loot;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;

import net.berkle.fairstructureloot.network.FairStructureLootNetworking;

/** Creates per-player chest menus backed by generated loot (re-openable like a personal stash). */
public final class InstancedLootOpener {

	private InstancedLootOpener() {
	}

	public static AbstractContainerMenu createInstancedMenu(
		ServerPlayer player,
		RandomizableContainerBlockEntity blockEntity,
		int syncId,
		Inventory inventory
	) {
		if (blockEntity instanceof ChestBlockEntity chest) {
			return DoubleChestHelper.findPair(player.level(), chest)
				.map(pair -> openChestPair(player, pair.first(), pair.second(), syncId, inventory))
				.orElseGet(() -> openSingleChest(player, chest, syncId, inventory));
		}
		return openSingleChest(player, blockEntity, syncId, inventory);
	}

	public static AbstractContainerMenu createInstancedDoubleMenu(
		ServerPlayer player,
		ChestBlockEntity first,
		ChestBlockEntity second,
		int syncId,
		Inventory inventory
	) {
		return openChestPair(player, first, second, syncId, inventory);
	}

	private static AbstractContainerMenu openSingleChest(
		ServerPlayer player,
		RandomizableContainerBlockEntity blockEntity,
		int syncId,
		Inventory inventory
	) {
		ResourceKey<LootTable> lootTable = StructureLootTables.resolveLootTable(blockEntity);
		ServerLevel level = (ServerLevel) player.level();
		BlockPos pos = blockEntity.getBlockPos();
		if (!StructureLootTables.isInstanced(level, lootTable)) {
			return null;
		}

		if (blockEntity instanceof ChestBlockEntity chest
			&& StructureLootTables.isAncientCityCenterChest(level, blockEntity)) {
			StructureLootTables.migrateAncientCityCenterChest(level, chest);
		}

		int size = blockEntity.getContainerSize();
		long lootSeed = StructureLootTables.resolveLootSeed(blockEntity, pos);
		return openMenu(player, blockEntity, null, pos, lootTable, lootSeed, size, syncId, inventory);
	}

	private static AbstractContainerMenu openChestPair(
		ServerPlayer player,
		ChestBlockEntity first,
		ChestBlockEntity second,
		int syncId,
		Inventory inventory
	) {
		ServerLevel level = (ServerLevel) player.level();
		ResourceKey<LootTable> lootTable = StructureLootTables.resolveLootTable(first, second);
		if (!StructureLootTables.isInstanced(level, lootTable)) {
			return null;
		}

		BlockPos storagePos = DoubleChestHelper.canonicalPos(first.getBlockPos(), second.getBlockPos());
		long lootSeed = StructureLootTables.resolveLootSeed(first, second);
		int size = DoubleChestHelper.DOUBLE_CHEST_SLOTS;
		return openMenu(player, first, second, storagePos, lootTable, lootSeed, size, syncId, inventory);
	}

	private static AbstractContainerMenu openMenu(
		ServerPlayer player,
		RandomizableContainerBlockEntity primary,
		ChestBlockEntity partner,
		BlockPos storagePos,
		ResourceKey<LootTable> lootTable,
		long lootSeed,
		int size,
		int syncId,
		Inventory inventory
	) {
		ServerLevel level = (ServerLevel) player.level();
		var open = InstancedLootSavedData.get(level).openContainer(
			level,
			player,
			storagePos,
			lootTable,
			lootSeed,
			size
		);

		InstancedLootContainer container = new InstancedLootContainer(
			level,
			player,
			storagePos,
			primary,
			partner,
			open.items(),
			size
		);

		FairStructureLootNetworking.notifyChestOpened(player, level, storagePos, lootTable, open.causedGlobalRoll());
		return menuForSize(syncId, inventory, container, size);
	}

	private static AbstractContainerMenu menuForSize(int syncId, Inventory inventory, InstancedLootContainer container, int size) {
		int rows = Mth.clamp(size / 9, 1, 6);
		MenuType<ChestMenu> menuType = switch (rows) {
			case 1 -> MenuType.GENERIC_9x1;
			case 2 -> MenuType.GENERIC_9x2;
			case 3 -> MenuType.GENERIC_9x3;
			case 4 -> MenuType.GENERIC_9x4;
			case 5 -> MenuType.GENERIC_9x5;
			default -> MenuType.GENERIC_9x6;
		};
		return new ChestMenu(menuType, syncId, inventory, container, rows);
	}
}
