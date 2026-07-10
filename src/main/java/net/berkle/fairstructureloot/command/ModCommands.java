package net.berkle.fairstructureloot.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import net.berkle.fairstructureloot.FairStructureLootMain;

/** Registers the {@code /fairstructureloot} command tree. */
public final class ModCommands {

	private ModCommands() {
	}

	public static void register(
		CommandDispatcher<CommandSourceStack> dispatcher,
		CommandBuildContext registryAccess,
		Commands.CommandSelection environment
	) {
		dispatcher.register(
			Commands.literal(FairStructureLootMain.COMMAND_ROOT)
				.then(StructureActivationCommand.setRoot())
				.then(StructureActivationCommand.listRoot())
				.then(LootRollModeCommand.lootRoot())
		);
	}
}
