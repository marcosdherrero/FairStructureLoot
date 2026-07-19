package net.berkle.fairstructureloot.loot;

import java.util.ArrayDeque;
import java.util.Queue;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import net.berkle.fairstructureloot.conversion.ElytraConversionHandler;

/**
 * Spreads fair-loot chunk scans across ticks so a mass CHUNK_LOAD burst (server unpause /
 * first join) cannot stall the server thread for tens of seconds.
 */
public final class FairLootChunkScanQueue {

	private static final int MAX_SCANS_PER_TICK = 4;

	private record Pending(ServerLevel level, ChunkPos pos, boolean endElytra) {
	}

	private static final Queue<Pending> QUEUE = new ArrayDeque<>();

	private FairLootChunkScanQueue() {
	}

	public static void enqueue(ServerLevel level, LevelChunk chunk) {
		ChunkPos pos = chunk.getPos();
		boolean end = level.dimension() == net.minecraft.world.level.Level.END;
		QUEUE.offer(new Pending(level, pos, end));
	}

	public static void tick(MinecraftServer server) {
		int budget = MAX_SCANS_PER_TICK;
		while (budget-- > 0) {
			Pending pending = QUEUE.poll();
			if (pending == null) {
				return;
			}
			if (server.getLevel(pending.level().dimension()) != pending.level()) {
				continue;
			}
			LevelChunk chunk = pending.level().getChunkSource().getChunkNow(pending.pos().x(), pending.pos().z());
			if (chunk == null) {
				continue;
			}
			InstancedChestScanner.scanChunk(pending.level(), chunk);
			if (pending.endElytra()) {
				ElytraConversionHandler.scheduleChunkScan(pending.level(), chunk);
			}
		}
	}

	public static void clear() {
		QUEUE.clear();
	}
}
