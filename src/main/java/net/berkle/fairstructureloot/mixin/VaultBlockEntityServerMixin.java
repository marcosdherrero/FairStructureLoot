package net.berkle.fairstructureloot.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultConfig;
import net.minecraft.world.level.storage.loot.LootTable;

import net.berkle.fairstructureloot.loot.InstancedVaultLoot;
import net.berkle.fairstructureloot.loot.StructureLootTables;

/** Replaces shared vault loot rolls with per-player (or shared-mode) instanced generation. */
@Mixin(VaultBlockEntity.Server.class)
public abstract class VaultBlockEntityServerMixin {

	@Inject(
		method = "resolveItemsToEject",
		at = @At("HEAD"),
		cancellable = true
	)
	private static void fairstructureloot$resolveItemsToEject(
		ServerLevel level,
		VaultConfig config,
		BlockPos pos,
		Player player,
		ItemInstance tool,
		CallbackInfoReturnable<List<ItemStack>> cir
	) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		ResourceKey<LootTable> lootTable = config.lootTable();
		if (!StructureLootTables.isInstanced(level, lootTable)) {
			return;
		}
		cir.setReturnValue(InstancedVaultLoot.resolveItemsToEject(level, serverPlayer, pos, lootTable));
	}
}
