package net.berkle.fairstructureloot.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;

import net.berkle.fairstructureloot.loot.InstancedLootOpener;
import net.berkle.fairstructureloot.loot.StructureLootTables;

/** Redirects right-click menu creation to {@link InstancedLootOpener} for fair-loot containers. */
@Mixin(RandomizableContainerBlockEntity.class)
public abstract class RandomizableContainerBlockEntityMenuMixin {

	@Inject(
		method = "createMenu(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/inventory/AbstractContainerMenu;",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/entity/RandomizableContainerBlockEntity;createMenu(ILnet/minecraft/world/entity/player/Inventory;)Lnet/minecraft/world/inventory/AbstractContainerMenu;",
			shift = At.Shift.BEFORE
		),
		cancellable = true
	)
	private void fairstructureloot$createInstancedMenu(
		int syncId,
		Inventory inventory,
		Player player,
		CallbackInfoReturnable<net.minecraft.world.inventory.AbstractContainerMenu> cir
	) {
		RandomizableContainerBlockEntity blockEntity = (RandomizableContainerBlockEntity) (Object) this;
		if (!(blockEntity.getLevel() instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		if (!StructureLootTables.isFairLootChest(serverLevel, blockEntity)) {
			return;
		}

		cir.setReturnValue(InstancedLootOpener.createInstancedMenu(serverPlayer, blockEntity, syncId, inventory));
		cir.cancel();
	}
}
