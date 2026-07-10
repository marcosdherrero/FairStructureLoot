package net.berkle.fairstructureloot.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import net.berkle.fairstructureloot.loot.StructureLootTables;

/** Cancels vanilla shared loot fill so per-player generation happens on first menu open. */
@Mixin(RandomizableContainer.class)
public interface RandomizableContainerBlockEntityMixin {

	@Inject(method = "unpackLootTable", at = @At("HEAD"), cancellable = true)
	default void fairstructureloot$skipSharedLootFill(@Nullable Player player, CallbackInfo ci) {
		RandomizableContainer container = (RandomizableContainer) (Object) this;
		Level level = container.getLevel();
		if (container instanceof BlockEntity blockEntity
			&& level instanceof ServerLevel serverLevel
			&& StructureLootTables.isFairLootChest(serverLevel, blockEntity)) {
			ci.cancel();
		}
	}
}
