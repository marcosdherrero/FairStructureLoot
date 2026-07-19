package net.berkle.fairstructureloot.loot;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;

import net.berkle.fairstructureloot.network.FairStructureLootNetworking;
import net.berkle.fairstructureloot.network.SyncFairLootChestsPayload;
import net.berkle.fairstructureloot.loot.StructureLootTables;
import net.berkle.fairstructureloot.loot.StructureLootTables.FairLootMarker;

/**
 * Syncs fair-loot container positions to clients when chunks load or players join.
 * Join sync scans the player's view-distance square of loaded chunks (batched payload).
 */
public final class InstancedChestScanner {

	private InstancedChestScanner() {
	}

	/** Notifies players tracking this chunk about a newly loaded fair-loot container. */
	public static void scanChunk(ServerLevel level, LevelChunk chunk) {
		ChunkPos chunkPos = chunk.getPos();
		for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
			FairLootMarker marker = markerFor(level, blockEntity);
			if (!marker.fairLoot()) {
				continue;
			}
			notifyPlayersWithChunk(level, chunkPos, blockEntity.getBlockPos(), marker);
		}
	}

	/** On join, marks every fair-loot container in the player's loaded view for HUD detection. */
	public static void syncLoadedChunks(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		BlockPos center = player.blockPosition();
		int chunkX = center.getX() >> 4;
		int chunkZ = center.getZ() >> 4;
		int radius = level.getServer().getPlayerList().getViewDistance();
		List<SyncFairLootChestsPayload.Entry> entries = new ArrayList<>();
		Identifier dimension = level.dimension().identifier();

		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX + dx, chunkZ + dz);
				if (chunk == null) {
					continue;
				}
				collectFairLootEntries(level, dimension, chunk, entries);
			}
		}
		FairStructureLootNetworking.syncFairLootChests(player, entries);
	}

	/** Notifies all players tracking a position that a fair-loot container exists (e.g. after elytra conversion). */
	public static void notifyFairLootContainer(ServerLevel level, BlockPos pos, FairLootMarker marker) {
		notifyPlayersWithChunk(level, ChunkPos.containing(pos), pos, marker);
	}

	private static void collectFairLootEntries(
		ServerLevel level,
		Identifier dimension,
		LevelChunk chunk,
		List<SyncFairLootChestsPayload.Entry> entries
	) {
		for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
			FairLootMarker marker = markerFor(level, blockEntity);
			if (!marker.fairLoot()) {
				continue;
			}
			BlockPos pos = blockEntity.getBlockPos().immutable();
			FairLootChestServerTracker.set(
				dimension,
				pos,
				true,
				marker.perPlayerIndicator(),
				StructureLootTables.resolveLootTable(blockEntity)
			);
			entries.add(new SyncFairLootChestsPayload.Entry(
				dimension,
				pos,
				marker.perPlayerIndicator()
			));
		}
	}

	private static void notifyPlayersWithChunk(
		ServerLevel level,
		ChunkPos chunkPos,
		BlockPos pos,
		FairLootMarker marker
	) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		var lootTable = StructureLootTables.resolveLootTable(blockEntity);
		for (ServerPlayer player : level.players()) {
			if (player.level() != level) {
				continue;
			}
			if (player.getChunkTrackingView().contains(chunkPos)) {
				FairStructureLootNetworking.markFairLootChest(
					player,
					level,
					pos,
					true,
					marker.perPlayerIndicator(),
					lootTable
				);
			}
		}
	}

	private static FairLootMarker markerFor(ServerLevel level, BlockEntity blockEntity) {
		maybeRegisterAncientCityCenter(level, blockEntity);
		return StructureLootTables.markerFor(level, blockEntity);
	}

	private static void maybeRegisterAncientCityCenter(ServerLevel level, BlockEntity blockEntity) {
		if (StructureLootTables.isAncientCityCenterChest(level, blockEntity)) {
			StructureLootTables.registerAncientCityCenterChest(level, blockEntity.getBlockPos());
		}
	}
}
