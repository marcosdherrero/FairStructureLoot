package net.berkle.fairstructureloot.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.berkle.fairstructureloot.loot.StructureLootTables;

/** Resolves fair-loot HUD state on the client (server cache first, loot-table fallback). */
public final class FairLootClientDetection {

	private FairLootClientDetection() {
	}

	public static StructureLootTables.FairLootMarker resolve(Level level, BlockPos pos, BlockEntity blockEntity) {
		if (FairLootChestClientCache.isFairLoot(level, pos)) {
			return new StructureLootTables.FairLootMarker(
				true,
				FairLootChestClientCache.isPerPlayerIndicator(level, pos)
			);
		}
		return StructureLootTables.markerFor(level, blockEntity);
	}

	public static boolean isFairLoot(Level level, BlockPos pos, BlockEntity blockEntity) {
		return resolve(level, pos, blockEntity).fairLoot();
	}
}
