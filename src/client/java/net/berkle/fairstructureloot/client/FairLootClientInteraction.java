package net.berkle.fairstructureloot.client;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Stops the client from opening the shared block-entity chest GUI for fair-loot containers.
 * Vanilla unpack is skipped on the server, so the client-side block inventory is always empty.
 */
public final class FairLootClientInteraction {

	private FairLootClientInteraction() {
	}

	public static void register() {
		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
			if (!level.isClientSide()) {
				return InteractionResult.PASS;
			}

			BlockEntity blockEntity = level.getBlockEntity(hitResult.getBlockPos());
			if (blockEntity == null) {
				return InteractionResult.PASS;
			}

			if (!FairLootClientDetection.isFairLoot(level, hitResult.getBlockPos(), blockEntity)) {
				return InteractionResult.PASS;
			}

			return InteractionResult.SUCCESS;
		});
	}
}
