package net.berkle.fairstructureloot.loot;



import org.jetbrains.annotations.Nullable;



import net.minecraft.core.BlockPos;

import net.minecraft.core.NonNullList;

import net.minecraft.server.level.ServerLevel;

import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.SimpleContainer;

import net.minecraft.world.entity.ContainerUser;

import net.minecraft.world.entity.player.Player;

import net.minecraft.world.item.ItemStack;

import net.minecraft.world.level.block.entity.BarrelBlockEntity;

import net.minecraft.world.level.block.entity.ChestBlockEntity;

import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;



/**

 * Per-player menu inventory backed by {@link InstancedLootSavedData}.

 * Syncs lid animation for chests/barrels and persists slot changes on {@link #setChanged()}.

 */

public final class InstancedLootContainer extends SimpleContainer {



	private final ServerLevel level;

	private final BlockPos pos;

	private final RandomizableContainerBlockEntity primaryContainer;

	@Nullable

	private final ChestBlockEntity partnerChest;

	private final ServerPlayer owner;

	private final NonNullList<ItemStack> backingItems;



	public InstancedLootContainer(

		ServerLevel level,

		ServerPlayer owner,

		BlockPos pos,

		RandomizableContainerBlockEntity primaryContainer,

		@Nullable ChestBlockEntity partnerChest,

		NonNullList<ItemStack> backingItems,

		int size

	) {

		super(size);

		this.level = level;

		this.owner = owner;

		this.pos = pos;

		this.primaryContainer = primaryContainer;

		this.partnerChest = partnerChest;

		this.backingItems = backingItems;

		for (int i = 0; i < size; i++) {

			ItemStack stack = backingItems.get(i);

			this.items.set(i, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());

		}

	}



	ServerLevel level() {

		return level;

	}



	BlockPos pos() {

		return pos;

	}



	@Override

	public void startOpen(ContainerUser user) {

		if (primaryContainer instanceof ChestBlockEntity chest) {

			chest.startOpen(user);

		} else if (primaryContainer instanceof BarrelBlockEntity barrel) {

			barrel.startOpen(user);

		}

		if (partnerChest != null) {

			partnerChest.startOpen(user);

		}

		InstancedLootMenuRegistry.register(level, pos, owner);

	}



	@Override

	public void stopOpen(ContainerUser user) {

		if (primaryContainer instanceof ChestBlockEntity chest) {

			chest.stopOpen(user);

		} else if (primaryContainer instanceof BarrelBlockEntity barrel) {

			barrel.stopOpen(user);

		}

		if (partnerChest != null) {

			partnerChest.stopOpen(user);

		}

		InstancedLootMenuRegistry.unregister(level, pos, owner);

	}



	/** Copies live slots into the backing list and marks overworld SavedData dirty. */

	@Override

	public void setChanged() {

		super.setChanged();

		for (int i = 0; i < getContainerSize(); i++) {

			ItemStack stack = getItem(i);

			backingItems.set(i, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());

		}

		InstancedLootSavedData.get(level).updateInventory(level, owner, pos, backingItems);

	}



	@Override

	public boolean stillValid(Player player) {

		if (player != owner || !primaryContainer.stillValid(player)) {

			return false;

		}

		return partnerChest == null || partnerChest.stillValid(player);

	}

}


