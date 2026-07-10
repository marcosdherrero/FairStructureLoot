package net.berkle.fairstructureloot.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import net.berkle.fairstructureloot.loot.InstancedLootOpener;
import net.berkle.fairstructureloot.loot.StructureLootTables;

/** Instanced loot for double chests (vanilla opens these via {@code ChestBlock$2$1}, not block-entity {@code createMenu}). */
@Mixin(targets = "net.minecraft.world.level.block.ChestBlock$2$1")
public abstract class ChestBlockDoubleMenuMixin {

	@Shadow
	@Final
	private ChestBlockEntity val$first;

	@Shadow
	@Final
	private ChestBlockEntity val$second;

	@Inject(
		method = "createMenu(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/inventory/AbstractContainerMenu;",
		at = @At("HEAD"),
		cancellable = true
	)
	private void fairstructureloot$createInstancedDoubleMenu(
		int syncId,
		Inventory inventory,
		Player player,
		CallbackInfoReturnable<AbstractContainerMenu> cir
	) {
		if (!(player instanceof ServerPlayer serverPlayer) || !(val$first.getLevel() instanceof ServerLevel serverLevel)) {
			return;
		}
		if (!StructureLootTables.isFairLootChest(serverLevel, val$first)
			&& !StructureLootTables.isFairLootChest(serverLevel, val$second)) {
			return;
		}

		AbstractContainerMenu menu = InstancedLootOpener.createInstancedDoubleMenu(
			serverPlayer,
			val$first,
			val$second,
			syncId,
			inventory
		);
		if (menu != null) {
			cir.setReturnValue(menu);
			cir.cancel();
		}
	}
}
