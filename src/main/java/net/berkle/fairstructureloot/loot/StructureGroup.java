package net.berkle.fairstructureloot.loot;

import java.util.Set;

import net.minecraft.resources.Identifier;

/** A togglable structure loot group mapped to one or more vanilla loot tables. */
public record StructureGroup(
	Identifier id,
	String commandName,
	boolean defaultEnabled,
	Set<Identifier> lootTableIds,
	boolean perPlayerIndicator
) {
	public StructureGroup(Identifier id, String commandName, boolean defaultEnabled, Set<Identifier> lootTableIds) {
		this(id, commandName, defaultEnabled, lootTableIds, false);
	}
}
