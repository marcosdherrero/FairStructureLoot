package net.berkle.fairstructureloot.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;

@Mixin(BaseContainerBlockEntity.class)
public interface BaseContainerBlockEntityAccessor {

	@Invoker("getItems")
	NonNullList<ItemStack> fairstructureloot$getItems();
}
