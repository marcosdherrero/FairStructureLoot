package net.berkle.fairstructureloot.client;

import java.util.List;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import net.berkle.fairstructureloot.FairStructureLootMain;

/**
 * Break-hint drawn like a vanilla advancement toast (slide in / hold / slide out)
 * on our own HUD layer so other mods cannot corrupt ToastManager.
 * <p>
 * Re-triggering while visible only resets the hold timer — it does not replay the slide-in.
 */
public final class FairLootBreakHintOverlay implements HudElement {

	private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("toast/advancement");
	private static final Identifier TOAST_ICON = Identifier.fromNamespaceAndPath(
		FairStructureLootMain.MOD_ID,
		"textures/gui/toast_icon.png"
	);

	private static final Component BODY = Component.literal(
		"This is a loot container. Every player can get loot from here. "
			+ "Hold sneak and break the container to break it for all players."
	).withStyle(ChatFormatting.GRAY);

	/** Matches vanilla toast slide duration (~600 ms). */
	private static final long SLIDE_MS = 600L;
	/** Visible hold time after slide-in finishes (before slide-out). */
	private static final long HOLD_MS = 5000L;

	private static final int TOAST_WIDTH = 160;
	private static final int HORIZONTAL_PADDING = 6;
	private static final float TEXT_SCALE = 0.75f;
	private static final int LINE_GAP = 2;
	private static final int VERTICAL_PADDING = 8;
	private static final int ICON_SIZE = 22;
	private static final int BODY_COLOR = 0xFFD8D8D8;

	private enum Phase {
		HIDDEN,
		SLIDING_IN,
		SHOWING,
		SLIDING_OUT
	}

	private static Phase phase = Phase.HIDDEN;
	private static long phaseStartMs;
	/** When SHOWING began (or was last extended); slide-out starts HOLD_MS after this. */
	private static long holdStartMs;

	private static List<FormattedCharSequence> cachedLines;
	private static int cachedToastHeight = 32;

	public static void show() {
		long now = System.currentTimeMillis();
		if (phase == Phase.SLIDING_IN || phase == Phase.SHOWING) {
			// Already up — only refresh the hold timer; do not replay slide-in.
			holdStartMs = now;
			return;
		}
		if (phase == Phase.SLIDING_OUT) {
			// Reverse out mid-exit into a fresh slide-in from the current offset.
			float outProgress = slideProgress(now - phaseStartMs);
			float inProgress = 1.0f - outProgress;
			phase = Phase.SLIDING_IN;
			phaseStartMs = now - (long) (inProgress * SLIDE_MS);
			holdStartMs = now;
			return;
		}

		phase = Phase.SLIDING_IN;
		phaseStartMs = now;
		holdStartMs = now;
		Minecraft client = Minecraft.getInstance();
		if (client.player != null) {
			client.player.playSound(SoundEvents.UI_TOAST_IN, 1.0f, 1.0f);
		}
	}

	public static boolean isVisible() {
		return phase != Phase.HIDDEN;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
		if (phase == Phase.HIDDEN) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.options.hideGui || minecraft.font == null) {
			return;
		}

		long now = System.currentTimeMillis();
		tickPhase(now);

		if (phase == Phase.HIDDEN) {
			return;
		}

		ensureLayout(minecraft.font);
		float slide = slideOffsetFraction(now);
		int screenW = minecraft.getWindow().getGuiScaledWidth();
		int boxX = screenW - TOAST_WIDTH + Math.round(TOAST_WIDTH * slide);
		int boxY = 0;

		graphics.blitSprite(
			RenderPipelines.GUI_TEXTURED,
			BACKGROUND_SPRITE,
			boxX,
			boxY,
			TOAST_WIDTH,
			cachedToastHeight
		);

		int iconY = boxY + (cachedToastHeight - ICON_SIZE) / 2;
		graphics.blit(
			RenderPipelines.GUI_TEXTURED,
			TOAST_ICON,
			boxX + HORIZONTAL_PADDING,
			iconY,
			0,
			0,
			ICON_SIZE,
			ICON_SIZE,
			ICON_SIZE,
			ICON_SIZE
		);

		Font font = minecraft.font;
		int textX = HORIZONTAL_PADDING + ICON_SIZE + HORIZONTAL_PADDING;
		int textBlockHeight = textBlockHeight(font, cachedLines.size());
		int textY = boxY + (cachedToastHeight - textBlockHeight) / 2;
		graphics.pose().pushMatrix();
		graphics.pose().translate(boxX + textX, textY);
		graphics.pose().scale(TEXT_SCALE, TEXT_SCALE);
		int y = 0;
		int step = font.lineHeight + Math.round(LINE_GAP / TEXT_SCALE);
		for (FormattedCharSequence line : cachedLines) {
			graphics.text(font, line, 0, y, BODY_COLOR, false);
			y += step;
		}
		graphics.pose().popMatrix();
	}

	private static void tickPhase(long now) {
		switch (phase) {
			case SLIDING_IN -> {
				if (now - phaseStartMs >= SLIDE_MS) {
					phase = Phase.SHOWING;
					phaseStartMs = now;
					// Hold clock starts when fully in (or was already extended during slide-in).
					if (holdStartMs < phaseStartMs) {
						holdStartMs = phaseStartMs;
					}
				}
			}
			case SHOWING -> {
				if (now - holdStartMs >= HOLD_MS) {
					phase = Phase.SLIDING_OUT;
					phaseStartMs = now;
					Minecraft client = Minecraft.getInstance();
					if (client.player != null) {
						client.player.playSound(SoundEvents.UI_TOAST_OUT, 1.0f, 1.0f);
					}
				}
			}
			case SLIDING_OUT -> {
				if (now - phaseStartMs >= SLIDE_MS) {
					phase = Phase.HIDDEN;
				}
			}
			case HIDDEN -> {
			}
		}
	}

	/** 0 = fully on-screen, 1 = fully off to the right. */
	private static float slideOffsetFraction(long now) {
		return switch (phase) {
			case SLIDING_IN -> 1.0f - slideProgress(now - phaseStartMs);
			case SHOWING -> 0.0f;
			case SLIDING_OUT -> slideProgress(now - phaseStartMs);
			case HIDDEN -> 1.0f;
		};
	}

	private static float slideProgress(long elapsedMs) {
		return Mth.clamp(elapsedMs / (float) SLIDE_MS, 0.0f, 1.0f);
	}

	private static void ensureLayout(Font font) {
		if (cachedLines != null) {
			return;
		}
		int textX = HORIZONTAL_PADDING + ICON_SIZE + HORIZONTAL_PADDING;
		int textAreaWidth = TOAST_WIDTH - textX - HORIZONTAL_PADDING;
		cachedLines = font.split(BODY, (int) (textAreaWidth / TEXT_SCALE));
		int textBlockHeight = textBlockHeight(font, cachedLines.size());
		cachedToastHeight = Math.max(
			32,
			Math.max(ICON_SIZE + VERTICAL_PADDING * 2, textBlockHeight + VERTICAL_PADDING * 2)
		);
	}

	private static int textBlockHeight(Font font, int lineCount) {
		if (lineCount == 0) {
			return 0;
		}
		int scaledLine = Math.max(6, Math.round(font.lineHeight * TEXT_SCALE));
		return lineCount * scaledLine + (lineCount - 1) * LINE_GAP;
	}
}
