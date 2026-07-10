package net.berkle.fairstructureloot.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Green, orange, or black triangle (gray shadow) under the crosshair for fair structure loot containers. */
public final class FairLootHudIndicator implements HudElement {

	private static final int CROSSHAIR_OFFSET_Y = 10;

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
		Minecraft minecraft = Minecraft.getInstance();
		FairLootIndicatorHelper.fromCrosshairTarget(minecraft).ifPresent(state -> {
			int centerX = minecraft.getWindow().getGuiScaledWidth() / 2;
			int centerY = minecraft.getWindow().getGuiScaledHeight() / 2;
			int y = centerY + CROSSHAIR_OFFSET_Y;
			int color = switch (state) {
				case UNOPENED -> FairLootTriangleIcon.GREEN;
				case OPENED_BY_OTHER -> FairLootTriangleIcon.ORANGE;
				case OPENED_BY_YOU -> FairLootTriangleIcon.BLACK;
			};
			FairLootTriangleIcon.drawWithShadow(graphics, centerX, y, color);
		});
	}
}
