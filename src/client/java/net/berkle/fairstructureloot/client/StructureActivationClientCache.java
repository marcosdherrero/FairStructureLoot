package net.berkle.fairstructureloot.client;



import java.util.HashMap;

import java.util.List;

import java.util.Map;



import net.berkle.fairstructureloot.loot.StructureActivationAccess;

import net.berkle.fairstructureloot.loot.StructureGroup;

import net.berkle.fairstructureloot.loot.StructureGroupRegistry;

import net.berkle.fairstructureloot.network.SyncStructureActivationPayload;



/** Client mirror of server structure activation flags. */

public final class StructureActivationClientCache {



	private static final Map<String, Boolean> STATES = new HashMap<>();



	private StructureActivationClientCache() {

	}



	public static void replaceAll(List<SyncStructureActivationPayload.Entry> structures) {

		STATES.clear();

		for (StructureGroup group : StructureGroupRegistry.allGroups()) {

			STATES.put(group.commandName(), group.defaultEnabled());

		}

		for (SyncStructureActivationPayload.Entry entry : structures) {

			StructureGroupRegistry.groupByCommandName(entry.name())

				.ifPresent(group -> STATES.put(group.commandName(), entry.enabled()));

		}

	}



	public static boolean isEnabled(String commandName) {

		return STATES.getOrDefault(

			commandName,

			StructureGroupRegistry.groupByCommandName(commandName).map(StructureGroup::defaultEnabled).orElse(false)

		);

	}



	public static void clear() {
		STATES.clear();
		StructureActivationAccess.setProvider(commandName ->
			StructureGroupRegistry.groupByCommandName(commandName).map(StructureGroup::defaultEnabled).orElse(false)
		);
	}



	static {

		StructureActivationAccess.setProvider(StructureActivationClientCache::isEnabled);

	}

}

