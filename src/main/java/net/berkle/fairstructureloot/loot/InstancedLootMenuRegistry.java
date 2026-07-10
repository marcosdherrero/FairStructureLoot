package net.berkle.fairstructureloot.loot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.level.Level;

/**
 * Tracks which players currently have instanced menus open at a container position.
 * Used by {@link net.berkle.fairstructureloot.mixin.ContainerOpenersCounterMixin} for lid animation.
 */
public final class InstancedLootMenuRegistry {

	private static final Map<String, Set<UUID>> OPEN_PLAYERS = new ConcurrentHashMap<>();

	private InstancedLootMenuRegistry() {
	}

	public static void register(ServerLevel level, BlockPos pos, ServerPlayer player) {
		OPEN_PLAYERS.computeIfAbsent(ContainerKeys.containerKey(level, pos), key -> ConcurrentHashMap.newKeySet())
			.add(player.getUUID());
	}

	public static void unregister(Level level, BlockPos pos, ServerPlayer player) {
		String key = ContainerKeys.containerKey(level, pos);
		Set<UUID> openers = OPEN_PLAYERS.get(key);
		if (openers == null) {
			return;
		}
		openers.remove(player.getUUID());
		if (openers.isEmpty()) {
			OPEN_PLAYERS.remove(key);
		}
	}

	/** Resolves opener UUIDs to online players without scanning the full player list. */
	public static List<ContainerUser> getOpeners(Level level, BlockPos pos) {
		Set<UUID> openers = openersAt(level, pos);
		if ((openers == null || openers.isEmpty()) && level instanceof ServerLevel serverLevel) {
			BlockPos storagePos = storagePosForLookup(serverLevel, pos);
			if (!storagePos.equals(pos)) {
				openers = openersAt(level, storagePos);
			}
		}
		if (openers == null || openers.isEmpty() || !(level instanceof ServerLevel serverLevel)) {
			return List.of();
		}
		List<ContainerUser> users = new ArrayList<>(openers.size());
		for (UUID openerId : openers) {
			ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(openerId);
			if (player != null) {
				users.add(player);
			}
		}
		return users;
	}

	private static Set<UUID> openersAt(Level level, BlockPos pos) {
		return OPEN_PLAYERS.get(ContainerKeys.containerKey(level, pos));
	}

	private static BlockPos storagePosForLookup(ServerLevel level, BlockPos pos) {
		var blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
			return DoubleChestHelper.storagePos(level, chest);
		}
		return pos;
	}
}
