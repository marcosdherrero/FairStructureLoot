package net.berkle.fairstructureloot.mixin;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.level.storage.loot.LootTable;

import net.berkle.fairstructureloot.loot.InstancedTrialSpawnerLoot;
import net.berkle.fairstructureloot.loot.StructureLootTables;
import net.berkle.fairstructureloot.loot.TrialSpawnerPlayers;

@Mixin(TrialSpawnerState.class)
public abstract class TrialSpawnerStateMixin {

	private static final ThreadLocal<Entity> OMINOUS_TARGET = new ThreadLocal<>();

	@Shadow
	private static Entity selectEntityToSpawnItemAbove(
		List<Player> players,
		Set<UUID> currentMobs,
		TrialSpawner spawner,
		BlockPos pos,
		ServerLevel level
	) {
		throw new AssertionError();
	}

	@Redirect(
		method = "calculatePositionToSpawnSpawner",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/entity/trialspawner/TrialSpawnerState;selectEntityToSpawnItemAbove(Ljava/util/List;Ljava/util/Set;Lnet/minecraft/world/level/block/entity/trialspawner/TrialSpawner;Lnet/minecraft/core/BlockPos;Lnet/minecraft/server/level/ServerLevel;)Lnet/minecraft/world/entity/Entity;"
		)
	)
	private static Entity fairstructureloot$captureOminousTarget(
		List<Player> players,
		Set<UUID> currentMobs,
		TrialSpawner spawner,
		BlockPos pos,
		ServerLevel level
	) {
		Entity entity = selectEntityToSpawnItemAbove(players, currentMobs, spawner, pos, level);
		OMINOUS_TARGET.set(entity);
		return entity;
	}

	@Inject(method = "spawnOminousOminousItemSpawner", at = @At("RETURN"))
	private void fairstructureloot$clearOminousTarget(
		ServerLevel level,
		BlockPos pos,
		TrialSpawner spawner,
		CallbackInfo ci
	) {
		OMINOUS_TARGET.remove();
	}

	@ModifyVariable(
		method = "spawnOminousOminousItemSpawner",
		at = @At(
			value = "INVOKE",
			target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V"
		),
		ordinal = 0
	)
	private ItemStack fairstructureloot$ominousDispensedItem(
		ItemStack stack,
		ServerLevel level,
		BlockPos pos,
		TrialSpawner spawner
	) {
		ResourceKey<LootTable> lootTable = spawner.ominousConfig().itemsToDropWhenOminous();
		if (!StructureLootTables.isInstanced(level, lootTable)) {
			return stack;
		}
		ServerPlayer player = TrialSpawnerPlayers.resolveOminousTarget(level, pos, spawner, OMINOUS_TARGET.get());
		if (player == null) {
			return stack;
		}
		ItemStack rolled = InstancedTrialSpawnerLoot.rollOminousDispensedItem(level, pos, player, lootTable);
		return rolled.isEmpty() ? stack : rolled;
	}
}
