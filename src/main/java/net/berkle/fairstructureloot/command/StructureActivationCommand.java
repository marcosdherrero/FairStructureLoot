package net.berkle.fairstructureloot.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;

import net.berkle.fairstructureloot.loot.StructureActivationSavedData;
import net.berkle.fairstructureloot.loot.StructureGroup;
import net.berkle.fairstructureloot.loot.StructureGroupRegistry;
import net.berkle.fairstructureloot.network.FairStructureLootNetworking;

/** /fairstructureloot set {structure|all} {activate|deactivate} and list structures. */
public final class StructureActivationCommand {

	private StructureActivationCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> setRoot() {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("set")
			.requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
			.then(
				Commands.literal("all")
					.then(Commands.literal("activate").executes(ctx -> setAll(ctx, true)))
					.then(Commands.literal("deactivate").executes(ctx -> setAll(ctx, false)))
			);

		for (String commandName : StructureGroupRegistry.allCommandNames()) {
			root.then(
				Commands.literal(commandName)
					.then(Commands.literal("activate").executes(ctx -> setOne(ctx, commandName, true)))
					.then(Commands.literal("deactivate").executes(ctx -> setOne(ctx, commandName, false)))
			);
		}

		return root;
	}

	public static LiteralArgumentBuilder<CommandSourceStack> listRoot() {
		return Commands.literal("list")
			.requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
			.then(Commands.literal("structures").executes(StructureActivationCommand::listStructures));
	}

	private static int setOne(CommandContext<CommandSourceStack> ctx, String commandName, boolean active) {
		ServerLevel level = ctx.getSource().getLevel();
		StructureActivationSavedData data = StructureActivationSavedData.get(level);
		data.setEnabled(commandName, active);
		FairStructureLootNetworking.broadcastStructureActivation(level);
		ctx.getSource().sendSuccess(
			() -> Component.literal("Fair structure loot for " + commandName + ": " + (active ? "ACTIVE" : "INACTIVE")),
			true
		);
		return 1;
	}

	private static int setAll(CommandContext<CommandSourceStack> ctx, boolean active) {
		ServerLevel level = ctx.getSource().getLevel();
		StructureActivationSavedData data = StructureActivationSavedData.get(level);
		data.setAll(active);
		FairStructureLootNetworking.broadcastStructureActivation(level);
		ctx.getSource().sendSuccess(
			() -> Component.literal("Fair structure loot for all structures: " + (active ? "ACTIVE" : "INACTIVE")),
			true
		);
		return 1;
	}

	private static int listStructures(CommandContext<CommandSourceStack> ctx) {
		ServerLevel level = ctx.getSource().getLevel();
		StructureActivationSavedData data = StructureActivationSavedData.get(level);
		ctx.getSource().sendSuccess(() -> Component.literal("Fair structure loot groups:"), false);
		for (StructureGroup group : StructureGroupRegistry.allGroups()) {
			boolean enabled = data.isEnabled(group.commandName());
			ctx.getSource().sendSuccess(
				() -> Component.literal("  " + group.commandName() + ": " + (enabled ? "ACTIVE" : "INACTIVE")),
				false
			);
		}
		return StructureGroupRegistry.allGroups().size();
	}
}
