package com.baselet.plugin.gui;

import java.util.Collection;
import java.util.HashMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baselet.diagram.CurrentDiagram;
import com.baselet.diagram.DiagramHandler;
import com.baselet.diagram.DrawPanel;
import com.baselet.element.interfaces.GridElement;
import com.baselet.element.old.custom.CustomElementHandler;
import com.baselet.gui.CurrentGui;
import com.baselet.plugin.gui.Contributor.ActionName;

public class EclipseGUI {

	public enum Pane {
		PROPERTY, CUSTOMCODE, DIAGRAM
	}

	private static final Logger log = LoggerFactory.getLogger(EclipseGUI.class);

	private Editor editor;
	private final HashMap<DiagramHandler, Editor> diagrams;
	private Contributor contributor;

	// TODO@fab: needed?
	private static EclipseGUI current = new EclipseGUI();

	public static EclipseGUI getCurrent() {
		return current;
	}

	private EclipseGUI() {
		diagrams = new HashMap<DiagramHandler, Editor>();
	}

	public void diagramSelected(DiagramHandler handler) {
		// the menues are only visible if a diagram is selected. (contributor manages this)
		// AB: just update the export menu
		DrawPanel currentDiagram = CurrentGui.getInstance().getGui().getCurrentDiagram();
		if (currentDiagram == null) {
			return; // Possible if method is called at loading a palette
		}
		boolean enable = handler != null && !currentDiagram.getGridElements().isEmpty();
		contributor.setExportAsEnabled(enable);
	}

	public void enablePasteMenuEntry() {
		if (contributor != null) {
			contributor.setPaste(true);
		}
	}

	public CustomElementHandler getCurrentCustomHandler() {
		if (editor == null) {
			return null;
		}
		return editor.getCustomElementHandler();
	}

	public void setCustomElementSelected(boolean selected) {
		// TODO@fab
		if (editor != null && contributor != null) {
			contributor.setCustomElementSelected(selected);
		}
	}

	public void setCustomPanelEnabled(@SuppressWarnings("unused") boolean enable) {
		// TODO@fab
	}

	public void updateDiagramName(DiagramHandler diagram, @SuppressWarnings("unused") String name) {
		// TODO@fab called?
		Editor editor = diagrams.get(diagram);
		if (editor != null) {
			editor.diagramNameChanged();
		}
	}

	public void setDiagramChanged(DiagramHandler diagram, @SuppressWarnings("unused") boolean changed) {
		Editor editor = diagrams.get(diagram);
		if (editor != null) {
			editor.dirtyChanged();
		}
	}

	public void registerEditorForDiagramHandler(Editor editor, DiagramHandler handler) {
		diagrams.put(handler, editor);
	}

	public void setCurrentDiagramHandler(DiagramHandler handler) {
		CurrentDiagram.getInstance().setCurrentDiagramHandler(handler);
	}

	public void setCurrentEditor(Editor editor) {
		this.editor = editor;
	}

	public void panelDoAction(Pane pane, ActionName actionName) {
		// TODO@fab used?
		SWTOwnPropertyPane propertyPane = null;
		if (pane == Pane.PROPERTY) {
			propertyPane = editor.getPropertyPane();
		}
		// else if (pane == Pane.CUSTOMCODE) {
		// textpane = editor.getCustomPane();
		// }

		if (propertyPane != null) {
			switch (actionName) {
				case COPY:
					propertyPane.doCopy();
					break;
				case CUT:
					propertyPane.doCut();
					break;
				case PASTE:
					propertyPane.doPaste();
					break;
				case SELECTALL:
					propertyPane.doSelectAll();
					break;
				default:
					break;
			}
		}
	}

	public void setContributor(Contributor contributor) {
		this.contributor = contributor;
	}

	public void elementsSelected(Collection<GridElement> selectedElements) {
		// TODO@fab used?
		if (contributor != null) {
			contributor.setElementsSelected(selectedElements);
		}
	}

	public void setPaneFocused(final Pane pane) {
		// TODO@fab used?
		if (contributor != null) {
			// must be executed from within the SWT Display thread (see https://stackoverflow.com/questions/5980316/invalid-thread-access-error-with-java-swt)
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					contributor.setGlobalActionHandlers(pane);
				}
			});
		}
	}

	public void setValueOfZoomDisplay(int i) {
		// TODO@fab
		if (contributor != null) {
			contributor.updateZoomMenuRadioButton(i);
		}
	}

	public static void refreshWorkspace() {
		// TODO@fab what for?
		IWorkspaceRoot myWorkspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		try {
			myWorkspaceRoot.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			log.error("Error at refreshing the workspace", e);
		}
	}

	public boolean hasExtendedContextMenu() {
		// TODO@fab used?
		return false;
	}

	public boolean saveWindowSizeInConfig() {
		// TODO@fab used?
		return false;
	}
}
