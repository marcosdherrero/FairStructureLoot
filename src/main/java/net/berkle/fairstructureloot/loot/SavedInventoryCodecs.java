package net.berkle.fairstructureloot.loot;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

/** Codecs for persisting container slots (only non-empty stacks are written). */
final class SavedInventoryCodecs {

	private SavedInventoryCodecs() {
	}

	private record StoredSlot(int slot, ItemStack stack) {
		private static final Codec<StoredSlot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("Slot").forGetter(StoredSlot::slot),
			ItemStack.CODEC.fieldOf("Item").forGetter(StoredSlot::stack)
		).apply(instance, StoredSlot::new));
	}

	static Codec<List<ItemStack>> itemList() {
		return StoredSlot.CODEC.listOf().xmap(
			SavedInventoryCodecs::decodeSlots,
			SavedInventoryCodecs::encodeSlots
		);
	}

	private static List<ItemStack> decodeSlots(List<StoredSlot> stored) {
		if (stored.isEmpty()) {
			return List.of();
		}
		int size = stored.stream().mapToInt(StoredSlot::slot).max().orElse(-1) + 1;
		List<ItemStack> stacks = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			stacks.add(ItemStack.EMPTY);
		}
		for (StoredSlot entry : stored) {
			if (entry.slot() >= 0 && entry.slot() < size) {
				stacks.set(entry.slot(), entry.stack().copy());
			}
		}
		return stacks;
	}

	private static List<StoredSlot> encodeSlots(List<ItemStack> stacks) {
		List<StoredSlot> stored = new ArrayList<>();
		for (int i = 0; i < stacks.size(); i++) {
			ItemStack stack = stacks.get(i);
			if (!stack.isEmpty()) {
				stored.add(new StoredSlot(i, stack.copy()));
			}
		}
		return stored;
	}
}
