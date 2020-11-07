package com.baselet.plugin.gui;

public class EclipseGUI {

	private static Contributor contributor;

	public static void setContributor(Contributor contributor) {
		EclipseGUI.contributor = contributor;
	}

	public static void setPasteAvailable(boolean available) {
		if (contributor != null) {
			contributor.setPaste(available);
		}
	}

	public static void setUndoRedoAvailable(boolean undoAvailable, boolean redoAvailable) {
		if (contributor != null) {
			contributor.setUndoRedoAvailable(undoAvailable, redoAvailable);
		}
	}
}
