package net.berkle.fairstructureloot.mixin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;

import net.berkle.fairstructureloot.loot.InstancedLootMenuRegistry;

/** Appends instanced-menu players so chest/barrel lids animate while per-player menus are open. */
@Mixin(ContainerOpenersCounter.class)
public class ContainerOpenersCounterMixin {

	@Inject(method = "getEntitiesWithContainerOpen", at = @At("RETURN"), cancellable = true)
	private void fairstructureloot$appendInstancedMenuOpeners(
		Level level,
		BlockPos pos,
		CallbackInfoReturnable<List<ContainerUser>> cir
	) {
		List<ContainerUser> instanced = InstancedLootMenuRegistry.getOpeners(level, pos);
		if (instanced.isEmpty()) {
			return;
		}
		List<ContainerUser> combined = new ArrayList<>(cir.getReturnValue());
		Set<ContainerUser> seen = new HashSet<>(combined);
		for (ContainerUser user : instanced) {
			if (seen.add(user)) {
				combined.add(user);
			}
		}
		cir.setReturnValue(combined);
	}
}
