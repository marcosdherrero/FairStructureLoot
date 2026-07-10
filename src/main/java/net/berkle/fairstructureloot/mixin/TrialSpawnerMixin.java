package net.berkle.fairstructureloot.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.storage.loot.LootTable;

import net.berkle.fairstructureloot.loot.InstancedTrialSpawnerLoot;
import net.berkle.fairstructureloot.loot.StructureLootTables;

/**
 * Replaces shared trial spawner completion rewards with per-player instanced rolls.
 * Vanilla ejects once every 1.5s and removes one detected player per cycle; this mixin
 * rolls for that same player each time {@code ejectReward} is invoked.
 */
@Mixin(TrialSpawner.class)
public abstract class TrialSpawnerMixin {

	@Inject(method = "ejectReward", at = @At("HEAD"), cancellable = true)
	private void fairstructureloot$ejectReward(
		ServerLevel level,
		BlockPos pos,
		ResourceKey<LootTable> lootTable,
		CallbackInfo ci
	) {
		if (!StructureLootTables.isInstanced(level, lootTable)) {
			return;
		}
		TrialSpawner spawner = (TrialSpawner) (Object) this;
		ServerPlayer player = InstancedTrialSpawnerLoot.resolveNextEjectionPlayer(level, spawner);
		if (player == null) {
			ci.cancel();
			return;
		}
		InstancedTrialSpawnerLoot.ejectReward(level, pos, lootTable, player);
		ci.cancel();
	}
}
