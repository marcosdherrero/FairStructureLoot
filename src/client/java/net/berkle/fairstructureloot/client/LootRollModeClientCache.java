package net.berkle.fairstructureloot.client;

/** Client mirror of the world's loot roll mode ({@code random} vs {@code shared}). */
public final class LootRollModeClientCache {

	private static boolean sharedRoll;

	private LootRollModeClientCache() {
	}

	public static void setSharedRoll(boolean shared) {
		sharedRoll = shared;
	}

	public static boolean isShared() {
		return sharedRoll;
	}

	public static void clear() {
		sharedRoll = false;
	}
}
