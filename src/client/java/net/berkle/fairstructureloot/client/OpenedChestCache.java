package net.berkle.fairstructureloot.client;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import net.berkle.fairstructureloot.loot.ContainerKeys;

/** Client-side record of which instanced containers this player has already opened. */
public final class OpenedChestCache {

	private static final Set<String> PERSONALLY_OPENED_KEYS = new HashSet<>();
	private static final Set<String> GLOBALLY_OPENED_KEYS = new HashSet<>();

	private OpenedChestCache() {
	}

	public static void replacePersonallyOpened(List<String> containerKeys) {
		PERSONALLY_OPENED_KEYS.clear();
		PERSONALLY_OPENED_KEYS.addAll(containerKeys);
	}

	public static void replaceGloballyOpened(List<String> containerKeys) {
		GLOBALLY_OPENED_KEYS.clear();
		GLOBALLY_OPENED_KEYS.addAll(containerKeys);
	}

	public static void markPersonallyOpened(Identifier dimension, BlockPos pos) {
		PERSONALLY_OPENED_KEYS.add(ContainerKeys.containerKey(dimension, pos));
	}

	public static void markGloballyOpened(Identifier dimension, BlockPos pos) {
		GLOBALLY_OPENED_KEYS.add(ContainerKeys.containerKey(dimension, pos));
	}

	public static boolean isPersonallyOpened(Level level, BlockPos pos) {
		return PERSONALLY_OPENED_KEYS.contains(ContainerKeys.containerKey(level, pos));
	}

	public static boolean isGloballyOpened(Level level, BlockPos pos) {
		return GLOBALLY_OPENED_KEYS.contains(ContainerKeys.containerKey(level, pos));
	}

	public static void clear() {
		PERSONALLY_OPENED_KEYS.clear();
		GLOBALLY_OPENED_KEYS.clear();
	}
}
