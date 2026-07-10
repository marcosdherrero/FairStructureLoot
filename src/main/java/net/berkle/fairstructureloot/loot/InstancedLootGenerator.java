package net.berkle.fairstructureloot.loot;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import net.berkle.fairstructureloot.FairStructureLootMain;

/** Rolls vanilla loot tables with per-player or shared seeds for instanced containers. */
public final class InstancedLootGenerator {

	private static final int EMPTY_ROLL_RETRIES = 5;

	private InstancedLootGenerator() {
	}

	public static boolean hasAnyItems(List<ItemStack> stacks) {
		for (ItemStack stack : stacks) {
			if (!stack.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/** Detects inventories truncated by the pre-1.0.1 menu init bug (at most one stack saved). */
	public static boolean isCorruptedSparseRoll(ResourceKey<LootTable> lootTable, List<ItemStack> stacks) {
		if (StructureLootTables.usesPerPlayerIndicator(lootTable)) {
			return false;
		}
		int occupied = 0;
		for (ItemStack stack : stacks) {
			if (!stack.isEmpty()) {
				occupied++;
				if (occupied > 1) {
					return false;
				}
			}
		}
		return occupied == 1;
	}

	public static NonNullList<ItemStack> generate(
		ServerLevel level,
		ServerPlayer player,
		BlockPos pos,
		ResourceKey<LootTable> lootTableKey,
		long baseSeed,
		int size,
		boolean perPlayerSeed
	) {
		LootTable table = level.getServer().reloadableRegistries().getLootTable(lootTableKey);
		long playerSeed = perPlayerSeed
			? mixSeed(baseSeed, player.getUUID().getLeastSignificantBits(), player.getUUID().getMostSignificantBits())
			: normalizeSeed(baseSeed);

		LootParams.Builder paramsBuilder = new LootParams.Builder(level)
			.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
			.withLuck(player.getLuck())
			.withParameter(LootContextParams.THIS_ENTITY, player);
		LootParams params = paramsBuilder.create(LootContextParamSets.CHEST);

		NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
		int filledSlots = 0;
		for (int attempt = 0; attempt < EMPTY_ROLL_RETRIES; attempt++) {
			long seed = normalizeSeed(playerSeed ^ attempt);
			SimpleContainer lootContainer = new SimpleContainer(size);
			table.fill(lootContainer, params, seed);

			filledSlots = 0;
			for (int i = 0; i < size; i++) {
				ItemStack stack = lootContainer.getItem(i);
				items.set(i, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
				if (!stack.isEmpty()) {
					filledSlots++;
				}
			}
			if (filledSlots > 0) {
				break;
			}
		}

		if (filledSlots == 0) {
			FairStructureLootMain.LOGGER.warn(
				"[{}] Loot table {} rolled empty for {} at {} after {} attempts (perPlayerSeed={})",
				FairStructureLootMain.MOD_ID,
				lootTableKey.identifier(),
				player.getGameProfile().name(),
				pos,
				EMPTY_ROLL_RETRIES,
				perPlayerSeed
			);
		} else if (FairStructureLootMain.LOGGER.isDebugEnabled()) {
			FairStructureLootMain.LOGGER.debug(
				"[{}] Rolled {} for {} at {}: {} occupied slots, {} item types (perPlayerSeed={})",
				FairStructureLootMain.MOD_ID,
				lootTableKey.identifier(),
				player.getGameProfile().name(),
				pos,
				filledSlots,
				countDistinctItemTypes(items),
				perPlayerSeed
			);
		}
		return finalizeRoll(lootTableKey, items);
	}

	/** Applies table-specific inventory layout (e.g. End ship elytra centered in the barrel). */
	public static NonNullList<ItemStack> finalizeRoll(ResourceKey<LootTable> lootTableKey, NonNullList<ItemStack> items) {
		if (lootTableKey == StructureLootTables.END_SHIP_ELYTRA) {
			centerElytra(items);
		}
		return items;
	}

	private static void centerElytra(NonNullList<ItemStack> items) {
		if (items.isEmpty()) {
			return;
		}
		ItemStack elytra = ItemStack.EMPTY;
		for (int slot = 0; slot < items.size(); slot++) {
			ItemStack stack = items.get(slot);
			if (!stack.is(Items.ELYTRA)) {
				continue;
			}
			if (elytra.isEmpty()) {
				elytra = stack.copy();
			}
			items.set(slot, ItemStack.EMPTY);
		}
		if (elytra.isEmpty()) {
			elytra = new ItemStack(Items.ELYTRA);
		}
		items.set(centerSlot(items.size()), elytra);
	}

	private static int centerSlot(int containerSize) {
		return (containerSize - 1) / 2;
	}

	/** Combine the structure seed with a player id so each player gets unique but deterministic loot. */
	private static long mixSeed(long baseSeed, long low, long high) {
		return normalizeSeed(baseSeed ^ low ^ (high << 1));
	}

	private static long normalizeSeed(long seed) {
		return seed == 0L ? 1L : seed;
	}

	private static int countDistinctItemTypes(NonNullList<ItemStack> items) {
		int types = 0;
		for (int i = 0; i < items.size(); i++) {
			ItemStack stack = items.get(i);
			if (stack.isEmpty()) {
				continue;
			}
			Item item = stack.getItem();
			boolean seen = false;
			for (int j = 0; j < i; j++) {
				if (items.get(j).is(stack.getItem())) {
					seen = true;
					break;
				}
			}
			if (!seen) {
				types++;
			}
		}
		return types;
	}
}
