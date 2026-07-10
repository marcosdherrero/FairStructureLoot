package net.berkle.fairstructureloot.network;

import java.util.List;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;

import net.berkle.fairstructureloot.loot.DoubleChestHelper;
import net.berkle.fairstructureloot.loot.InstancedLootSavedData;
import net.berkle.fairstructureloot.loot.LootRollModeSavedData;
import net.berkle.fairstructureloot.loot.StructureActivationSavedData;
import net.berkle.fairstructureloot.loot.StructureLootTables;

/** Server-side registration and send helpers for all clientbound fair-loot payloads. */
public final class FairStructureLootNetworking {

	private FairStructureLootNetworking() {
	}

	public static void registerPayloadTypes() {
		PayloadTypeRegistry.clientboundPlay().register(MarkChestOpenedPayload.TYPE, MarkChestOpenedPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(MarkChestGloballyOpenedPayload.TYPE, MarkChestGloballyOpenedPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(MarkFairLootChestPayload.TYPE, MarkFairLootChestPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SyncOpenedChestsPayload.TYPE, SyncOpenedChestsPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SyncGloballyOpenedChestsPayload.TYPE, SyncGloballyOpenedChestsPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SyncLootRollModePayload.TYPE, SyncLootRollModePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SyncStructureActivationPayload.TYPE, SyncStructureActivationPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SyncFairLootChestsPayload.TYPE, SyncFairLootChestsPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(BreakHintPayload.TYPE, BreakHintPayload.CODEC);
	}

	public static void markFairLootChest(
		ServerPlayer player,
		ServerLevel level,
		BlockPos pos,
		boolean fairLoot,
		boolean perPlayerIndicator
	) {
		ServerPlayNetworking.send(
			player,
			new MarkFairLootChestPayload(level.dimension().identifier(), pos.immutable(), fairLoot, perPlayerIndicator)
		);
	}

	public static void notifyChestOpened(
		ServerPlayer player,
		ServerLevel level,
		BlockPos pos,
		ResourceKey<LootTable> lootTable,
		boolean causedGlobalRoll
	) {
		if (StructureLootTables.usesPerPlayerIndicator(lootTable)) {
			markPersonallyOpened(player, level, pos);
			return;
		}
		if (LootRollModeSavedData.get(level).isShared()) {
			if (causedGlobalRoll) {
				broadcastGloballyOpened(level, pos);
			}
			return;
		}
		markPersonallyOpened(player, level, pos);
	}

	private static void markPersonallyOpened(ServerPlayer player, ServerLevel level, BlockPos pos) {
		ServerPlayNetworking.send(player, new MarkChestOpenedPayload(level.dimension().identifier(), pos.immutable()));
	}

	private static void broadcastGloballyOpened(ServerLevel level, BlockPos pos) {
		MarkChestGloballyOpenedPayload payload = new MarkChestGloballyOpenedPayload(level.dimension().identifier(), pos.immutable());
		for (ServerPlayer player : level.players()) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	public static void syncOpenedChests(ServerPlayer player) {
		List<String> keys = InstancedLootSavedData.get((ServerLevel) player.level())
			.collectOpenedContainerKeysForPlayer(player.getUUID());
		ServerPlayNetworking.send(player, new SyncOpenedChestsPayload(keys));
	}

	public static void syncGloballyOpenedChests(ServerPlayer player) {
		List<String> keys = InstancedLootSavedData.get((ServerLevel) player.level()).collectGloballyOpenedContainerKeys();
		ServerPlayNetworking.send(player, new SyncGloballyOpenedChestsPayload(keys));
	}

	public static void syncLootRollMode(ServerPlayer player) {
		boolean shared = LootRollModeSavedData.get((ServerLevel) player.level()).isShared();
		ServerPlayNetworking.send(player, new SyncLootRollModePayload(shared));
	}

	public static void sendBreakHint(ServerPlayer player) {
		ServerPlayNetworking.send(player, new BreakHintPayload());
	}

	public static void syncStructureActivation(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		ServerPlayNetworking.send(
			player,
			new SyncStructureActivationPayload(StructureActivationSavedData.get(level).exportStates())
		);
	}

	public static void broadcastStructureActivation(ServerLevel level) {
		SyncStructureActivationPayload payload = new SyncStructureActivationPayload(
			StructureActivationSavedData.get(level).exportStates()
		);
		for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	public static void syncFairLootChests(ServerPlayer player, List<SyncFairLootChestsPayload.Entry> entries) {
		if (entries.isEmpty()) {
			return;
		}
		ServerPlayNetworking.send(player, new SyncFairLootChestsPayload(entries));
	}

	public static void clearFairLootMarkers(ServerLevel level, BlockPos storagePos, BlockEntity blockEntity) {
		for (ServerPlayer online : level.players()) {
			markFairLootChest(online, level, storagePos, false, false);
			if (blockEntity instanceof ChestBlockEntity chest) {
				DoubleChestHelper.findPartner(level, chest).ifPresent(partner -> {
					BlockPos partnerPos = partner.getBlockPos();
					if (!partnerPos.equals(storagePos)) {
						markFairLootChest(online, level, partnerPos, false, false);
					}
				});
			}
		}
	}
}
