package net.querz.mcaselector.ui.component;

/** Small UI-only description of the world currently represented by the chunk clipboard. */
public final class ClipboardContext {

	private static String sourceWorld;

	private ClipboardContext() {}

	public static void setSourceWorld(String sourceWorld) {
		ClipboardContext.sourceWorld = sourceWorld;
	}

	public static String getSourceWorld() {
		return sourceWorld;
	}
}
