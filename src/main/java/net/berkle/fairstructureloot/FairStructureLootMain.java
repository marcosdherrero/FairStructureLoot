package net.berkle.fairstructureloot;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.berkle.fairstructureloot.command.ModCommands;
import net.berkle.fairstructureloot.events.ModServerEvents;
import net.berkle.fairstructureloot.network.FairStructureLootNetworking;

/** Per-player instanced loot for rare structure rewards (Lootr-like, scoped). */
public class FairStructureLootMain implements ModInitializer {

	public static final String MOD_ID = "fairstructureloot";
	public static final String COMMAND_ROOT = "fairstructureloot";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("[{}] Initializing per-player structure loot…", MOD_ID);
		FairStructureLootNetworking.registerPayloadTypes();
		CommandRegistrationCallback.EVENT.register(ModCommands::register);
		ModServerEvents.register();
		LOGGER.info("[{}] Ready — use /{} set <structure|all> <activate|deactivate>", MOD_ID, COMMAND_ROOT);
	}
}
