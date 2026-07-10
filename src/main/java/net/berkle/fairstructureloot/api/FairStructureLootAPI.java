package net.berkle.fairstructureloot.api;

import net.berkle.fairstructureloot.loot.StructureGroup;
import net.berkle.fairstructureloot.loot.StructureGroupRegistry;

/** Public API for registering additional structure loot groups from other mods. */
public final class FairStructureLootAPI {

	private FairStructureLootAPI() {
	}

	public static void registerGroup(StructureGroup group) {
		StructureGroupRegistry.register(group);
	}
}
