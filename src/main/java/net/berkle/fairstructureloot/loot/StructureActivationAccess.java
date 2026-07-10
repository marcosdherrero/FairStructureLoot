package net.berkle.fairstructureloot.loot;



/** Client/server bridge for structure activation checks on non-server levels. */

public final class StructureActivationAccess {



	private static volatile Provider provider = StructureActivationAccess::defaultEnabled;



	private StructureActivationAccess() {

	}



	public static void setProvider(Provider provider) {

		StructureActivationAccess.provider = provider;

	}



	public static Provider get() {

		return provider;

	}



	private static boolean defaultEnabled(String commandName) {

		return StructureGroupRegistry.groupByCommandName(commandName)

			.map(StructureGroup::defaultEnabled)

			.orElse(false);

	}



	@FunctionalInterface

	public interface Provider {

		boolean isEnabled(String commandName);

	}

}

