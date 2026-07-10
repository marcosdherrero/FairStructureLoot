package net.berkle.fairstructureloot.loot;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import net.minecraft.world.level.Level;

/** Stable keys for instanced containers in saved data and client sync. */
public final class ContainerKeys {

	private ContainerKeys() {
	}

	public static String containerKey(Level level, BlockPos pos) {
		return containerKey(level.dimension().identifier(), pos);
	}

	public static String containerKey(Identifier dimension, BlockPos pos) {
		return dimension + ":" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
	}
}
