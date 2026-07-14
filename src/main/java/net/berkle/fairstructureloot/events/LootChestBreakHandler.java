package net.berkle.fairstructureloot.events;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.storage.loot.LootTable;

import net.berkle.fairstructureloot.loot.ContainerKeys;
import net.berkle.fairstructureloot.loot.DoubleChestHelper;
import net.berkle.fairstructureloot.loot.InstancedLootSavedData;
import net.berkle.fairstructureloot.loot.InstancedVaultLoot;
import net.berkle.fairstructureloot.loot.StructureLootTables;
import net.berkle.fairstructureloot.network.FairStructureLootNetworking;
import net.berkle.fairstructureloot.network.FairStructureLootNetworking;

/**
 * When a fair-loot container is destroyed (break, explosion, piston, etc.), drops one player's
 * instanced loot and clears persistence plus client position sync.
 */
public final class LootChestBreakHandler {

	private static final ThreadLocal<String> PENDING_CONTAINER_KEY = new ThreadLocal<>();
	private static final ThreadLocal<ServerPlayer> PENDING_BREAKING_PLAYER = new ThreadLocal<>();
	private static final Set<String> HANDLED_CONTAINER_KEYS = ConcurrentHashMap.newKeySet();
	/** Minimum ticks between break-hint toasts per player (prevents spam while holding break). */
	private static final int HINT_COOLDOWN_TICKS = 40;
	private static final Map<UUID, Long> LAST_HINT_TICK = new ConcurrentHashMap<>();

	private record ContainerContext(
		BlockPos storagePos,
		ResourceKey<LootTable> lootTable,
		long lootSeed,
		int size
	) {
	}

	private LootChestBreakHandler() {
	}

