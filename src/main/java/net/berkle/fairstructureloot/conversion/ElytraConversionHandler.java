package net.berkle.fairstructureloot.conversion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import net.berkle.fairstructureloot.FairStructureLootMain;
import net.berkle.fairstructureloot.loot.InstancedChestScanner;
import net.berkle.fairstructureloot.loot.StructureActivationSavedData;
import net.berkle.fairstructureloot.loot.StructureLootTables;

/** Replaces End ship elytra item frames with instanced loot barrels (one elytra per player). */
public final class ElytraConversionHandler {

	public static final String GROUP_COMMAND = "end_ship";

	private static final int SHIP_CHEST_SEARCH_RADIUS = 10;

	private ElytraConversionHandler() {
	}

	public static void tryConvert(ServerLevel level, ItemFrame frame) {
		if (!frame.isAlive() || frame.isRemoved()) {
			return;
		}
		if (!level.dimension().equals(Level.END)) {
			return;
		}
		if (frame.getItem().isEmpty() || !frame.getItem().is(Items.ELYTRA)) {
			return;
		}
		if (!StructureActivationSavedData.get(level).isEnabled(GROUP_COMMAND)) {
			return;
		}

		BlockPos framePos = frame.blockPosition();
		Direction intoRoom = frame.getDirection();
		BlockPos barrelPos = findBetweenShipChests(level, framePos)
			.orElseGet(() -> framePos.relative(intoRoom));

		frame.discard();

		if (!level.getBlockState(barrelPos).canBeReplaced()) {
			level.destroyBlock(barrelPos, true);
		}

		Direction barrelFacing = directionToward(framePos, barrelPos, intoRoom);
		BlockState barrelState = Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, barrelFacing);
		level.setBlock(barrelPos, barrelState, 3);

		BlockEntity blockEntity = level.getBlockEntity(barrelPos);
		if (blockEntity instanceof RandomizableContainerBlockEntity container) {
			container.setLootTable(StructureLootTables.END_SHIP_ELYTRA, barrelPos.asLong());
			container.setChanged();
		}

		InstancedChestScanner.notifyFairLootContainer(
			level,
			barrelPos,
			new StructureLootTables.FairLootMarker(true, true)
		);

		FairStructureLootMain.LOGGER.debug(
			"[{}] Converted elytra frame at {} to instanced barrel at {}",
			FairStructureLootMain.MOD_ID,
			framePos,
			barrelPos
		);
	}

	/**
	 * End ship treasure room: two floor chests flank the center tile (structure midpoint is air at y of chests).
	 */
	private static Optional<BlockPos> findBetweenShipChests(ServerLevel level, BlockPos framePos) {
		List<BlockPos> chests = collectNearbyChests(level, framePos, SHIP_CHEST_SEARCH_RADIUS);
		if (chests.size() < 2) {
			return Optional.empty();
		}

		chests.sort(Comparator.comparingDouble(pos -> pos.distSqr(framePos)));

		Optional<BlockPos> best = Optional.empty();
		double bestDistSq = Double.MAX_VALUE;
		for (int i = 0; i < chests.size(); i++) {
			for (int j = i + 1; j < chests.size(); j++) {
				BlockPos first = chests.get(i);
				BlockPos second = chests.get(j);
				Optional<BlockPos> midpoint = midpointBetweenChests(level, framePos, first, second);
				if (midpoint.isEmpty()) {
					continue;
				}
				double distSq = midpoint.get().distSqr(framePos);
				if (distSq < bestDistSq) {
					bestDistSq = distSq;
					best = midpoint;
				}
			}
		}
		return best;
	}

	private static List<BlockPos> collectNearbyChests(ServerLevel level, BlockPos center, int radius) {
		List<BlockPos> chests = new ArrayList<>();
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dy = -3; dy <= 3; dy++) {
				for (int dz = -radius; dz <= radius; dz++) {
					cursor.setWithOffset(center, dx, dy, dz);
					if (!level.getBlockState(cursor).is(Blocks.CHEST)) {
						continue;
					}
					chests.add(cursor.immutable());
				}
			}
		}
		return chests;
	}

	private static Optional<BlockPos> midpointBetweenChests(
		ServerLevel level,
		BlockPos framePos,
		BlockPos first,
		BlockPos second
	) {
		if (first.getY() != second.getY()) {
			return Optional.empty();
		}
		int deltaX = Math.abs(first.getX() - second.getX());
		int deltaZ = Math.abs(first.getZ() - second.getZ());
		if (deltaX > 4 || deltaZ > 4 || (deltaX == 0 && deltaZ == 0)) {
			return Optional.empty();
		}
		if (deltaX > 0 && deltaZ > 0) {
			return Optional.empty();
		}

		BlockPos midpoint = new BlockPos(
			(first.getX() + second.getX()) / 2,
			first.getY(),
			(first.getZ() + second.getZ()) / 2
		);
		if (midpoint.distSqr(framePos) > (double) SHIP_CHEST_SEARCH_RADIUS * SHIP_CHEST_SEARCH_RADIUS) {
			return Optional.empty();
		}
		BlockState state = level.getBlockState(midpoint);
		if (!state.canBeReplaced() && !state.is(Blocks.STRUCTURE_BLOCK)) {
			return Optional.empty();
		}
		return Optional.of(midpoint);
	}

	private static Direction directionToward(BlockPos from, BlockPos to, Direction fallback) {
		BlockPos delta = from.subtract(to);
		Direction nearest = Direction.getNearest(delta.getX(), delta.getY(), delta.getZ(), fallback);
		return nearest.getAxis().isHorizontal() ? nearest : fallback;
	}

	public static void scanChunk(ServerLevel level, LevelChunk chunk) {
		if (!level.dimension().equals(Level.END)) {
			return;
		}
		ChunkPos chunkPos = chunk.getPos();
		AABB searchBox = new AABB(
			chunkPos.getMinBlockX(),
			level.getMinY(),
			chunkPos.getMinBlockZ(),
			chunkPos.getMaxBlockX() + 1,
			level.getMaxY(),
			chunkPos.getMaxBlockZ() + 1
		);
		for (ItemFrame frame : level.getEntitiesOfClass(ItemFrame.class, searchBox, frame -> true)) {
			tryConvert(level, frame);
		}
	}

	public static void scanPlayerView(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level) || !level.dimension().equals(Level.END)) {
			return;
		}
		BlockPos center = player.blockPosition();
		int chunkX = center.getX() >> 4;
		int chunkZ = center.getZ() >> 4;
		int radius = level.getServer().getPlayerList().getViewDistance();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX + dx, chunkZ + dz);
				if (chunk == null) {
					continue;
				}
				scanChunk(level, chunk);
			}
		}
	}

	public static void scheduleChunkScan(ServerLevel level, LevelChunk chunk) {
		level.getServer().execute(() -> scanChunk(level, chunk));
	}
}
