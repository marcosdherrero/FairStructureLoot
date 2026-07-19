package net.berkle.fairstructureloot.events;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.Level;

import net.berkle.fairstructureloot.FairStructureLootMain;
import net.berkle.fairstructureloot.conversion.ElytraConversionHandler;
import net.berkle.fairstructureloot.loot.FairLootChunkScanQueue;
import net.berkle.fairstructureloot.loot.InstancedChestScanner;
import net.berkle.fairstructureloot.network.FairStructureLootNetworking;

/** Registers join sync, chunk-load scans, break handlers, and End elytra conversion. */
public final class ModServerEvents {

	/**
	 * Wait after JOIN before any fair-loot SavedData / packet work.
	 * Login + first chunk burst must finish first (Watchdog: 60s tick on unpause).
	 */
	private static final int JOIN_SYNC_DELAY_TICKS = 100;

	private static final Map<UUID, ResourceKey<Level>> LAST_PLAYER_DIMENSION = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> PENDING_JOIN_SYNC = new ConcurrentHashMap<>();

	private ModServerEvents() {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.player;
			LAST_PLAYER_DIMENSION.put(player.getUUID(), player.level().dimension());
			// Do not touch SavedData or send payloads during JOIN — unpause + chunk load
			// already stress the server thread; deferred sync runs after login settles.
			PENDING_JOIN_SYNC.put(player.getUUID(), JOIN_SYNC_DELAY_TICKS);
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID id = handler.getPlayer().getUUID();
			LAST_PLAYER_DIMENSION.remove(id);
			PENDING_JOIN_SYNC.remove(id);
		});

		PlayerBlockBreakEvents.BEFORE.register(LootChestBreakHandler::onBlockBreakBefore);
		PlayerBlockBreakEvents.AFTER.register(LootChestBreakHandler::onBlockBreakAfter);
		PlayerBlockBreakEvents.CANCELED.register(LootChestBreakHandler::onBlockBreakCanceled);

		ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, level) -> {
			if (level instanceof ServerLevel serverLevel) {
				LootChestBreakHandler.onBlockEntityUnload(serverLevel, blockEntity);
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			LootChestBreakHandler.clearHandledContainerKeys();
			FairLootChunkScanQueue.tick(server);
			runPendingJoinSync(server);
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				ResourceKey<Level> current = player.level().dimension();
				ResourceKey<Level> previous = LAST_PLAYER_DIMENSION.put(player.getUUID(), current);
				if (previous != null && !previous.equals(current) && current.equals(Level.END)) {
					ElytraConversionHandler.scanPlayerView(player);
				}
			}
		});

		ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> {
			if (level instanceof ServerLevel serverLevel) {
				// Queue only — never scan inline on CHUNK_LOAD (C2ME / unpause floods).
				FairLootChunkScanQueue.enqueue(serverLevel, chunk);
			}
		});

		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (world instanceof ServerLevel serverLevel && entity instanceof ItemFrame frame) {
				ElytraConversionHandler.tryConvert(serverLevel, frame);
			}
		});
	}

	private static void runPendingJoinSync(net.minecraft.server.MinecraftServer server) {
		if (PENDING_JOIN_SYNC.isEmpty()) {
			return;
		}
		Iterator<Map.Entry<UUID, Integer>> iterator = PENDING_JOIN_SYNC.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, Integer> entry = iterator.next();
			int remaining = entry.getValue() - 1;
			if (remaining > 0) {
				entry.setValue(remaining);
				continue;
			}
			iterator.remove();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null || player.hasDisconnected()) {
				continue;
			}
			long started = System.nanoTime();
			try {
				FairStructureLootNetworking.syncLootRollMode(player);
				FairStructureLootNetworking.syncStructureActivation(player);
				FairStructureLootNetworking.syncOpenedChests(player);
				FairStructureLootNetworking.syncGloballyOpenedChests(player);
				InstancedChestScanner.syncLoadedChunks(player);
				ElytraConversionHandler.scanPlayerView(player);
			} catch (RuntimeException ex) {
				FairStructureLootMain.LOGGER.error(
					"[{}] Failed deferred join sync for {}",
					FairStructureLootMain.MOD_ID,
					player.getGameProfile().name(),
					ex
				);
			}
			long ms = (System.nanoTime() - started) / 1_000_000L;
			if (ms >= 250L) {
				FairStructureLootMain.LOGGER.warn(
					"[{}] Deferred join sync for {} took {} ms",
					FairStructureLootMain.MOD_ID,
					player.getGameProfile().name(),
					ms
				);
			}
		}
	}
}