	public static boolean onBlockBreakBefore(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
			return true;
		}
		Optional<ContainerContext> context = resolveContext(serverLevel, blockEntity);
		if (context.isEmpty()) {
			return true;
		}
		if (player instanceof ServerPlayer serverPlayer && !serverPlayer.isCrouching()) {
			long tick = serverLevel.getGameTime();
			Long lastHint = LAST_HINT_TICK.get(serverPlayer.getUUID());
			if (lastHint == null || tick - lastHint >= HINT_COOLDOWN_TICKS) {
				LAST_HINT_TICK.put(serverPlayer.getUUID(), tick);
				FairStructureLootNetworking.sendBreakHint(serverPlayer);
			}
			return false;
		}
		PENDING_CONTAINER_KEY.set(ContainerKeys.containerKey(serverLevel, context.get().storagePos()));
		if (player instanceof ServerPlayer serverPlayer) {
			PENDING_BREAKING_PLAYER.set(serverPlayer);
		}
		return true;
	}

	public static void onBlockBreakAfter(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		String containerKey = PENDING_CONTAINER_KEY.get();
		ServerPlayer breakingPlayer = PENDING_BREAKING_PLAYER.get();
		PENDING_CONTAINER_KEY.remove();
		PENDING_BREAKING_PLAYER.remove();
		if (containerKey == null || level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
			return;
		}
		Optional<ContainerContext> context = resolveContext(serverLevel, blockEntity);
		if (context.isEmpty()) {
			return;
		}
		if (!ContainerKeys.containerKey(serverLevel, context.get().storagePos()).equals(containerKey)) {
			return;
		}
		onDestroyed(serverLevel, blockEntity, context.get(), breakingPlayer);
	}

	public static void onBlockBreakCanceled(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		PENDING_CONTAINER_KEY.remove();
		PENDING_BREAKING_PLAYER.remove();
	}

	public static void onBlockEntityUnload(ServerLevel level, BlockEntity blockEntity) {
		BlockPos pos = blockEntity.getBlockPos();
		ChunkPos chunkPos = ChunkPos.containing(pos);
		// Use the in-memory chunk only — level.getBlockState() can block-load chunks during
		// unload (e.g. world save / quit) and deadlock with async chunk systems like C2ME.
		if (!level.hasChunk(chunkPos.x(), chunkPos.z())) {
			return;
		}
		ChunkAccess chunk = level.getChunkSource().getChunkNow(chunkPos.x(), chunkPos.z());
		if (chunk == null) {
			return;
		}
		if (!chunk.getBlockState(pos).isAir()) {
			return;
		}
		onDestroyed(level, blockEntity, null, null);
	}

	public static void clearHandledContainerKeys() {
		HANDLED_CONTAINER_KEYS.clear();
	}

	private static void onDestroyed(
		ServerLevel level,
		BlockEntity blockEntity,
		ContainerContext knownContext,
		ServerPlayer breakingPlayer
	) {
		ContainerContext context = knownContext != null
			? knownContext
			: resolveContext(level, blockEntity).orElse(null);
		if (context == null) {
			return;
		}
		String key = ContainerKeys.containerKey(level, context.storagePos());
		if (!HANDLED_CONTAINER_KEYS.add(key)) {
			return;
		}

		ServerPlayer targetPlayer = breakingPlayer != null
			? breakingPlayer
			: PENDING_BREAKING_PLAYER.get();
		if (targetPlayer == null) {
			targetPlayer = findClosestPlayer(level, context.storagePos());
		}

		InstancedLootSavedData.get(level).destroyContainer(
			level,
			context.storagePos(),
			targetPlayer,
			context.lootTable(),
			context.lootSeed(),
			context.size()
		);
		FairStructureLootNetworking.clearFairLootMarkers(level, context.storagePos(), blockEntity);
	}

	private static ServerPlayer findClosestPlayer(ServerLevel level, BlockPos pos) {
		ServerPlayer closest = null;
		double closestDistSq = Double.MAX_VALUE;
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 0.5;
		double z = pos.getZ() + 0.5;
		for (ServerPlayer player : level.players()) {
			if (player.level() != level) {
				continue;
			}
			double distSq = player.distanceToSqr(x, y, z);
			if (distSq < closestDistSq) {
				closestDistSq = distSq;
				closest = player;
			}
		}
		return closest;
	}

	private static Optional<ContainerContext> resolveContext(ServerLevel level, BlockEntity blockEntity) {
		if (blockEntity == null || !StructureLootTables.isFairLootChest(level, blockEntity)) {
			return Optional.empty();
		}

		BlockPos pos = blockEntity.getBlockPos();
		ResourceKey<LootTable> lootTable = StructureLootTables.resolveLootTable(blockEntity);
		long lootSeed;
		int size;
		BlockPos storagePos;

		if (blockEntity instanceof ChestBlockEntity chest) {
			if (StructureLootTables.isAncientCityCenterChest(level, chest)) {
				StructureLootTables.migrateAncientCityCenterChest(level, chest);
			}
			Optional<DoubleChestHelper.ChestPair> pair = DoubleChestHelper.findPair(level, chest);
			if (pair.isPresent()) {
				DoubleChestHelper.ChestPair connected = pair.get();
				storagePos = connected.storagePos();
				lootTable = StructureLootTables.resolveLootTable(connected.first(), connected.second());
				lootSeed = StructureLootTables.resolveLootSeed(connected.first(), connected.second());
				size = connected.containerSize();
			} else {
				storagePos = chest.getBlockPos();
				lootSeed = StructureLootTables.resolveLootSeed(chest, pos);
				size = chest.getContainerSize();
			}
		} else if (blockEntity instanceof VaultBlockEntity vault) {
			storagePos = pos;
			lootTable = vault.getConfig().lootTable();
			lootSeed = pos.asLong();
			size = InstancedVaultLoot.VAULT_ROLL_SLOTS;
		} else if (blockEntity instanceof RandomizableContainerBlockEntity randomizable) {
			storagePos = pos;
			lootSeed = StructureLootTables.resolveLootSeed(blockEntity, pos);
			size = randomizable.getContainerSize();
		} else {
			return Optional.empty();
		}

		if (lootTable == null || !StructureLootTables.isInstanced(level, lootTable)) {
			return Optional.empty();
		}
		return Optional.of(new ContainerContext(storagePos, lootTable, lootSeed, size));
	}
}
