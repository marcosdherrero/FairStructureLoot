package net.berkle.fairstructureloot.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import net.berkle.fairstructureloot.loot.LootRollModeSavedData;
import net.berkle.fairstructureloot.loot.LootRollModeSavedData.Mode;
import net.berkle.fairstructureloot.network.FairStructureLootNetworking;

/** /fairstructureloot set loot {random|shared} */
public final class LootRollModeCommand {

	private LootRollModeCommand() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> lootRoot() {
		return Commands.literal("loot")
			.requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
			.then(Commands.literal("random").executes(ctx -> setMode(ctx, Mode.RANDOM)))
			.then(Commands.literal("shared").executes(ctx -> setMode(ctx, Mode.SHARED)));
	}

	private static int setMode(CommandContext<CommandSourceStack> ctx, Mode mode) {
		ServerLevel level = ctx.getSource().getLevel();
		LootRollModeSavedData.get(level).setMode(mode);
		for (ServerPlayer player : level.players()) {
			FairStructureLootNetworking.syncLootRollMode(player);
		}
		ctx.getSource().sendSuccess(
			() -> Component.literal("Fair structure loot roll mode: " + mode.commandName()),
			true
		);
		return 1;
	}
}
