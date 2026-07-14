package net.berkle.fairstructureloot.loot;



import java.util.Optional;



import net.minecraft.core.BlockPos;

import net.minecraft.core.NonNullList;

import net.minecraft.core.component.DataComponents;

import net.minecraft.resources.Identifier;

import net.minecraft.resources.ResourceKey;

import net.minecraft.server.level.ServerLevel;

import net.minecraft.tags.BiomeTags;

import net.minecraft.world.item.ItemStack;

import net.minecraft.world.item.Items;

import net.minecraft.world.item.component.SeededContainerLoot;

import net.minecraft.world.level.Level;

import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraft.world.level.block.entity.ChestBlockEntity;

import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;

import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;

import net.minecraft.world.level.storage.loot.LootTable;



import org.jetbrains.annotations.Nullable;



import net.berkle.fairstructureloot.FairStructureLootMain;

import net.berkle.fairstructureloot.mixin.BaseContainerBlockEntityAccessor;



/** Loot tables that should generate per-player instead of being shared. */

public final class StructureLootTables {



	public static final Identifier ANCIENT_CITY_CENTER_ID = Identifier.fromNamespaceAndPath(

		FairStructureLootMain.MOD_ID, "chests/ancient_city_center"

	);



	public static final ResourceKey<LootTable> ANCIENT_CITY_CENTER = ResourceKey.create(

		net.minecraft.core.registries.Registries.LOOT_TABLE,

		ANCIENT_CITY_CENTER_ID

	);



	public static final Identifier END_SHIP_ELYTRA_ID = Identifier.fromNamespaceAndPath(

		"fairstructureloot", "chests/end_ship_elytra"

	);



	public static final ResourceKey<LootTable> END_SHIP_ELYTRA = ResourceKey.create(

		net.minecraft.core.registries.Registries.LOOT_TABLE,

		END_SHIP_ELYTRA_ID

	);



	private StructureLootTables() {

	}



	/** Fair-loot flag and HUD indicator mode for a container block entity. */

	public record FairLootMarker(boolean fairLoot, boolean perPlayerIndicator) {

		public static final FairLootMarker NONE = new FairLootMarker(false, false);

	}



	public static FairLootMarker markerFor(Level level, BlockEntity blockEntity) {

		if (blockEntity == null) {

			return FairLootMarker.NONE;

		}

		// resolveLootTable already covers this chest, the partner's declared table,
		// and either half being an ancient-city center chest. Never recurse into the
		// partner via markerFor - double-chest A<->B would StackOverflow.
		ResourceKey<LootTable> table = resolveLootTable(blockEntity);

		if (table != null && isInstanced(level, table)) {

			return new FairLootMarker(true, usesPerPlayerIndicator(table));

		}

		return FairLootMarker.NONE;

	}



	public static boolean isInstanced(Level level, @Nullable ResourceKey<LootTable> lootTable) {

		if (lootTable == null) {

			return false;

		}

		Optional<StructureGroup> group = StructureGroupRegistry.groupForLootTable(lootTable.identifier());

		return group.isPresent() && isStructureEnabled(level, group.get().commandName());

	}



	public static boolean isInstanced(ServerLevel level, @Nullable ResourceKey<LootTable> lootTable) {

		return isInstanced((Level) level, lootTable);

	}



	/** Vanilla city_center structure places a single golden apple in this slot with no loot table. */

	private static final int ANCIENT_CITY_CENTER_APPLE_SLOT = 13;



	@Nullable

	public static ResourceKey<LootTable> resolveLootTable(BlockEntity blockEntity) {

		if (blockEntity == null) {

			return null;

		}

		if (blockEntity instanceof VaultBlockEntity vault) {

			return vault.getConfig().lootTable();

		}

		ResourceKey<LootTable> table = resolveDeclaredLootTable(blockEntity);

		if (table != null) {

			return table;

		}

		if (blockEntity instanceof ChestBlockEntity chest) {

			Level level = chest.getLevel();

			if (level != null) {

				ResourceKey<LootTable> partnerTable = DoubleChestHelper.findPartner(level, chest)

					.map(StructureLootTables::resolveDeclaredLootTable)

					.orElse(null);

				if (partnerTable != null) {

					return partnerTable;

				}

			}

		}

		if (blockEntity instanceof ChestBlockEntity chest && blockEntity.getLevel() instanceof ServerLevel serverLevel) {

			if (isAncientCityCenterChest(serverLevel, chest)) {

				return ANCIENT_CITY_CENTER;

			}

			// One-shot partner check only (no recursion back to this chest).
			ChestBlockEntity partner = DoubleChestHelper.findPartner(serverLevel, chest).orElse(null);

			if (partner != null && isAncientCityCenterChest(serverLevel, partner)) {

				return ANCIENT_CITY_CENTER;

			}

		}

		return null;

	}



	@Nullable

	public static ResourceKey<LootTable> resolveLootTable(ChestBlockEntity first, ChestBlockEntity second) {

		ResourceKey<LootTable> table = resolveLootTable(first);

		return table != null ? table : resolveLootTable(second);

	}



	public static long resolveLootSeed(ChestBlockEntity first, ChestBlockEntity second) {

		if (resolveDeclaredLootTable(first) != null) {

			return resolveLootSeed(first, first.getBlockPos());

		}

		if (resolveDeclaredLootTable(second) != null) {

			return resolveLootSeed(second, second.getBlockPos());

		}

		return resolveLootSeed(first, DoubleChestHelper.canonicalPos(first.getBlockPos(), second.getBlockPos()));

	}



