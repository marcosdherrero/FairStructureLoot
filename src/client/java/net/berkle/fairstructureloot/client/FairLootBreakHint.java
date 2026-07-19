package net.berkle.fairstructureloot.client;

/** Shows the sneak-to-break hint via our advancement-styled HUD overlay (not ToastManager). */
public final class FairLootBreakHint {

	private FairLootBreakHint() {
	}

	public static void show() {
		FairLootBreakHintOverlay.show();
	}
}
