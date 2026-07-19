package net.berkle.fairstructureloot.client;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Client interaction: skip empty shared chest GUI for fair-loot containers, and show the
 * sneak-to-break hint as soon as the player starts mining one without holding Shift.
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

		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
			if (!level.isClientSide() || player.isShiftKeyDown()) {
				return InteractionResult.PASS;
			}
			BlockEntity blockEntity = level.getBlockEntity(pos);
			if (blockEntity == null || !FairLootClientDetection.isFairLoot(level, pos, blockEntity)) {
				return InteractionResult.PASS;
			}
			FairLootBreakHint.show();
			return InteractionResult.PASS;
		});
	}
}
