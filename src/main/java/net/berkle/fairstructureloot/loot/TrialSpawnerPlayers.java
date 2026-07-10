package net.berkle.fairstructureloot.loot;

import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;

import net.berkle.fairstructureloot.mixin.TrialSpawnerStateDataAccessor;

/** Resolves trial spawner participants from vanilla {@code detectedPlayers} state. */
public final class TrialSpawnerPlayers {

	private TrialSpawnerPlayers() {
	}

	public static Set<UUID> detectedPlayerIds(TrialSpawner spawner) {
		return ((TrialSpawnerStateDataAccessor) spawner.getStateData()).fairstructureloot$getDetectedPlayers();
	}

	public static ServerPlayer resolveDetectedPlayer(ServerLevel level, TrialSpawner spawner, UUID playerId) {
		if (!detectedPlayerIds(spawner).contains(playerId)) {
			return null;
		}
		ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
		if (player == null || !player.isAlive() || player.level() != level) {
			return null;
		}
		return player;
	}

	public static ServerPlayer resolveOminousTarget(
		ServerLevel level,
		BlockPos spawnerPos,
		TrialSpawner spawner,
		Entity selectedEntity
	) {
		if (selectedEntity instanceof ServerPlayer serverPlayer) {
			ServerPlayer detected = resolveDetectedPlayer(level, spawner, serverPlayer.getUUID());
			if (detected != null) {
				return detected;
			}
		}
		return nearestDetectedPlayer(level, spawnerPos, spawner);
	}

	public static ServerPlayer nearestDetectedPlayer(ServerLevel level, BlockPos spawnerPos, TrialSpawner spawner) {
		double maxDistSq = (double) spawner.getRequiredPlayerRange() * spawner.getRequiredPlayerRange();
		ServerPlayer nearest = null;
		double nearestDist = maxDistSq;
		for (UUID playerId : detectedPlayerIds(spawner)) {
			ServerPlayer player = resolveDetectedPlayer(level, spawner, playerId);
			if (player == null) {
				continue;
			}
			double dist = player.distanceToSqr(spawnerPos.getX() + 0.5D, spawnerPos.getY() + 0.5D, spawnerPos.getZ() + 0.5D);
			if (dist <= maxDistSq && dist < nearestDist) {
				nearestDist = dist;
				nearest = player;
			}
		}
		return nearest;
	}
}