	@Nullable

	private static ResourceKey<LootTable> resolveDeclaredLootTable(BlockEntity blockEntity) {

		if (blockEntity == null) {

			return null;

		}

		if (blockEntity instanceof RandomizableContainerBlockEntity randomizable) {

			ResourceKey<LootTable> table = randomizable.getLootTable();

			if (table != null) {

				return table;

			}

		}

		SeededContainerLoot containerLoot = readContainerLoot(blockEntity);

		return containerLoot != null ? containerLoot.lootTable() : null;

	}



	@Nullable

	private static SeededContainerLoot readContainerLoot(BlockEntity blockEntity) {

		if (blockEntity == null) {

			return null;

		}

		return blockEntity.components().get(DataComponents.CONTAINER_LOOT);

	}



	public static long resolveLootSeed(BlockEntity blockEntity, BlockPos pos) {

		if (blockEntity == null) {

			return pos.asLong();

		}

		if (blockEntity instanceof RandomizableContainerBlockEntity randomizable) {

			long seed = randomizable.getLootTableSeed();

			if (seed != 0L) {

				return seed;

			}

		}

		SeededContainerLoot containerLoot = readContainerLoot(blockEntity);

		if (containerLoot != null && containerLoot.seed() != 0L) {

			return containerLoot.seed();

		}

		return pos.asLong();

	}



	public static boolean isAncientCityCenterChest(ServerLevel level, BlockEntity blockEntity) {

		if (!(blockEntity instanceof ChestBlockEntity chest)) {

			return false;

		}

		if (!isStructureEnabled(level, "ancient_city")) {

			return false;

		}

		if (resolveDeclaredLootTable(blockEntity) != null) {

			pruneInvalidCenterChestRegistration(level, chest);

			return false;

		}

		if (!level.getBiome(blockEntity.getBlockPos()).is(BiomeTags.HAS_ANCIENT_CITY)) {

			pruneInvalidCenterChestRegistration(level, chest);

			return false;

		}

		if (matchesStructurePlacedCenterChest(chest)) {

			return true;

		}

		BlockPos pos = blockEntity.getBlockPos();

		if (!AncientCityCenterChestTracker.get(level).isRegistered(level, pos)) {

			return false;

		}

		// Registered after migration: block inventory is cleared but still no loot table.

		if (storedItems(chest).stream().allMatch(ItemStack::isEmpty)) {

			return true;

		}

		// Stale registration from older broad detection — regular loot chest misclassified.

		AncientCityCenterChestTracker.get(level).unregister(level, pos);

		return false;

	}



	private static void pruneInvalidCenterChestRegistration(ServerLevel level, ChestBlockEntity chest) {

		if (AncientCityCenterChestTracker.get(level).isRegistered(level, chest.getBlockPos())) {

			AncientCityCenterChestTracker.get(level).unregister(level, chest.getBlockPos());

		}

	}



	private static boolean matchesStructurePlacedCenterChest(ChestBlockEntity chest) {

		NonNullList<ItemStack> items = storedItems(chest);

		ItemStack centerStack = items.get(ANCIENT_CITY_CENTER_APPLE_SLOT);

		if (!centerStack.is(Items.GOLDEN_APPLE) || centerStack.getCount() != 1) {

			return false;

		}

		for (int slot = 0; slot < items.size(); slot++) {

			if (slot == ANCIENT_CITY_CENTER_APPLE_SLOT) {

				continue;

			}

			if (!items.get(slot).isEmpty()) {

				return false;

			}

		}

		return true;

	}



	/** Reads slot contents without triggering {@code unpackLootTable} via {@link ChestBlockEntity#getItem}. */

	private static NonNullList<ItemStack> storedItems(ChestBlockEntity chest) {

		return ((BaseContainerBlockEntityAccessor) chest).fairstructureloot$getItems();

	}



	public static void registerAncientCityCenterChest(ServerLevel level, BlockPos pos) {

		AncientCityCenterChestTracker.get(level).register(level, pos);

	}



	public static void migrateAncientCityCenterChest(ServerLevel level, ChestBlockEntity chest) {

		BlockPos pos = chest.getBlockPos();

		registerAncientCityCenterChest(level, pos);

		NonNullList<ItemStack> items = storedItems(chest);

		boolean hasStructureLoot = false;

		for (int slot = 0; slot < items.size(); slot++) {

			if (!items.get(slot).isEmpty()) {

				hasStructureLoot = true;

				break;

			}

		}

		if (hasStructureLoot) {

			chest.clearContent();

		}

	}



	public static boolean isFairLootChest(Level level, BlockEntity blockEntity) {

		if (blockEntity == null) {

			return false;

		}

		return markerFor(level, blockEntity).fairLoot();

	}



	/** Elytra and ancient city center chests ignore shared-roll orange state (green/black only). */

	public static boolean usesPerPlayerIndicator(@Nullable ResourceKey<LootTable> lootTable) {

		if (lootTable == null) {

			return false;

		}

		if (lootTable == ANCIENT_CITY_CENTER || lootTable == END_SHIP_ELYTRA) {

			return true;

		}

		return StructureGroupRegistry.groupForLootTable(lootTable.identifier())

			.map(StructureGroup::perPlayerIndicator)

			.orElse(false);

	}



	public static boolean isStructureEnabled(Level level, String commandName) {

		if (level instanceof ServerLevel serverLevel) {

			return StructureActivationSavedData.get(serverLevel).isEnabled(commandName);

		}

		return StructureActivationAccess.get().isEnabled(commandName);

	}

}

