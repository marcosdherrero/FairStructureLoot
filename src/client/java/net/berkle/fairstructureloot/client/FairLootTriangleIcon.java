package net.berkle.fairstructureloot.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

/** Draws fair-loot HUD triangle markers with a gray drop shadow. */
public final class FairLootTriangleIcon {

	public static final int SHADOW = 0xFF404040;
	public static final int GREEN = 0xFF28C841;
	public static final int ORANGE = 0xFFEBA023;
	public static final int BLACK = 0xFF000000;

	private FairLootTriangleIcon() {
	}

	public static void drawWithShadow(GuiGraphicsExtractor graphics, int centerX, int y, int color) {
		drawWithShadow(graphics, centerX, y, color, 1);
	}

	public static void drawWithShadow(GuiGraphicsExtractor graphics, int centerX, int y, int color, int scale) {
		drawTriangle(graphics, centerX + scale, y + scale, SHADOW, scale);
		drawTriangle(graphics, centerX, y, color, scale);
	}

	/** Green, orange, and black triangles — same layout as the mod icon (no background). */
	public static void drawToastIcon(GuiGraphicsExtractor graphics, int centerX, int centerY, int scale) {
		drawWithShadow(graphics, centerX - 3 * scale, centerY - 3 * scale, GREEN, scale);
		drawWithShadow(graphics, centerX + 2 * scale, centerY, ORANGE, scale);
		drawWithShadow(graphics, centerX - scale, centerY + 3 * scale, BLACK, scale);
	}

	/**
	 * Draws the mod-icon triangle cluster centered in a box ({@code #abcdef} icon layout, no fill).
	 * Bbox matches {@code assets/icon.png}: green upper-left, orange mid-right, black lower-left.
	 */
	public static void drawToastIconInBox(
		GuiGraphicsExtractor graphics,
		int boxX,
		int boxY,
		int boxWidth,
		int boxHeight,
		int scale
	) {
		int iconWidth = toastIconWidth(scale);
		int iconHeight = toastIconHeight(scale);
		int bboxLeft = boxX + (boxWidth - iconWidth) / 2;
		int bboxTop = boxY + (boxHeight - iconHeight) / 2;
		int centerX = bboxLeft + 5 * scale;
		int centerY = bboxTop + 3 * scale;
		drawToastIcon(graphics, centerX, centerY, scale);
	}

	/** Vertical span of {@link #drawToastIcon} for centering in UI slots. */
	public static int toastIconHeight(int scale) {
		return 10 * scale;
	}

	/** Horizontal span of {@link #drawToastIcon} for centering in UI slots. */
	public static int toastIconWidth(int scale) {
		return 11 * scale;
	}

	private static void drawTriangle(GuiGraphicsExtractor graphics, int centerX, int y, int color, int scale) {
		int left = centerX - 2 * scale;
		fillBlock(graphics, left + 2 * scale, y, scale, scale, color);
		fillBlock(graphics, left + scale, y + scale, scale * 3, scale, color);
		fillBlock(graphics, left, y + 2 * scale, scale * 5, scale, color);
	}

	private static void fillBlock(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
		if (width <= 0 || height <= 0) {
			return;
		}
		graphics.fill(RenderPipelines.GUI, x, y, x + width, y + height, color);
	}
}
