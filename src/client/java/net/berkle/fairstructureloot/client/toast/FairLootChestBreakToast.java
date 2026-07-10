package net.berkle.fairstructureloot.client.toast;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

import net.berkle.fairstructureloot.FairStructureLootMain;
import net.berkle.fairstructureloot.client.FairLootTriangleIcon;

/** Advancement-style toast (160px wide) with the three-triangle fair-loot icon. */
public final class FairLootChestBreakToast implements Toast {

	public static final Object TOKEN = new Object();

	private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("toast/advancement");
	private static final Identifier TOAST_ICON = Identifier.fromNamespaceAndPath(
		FairStructureLootMain.MOD_ID,
		"textures/gui/toast_icon.png"
	);

	private static final int HORIZONTAL_PADDING = 6;
	private static final float TEXT_SCALE = 0.75f;
	private static final int LINE_GAP = 2;
	private static final int VERTICAL_PADDING = 8;
	private static final int ICON_SCALE = 2;
	private static final int BODY_COLOR = 0xFFD8D8D8;

	public static final int DISPLAY_TIME = 12000;

	private static final Component BODY = Component.literal(
		"This is a loot container. Every player can get loot from here. "
			+ "Hold sneak and break the container to break it for all players."
	).withStyle(ChatFormatting.GRAY);

	private static FairLootChestBreakToast instance;

	private final int iconX;
	private final int textX;
	private final List<FormattedCharSequence> wrappedLines;
	private final int toastHeight;
	private Toast.Visibility wantedVisibility = Toast.Visibility.HIDE;
	private long lastShownAt = -1L;
	private boolean restartRequested;

	public FairLootChestBreakToast(Font font) {
		int iconWidth = FairLootTriangleIcon.toastIconWidth(ICON_SCALE);
		this.iconX = HORIZONTAL_PADDING;
		this.textX = HORIZONTAL_PADDING + iconWidth + HORIZONTAL_PADDING;
		int textAreaWidth = DEFAULT_WIDTH - textX - HORIZONTAL_PADDING;
		this.wrappedLines = font.split(BODY, (int) (textAreaWidth / TEXT_SCALE));
		int iconHeight = FairLootTriangleIcon.toastIconHeight(ICON_SCALE);
		this.toastHeight = Math.max(
			SLOT_HEIGHT,
			Math.max(iconHeight + VERTICAL_PADDING * 2, textBlockHeight(font) + VERTICAL_PADDING * 2)
		);
	}

	public static void show(ToastManager manager, Font font) {
		FairLootChestBreakToast existing = manager.getToast(FairLootChestBreakToast.class, TOKEN);
		if (existing != null) {
			existing.requestRestart();
			return;
		}
		if (instance == null) {
			instance = new FairLootChestBreakToast(font);
		}
		manager.addToast(instance);
	}

	private void requestRestart() {
		restartRequested = true;
		wantedVisibility = Toast.Visibility.SHOW;
	}

	@Override
	public Object getToken() {
		return TOKEN;
	}

	@Override
	public int width() {
		return DEFAULT_WIDTH;
	}

	private int scaledLineHeight(Font font) {
		return Math.max(6, Math.round(font.lineHeight * TEXT_SCALE));
	}

	private int lineStepUnscaled(Font font) {
		return font.lineHeight + Math.round(LINE_GAP / TEXT_SCALE);
	}

	private int textBlockHeight(Font font) {
		int lineCount = wrappedLines.size();
		if (lineCount == 0) {
			return 0;
		}
		return lineCount * scaledLineHeight(font) + (lineCount - 1) * LINE_GAP;
	}

	@Override
	public int height() {
		return toastHeight;
	}

	@Override
	public int occcupiedSlotCount() {
		return (height() + SLOT_HEIGHT - 1) / SLOT_HEIGHT;
	}

	@Override
	public Toast.Visibility getWantedVisibility() {
		return wantedVisibility;
	}

	@Override
	public void update(ToastManager manager, long timeSinceVisible) {
		if (restartRequested) {
			lastShownAt = timeSinceVisible;
			restartRequested = false;
		} else if (lastShownAt < 0L) {
			lastShownAt = timeSinceVisible;
		}

		long elapsed = timeSinceVisible - lastShownAt;
		wantedVisibility = elapsed >= DISPLAY_TIME * manager.getNotificationDisplayTimeMultiplier()
			? Toast.Visibility.HIDE
			: Toast.Visibility.SHOW;
	}

	@Override
	public SoundEvent getSoundEvent() {
		return SoundEvents.UI_TOAST_IN;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long timeSinceVisible) {
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, 0, 0, width(), toastHeight);

		int iconWidth = FairLootTriangleIcon.toastIconWidth(ICON_SCALE);
		int iconHeight = FairLootTriangleIcon.toastIconHeight(ICON_SCALE);
		int iconY = (toastHeight - iconHeight) / 2;
		graphics.blit(
			RenderPipelines.GUI_TEXTURED,
			TOAST_ICON,
			iconX,
			iconY,
			0,
			0,
			iconWidth,
			iconHeight,
			iconWidth,
			iconHeight
		);

		int textBlockHeight = textBlockHeight(font);
		int textY = (toastHeight - textBlockHeight) / 2;
		graphics.pose().pushMatrix();
		graphics.pose().translate(textX, textY);
		graphics.pose().scale(TEXT_SCALE, TEXT_SCALE);
		int y = 0;
		int step = lineStepUnscaled(font);
		for (FormattedCharSequence line : wrappedLines) {
			graphics.text(font, line, 0, y, BODY_COLOR, false);
			y += step;
		}
		graphics.pose().popMatrix();
	}
}
