package net.berkle.fairstructureloot.client;

import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import net.berkle.fairstructureloot.loot.StructureLootTables;

/**
 * Resolves fair-loot HUD indicator state for the block under the crosshair.
 * Caches fair-loot detection per block position; opened-state is always fresh.
 */
public final class FairLootIndicatorHelper {

	public enum State {
		/** Not opened by you (and no shared roll yet, if applicable). */
		UNOPENED,
		/** Shared roll: someone else opened; you have not seen your roll yet. */
		OPENED_BY_OTHER,
		/** You opened this container and have your roll. */
		OPENED_BY_YOU
	}

	private static BlockPos cachedFairLootPos = BlockPos.ZERO;
	private static Identifier cachedDimension;
	private static StructureLootTables.FairLootMarker cachedMarker = StructureLootTables.FairLootMarker.NONE;

	private FairLootIndicatorHelper() {
	}

	public static Optional<State> fromCrosshairTarget(Minecraft minecraft) {
		if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
			return Optional.empty();
		}
		if (minecraft.options.hideGui) {
			return Optional.empty();
		}
		HitResult hit = minecraft.hitResult;
		if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
			return Optional.empty();
		}
		BlockPos pos = ((BlockHitResult) hit).getBlockPos();
		BlockEntity blockEntity = minecraft.level.getBlockEntity(pos);
		if (blockEntity == null) {
			return Optional.empty();
		}

		StructureLootTables.FairLootMarker marker = resolveMarker(minecraft.level, pos, blockEntity);
		if (!marker.fairLoot()) {
			return Optional.empty();
		}

		if (OpenedChestCache.isPersonallyOpened(minecraft.level, pos)) {
			return Optional.of(State.OPENED_BY_YOU);
		}
		if (!marker.perPlayerIndicator()
			&& LootRollModeClientCache.isShared()
			&& OpenedChestCache.isGloballyOpened(minecraft.level, pos)) {
			return Optional.of(State.OPENED_BY_OTHER);
		}
		return Optional.of(State.UNOPENED);
	}

	private static StructureLootTables.FairLootMarker resolveMarker(Level level, BlockPos pos, BlockEntity blockEntity) {
		Identifier dimension = level.dimension().identifier();
		if (pos.equals(cachedFairLootPos) && dimension.equals(cachedDimension)) {
			return cachedMarker;
		}

		StructureLootTables.FairLootMarker marker = FairLootClientDetection.resolve(level, pos, blockEntity);
		cachedFairLootPos = pos.immutable();
		cachedDimension = dimension;
		cachedMarker = marker;
		return marker;
	}

	public static void clearCache() {
		cachedFairLootPos = BlockPos.ZERO;
		cachedDimension = null;
		cachedMarker = StructureLootTables.FairLootMarker.NONE;
	}
}
