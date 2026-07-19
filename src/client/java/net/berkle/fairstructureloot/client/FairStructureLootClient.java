package net.berkle.fairstructureloot.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

import net.minecraft.resources.Identifier;

import net.berkle.fairstructureloot.FairStructureLootMain;
import net.berkle.fairstructureloot.network.BreakHintPayload;
import net.berkle.fairstructureloot.network.MarkChestGloballyOpenedPayload;
import net.berkle.fairstructureloot.network.MarkChestOpenedPayload;
import net.berkle.fairstructureloot.network.MarkFairLootChestPayload;
import net.berkle.fairstructureloot.network.SyncFairLootChestsPayload;
import net.berkle.fairstructureloot.network.SyncGloballyOpenedChestsPayload;
import net.berkle.fairstructureloot.network.SyncLootRollModePayload;
import net.berkle.fairstructureloot.network.SyncOpenedChestsPayload;
import net.berkle.fairstructureloot.network.SyncStructureActivationPayload;

/** Client entry: HUD indicator, S2C packet handlers, disconnect cache cleanup. */
public class FairStructureLootClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		FairLootClientInteraction.register();

		// Register last after other mods' client init so HUD markers stay on top.
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			HudElementRegistry.addLast(
				Identifier.fromNamespaceAndPath(FairStructureLootMain.MOD_ID, "fair_loot_indicator"),
				new FairLootHudIndicator()
			);
			HudElementRegistry.addLast(
				Identifier.fromNamespaceAndPath(FairStructureLootMain.MOD_ID, "fair_loot_break_hint"),
				new FairLootBreakHintOverlay()
			);
		});

		ClientPlayNetworking.registerGlobalReceiver(BreakHintPayload.TYPE, (payload, context) ->
			context.client().execute(FairLootBreakHint::show)
		);

		ClientPlayNetworking.registerGlobalReceiver(MarkFairLootChestPayload.TYPE, (payload, context) ->
			context.client().execute(() ->
				FairLootChestClientCache.setFairLoot(
					payload.dimension(),
					payload.pos(),
					payload.fairLoot(),
					payload.perPlayerIndicator()
				)
			)
		);

		ClientPlayNetworking.registerGlobalReceiver(MarkChestOpenedPayload.TYPE, (payload, context) ->
			context.client().execute(() -> OpenedChestCache.markPersonallyOpened(payload.dimension(), payload.pos()))
		);

		ClientPlayNetworking.registerGlobalReceiver(MarkChestGloballyOpenedPayload.TYPE, (payload, context) ->
			context.client().execute(() -> OpenedChestCache.markGloballyOpened(payload.dimension(), payload.pos()))
		);

		ClientPlayNetworking.registerGlobalReceiver(SyncOpenedChestsPayload.TYPE, (payload, context) ->
			context.client().execute(() -> OpenedChestCache.replacePersonallyOpened(payload.containerKeys()))
		);

		ClientPlayNetworking.registerGlobalReceiver(SyncGloballyOpenedChestsPayload.TYPE, (payload, context) ->
			context.client().execute(() -> OpenedChestCache.replaceGloballyOpened(payload.containerKeys()))
		);

		ClientPlayNetworking.registerGlobalReceiver(SyncLootRollModePayload.TYPE, (payload, context) ->
			context.client().execute(() -> LootRollModeClientCache.setSharedRoll(payload.sharedRoll()))
		);

		ClientPlayNetworking.registerGlobalReceiver(SyncStructureActivationPayload.TYPE, (payload, context) ->
			context.client().execute(() -> StructureActivationClientCache.replaceAll(payload.structures()))
		);

		ClientPlayNetworking.registerGlobalReceiver(SyncFairLootChestsPayload.TYPE, (payload, context) ->
			context.client().execute(() -> FairLootChestClientCache.replaceAll(payload.entries()))
		);

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			OpenedChestCache.clear();
			FairLootChestClientCache.clear();
			LootRollModeClientCache.clear();
			StructureActivationClientCache.clear();
			FairLootIndicatorHelper.clearCache();
		});
	}
}
