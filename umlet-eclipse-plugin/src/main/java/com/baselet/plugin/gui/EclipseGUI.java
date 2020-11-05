package com.baselet.plugin.gui;

import com.baselet.diagram.DiagramHandler;
import com.baselet.diagram.DrawPanel;
import com.baselet.gui.CurrentGui;

public class EclipseGUI {

	public enum Pane {
		PROPERTY, CUSTOMCODE, DIAGRAM
	}

	private static Contributor contributor;

	public static void diagramSelected(DiagramHandler handler) {
		// the menues are only visible if a diagram is selected. (contributor manages this)
		// AB: just update the export menu
		DrawPanel currentDiagram = CurrentGui.getInstance().getGui().getCurrentDiagram();
		if (currentDiagram == null) {
			return; // Possible if method is called at loading a palette
		}
		boolean enable = handler != null && !currentDiagram.getGridElements().isEmpty();
		contributor.setExportAsEnabled(enable);
	}

	public static void setContributor(Contributor contributor) {
		EclipseGUI.contributor = contributor;
	}

	public static void elementsSelected(boolean selected) {
		// TODO@fab used?
		if (contributor != null) {
			contributor.setElementsSelected(selected);
		}
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
