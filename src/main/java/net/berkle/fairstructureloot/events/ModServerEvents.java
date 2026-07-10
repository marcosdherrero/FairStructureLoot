package net.berkle.fairstructureloot.events;

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

import net.berkle.fairstructureloot.conversion.ElytraConversionHandler;
import net.berkle.fairstructureloot.loot.InstancedChestScanner;
import net.berkle.fairstructureloot.network.FairStructureLootNetworking;

/** Registers join sync, chunk-load scans, break handlers, and End elytra conversion. */
public final class ModServerEvents {

	private static final Map<UUID, ResourceKey<Level>> LAST_PLAYER_DIMENSION = new ConcurrentHashMap<>();

	private ModServerEvents() {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			FairStructureLootNetworking.syncLootRollMode(handler.player);
			FairStructureLootNetworking.syncStructureActivation(handler.player);
			FairStructureLootNetworking.syncOpenedChests(handler.player);
			FairStructureLootNetworking.syncGloballyOpenedChests(handler.player);
			InstancedChestScanner.syncLoadedChunks(handler.player);
			ElytraConversionHandler.scanPlayerView(handler.player);
			LAST_PLAYER_DIMENSION.put(handler.player.getUUID(), handler.player.level().dimension());
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
			LAST_PLAYER_DIMENSION.remove(handler.getPlayer().getUUID())
		);

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
				InstancedChestScanner.scanChunk(serverLevel, chunk);
				if (level.dimension() == Level.END) {
					ElytraConversionHandler.scheduleChunkScan(serverLevel, chunk);
				}
			}
		});

		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (world instanceof ServerLevel serverLevel && entity instanceof ItemFrame frame) {
				ElytraConversionHandler.tryConvert(serverLevel, frame);
			}
		});
	}
}
