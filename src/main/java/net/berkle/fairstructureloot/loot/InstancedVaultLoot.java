package net.berkle.fairstructureloot.loot;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;

import net.berkle.fairstructureloot.network.FairStructureLootNetworking;

/** Per-player vault unlock rolls (trial chamber vaults use {@link VaultBlockEntity.Server}, not chest menus). */
public final class InstancedVaultLoot {

	public static final int VAULT_ROLL_SLOTS = 27;

	private InstancedVaultLoot() {
	}

	public static List<ItemStack> resolveItemsToEject(
		ServerLevel level,
		ServerPlayer player,
		BlockPos pos,
		ResourceKey<LootTable> lootTable
	) {
		long lootSeed = pos.asLong();
		var open = InstancedLootSavedData.get(level).openContainer(
			level,
			player,
			pos,
			lootTable,
			lootSeed,
			VAULT_ROLL_SLOTS
		);
		FairStructureLootNetworking.notifyChestOpened(player, level, pos, lootTable, open.causedGlobalRoll());

		List<ItemStack> items = new ArrayList<>();
		for (ItemStack stack : open.items()) {
			if (!stack.isEmpty()) {
				items.add(stack.copy());
			}
		}
		return items;
	}
}
