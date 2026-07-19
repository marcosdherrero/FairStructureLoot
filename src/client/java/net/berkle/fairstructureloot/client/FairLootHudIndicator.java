package net.berkle.fairstructureloot.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Color-only triangle under the crosshair for fair-loot containers (none for normal chests):
 * <ul>
 *   <li>Green — unopened (no shared roll yet, or per-player rolls and you have not opened)</li>
 *   <li>Yellow — shared rolls on and someone else already rolled</li>
 *   <li>Black — you have opened this container</li>
 * </ul>
 */
public final class FairLootHudIndicator implements HudElement {

	private static final int CROSSHAIR_OFFSET_Y = 10;

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.options.hideGui) {
			return;
		}
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
