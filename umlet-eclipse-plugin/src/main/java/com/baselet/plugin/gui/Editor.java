package com.baselet.plugin.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.part.EditorPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baselet.diagram.draw.helper.theme.Theme.PredefinedColors;
import com.baselet.diagram.draw.helper.theme.ThemeFactory;
import com.baselet.element.interfaces.Diagram;
import com.baselet.element.interfaces.GridElement;
import com.baselet.element.old.custom.CustomElementHandler;
import com.baselet.plugin.MainPlugin;
import com.baselet.plugin.gui.EclipseGUI.Pane;
import com.baselet.plugin.swt.DiagramIO;
import com.baselet.plugin.swt.IElementFactory;
import com.baselet.plugin.swt.SWTDiagramHandler;
import com.baselet.plugin.swt.SWTElementFactory;

public class Editor extends EditorPart {

	private final class OperationTargetToTextBridge implements IOperationTarget {
		@Override
		public boolean canDoOperation(int operation) {
			return operation < IOperationTarget.DUPLICATE && propertiesTextViewer.canDoOperation(operation);
		}

		@Override
		public void doOperation(int operation, List<GridElement> elements, Object value) {
			propertiesTextViewer.doOperation(operation);
		}
	}

	public interface IPaneListener {
		public void paneSelected(Pane paneType);
	}

	private static final Logger log = LoggerFactory.getLogger(Editor.class);

	private final UUID uuid = UUID.randomUUID();

	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO@fab
		monitor.done();
	}

	@Override
	public void doSaveAs() {
		// TODO@fab
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	private TextViewer propertiesTextViewer;

	private final Diagram diagram = new SWTDiagramHandler();

	private final IElementFactory factory = new SWTElementFactory();

	private DiagramViewer paletteViewer;

	private DiagramViewer diagramViewer;

	// actions used in popup menus
	private IWorkbenchAction copyAction;
	private IWorkbenchAction cutAction;
	private IWorkbenchAction pasteAction;
	private IWorkbenchAction selectAllAction;
	private IWorkbenchAction deleteAction;
	private IWorkbenchAction undoAction;
	private IWorkbenchAction redoAction;
	private final Map<String, Action> fgColorActions = new HashMap<String, Action>();
	private final Map<String, Action> bgColorActions = new HashMap<String, Action>();

	private IPaneListener paneListener;

	private OperationTargetToTextBridge targetToTextBridge;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		log.info("Call editor.init() " + uuid.toString());
		setSite(site);
		setInput(input);
		setPartName(input.getName());
		DiagramIO.readFromFile(getFile(input), diagram, factory);
	}

	private File getFile(IEditorInput input) throws PartInitException {
		if (input instanceof IFileEditorInput) { // Files opened from workspace
			return ((IFileEditorInput) input).getFile().getLocation().toFile();
		}
		else if (input instanceof org.eclipse.ui.ide.FileStoreEditorInput) { // Files from outside of the workspace (eg: edit current palette)
			return new File(((org.eclipse.ui.ide.FileStoreEditorInput) input).getURI());
		}
		else {
			throw new PartInitException("Editor input not supported.");
		}
	}

	@Override
	public boolean isDirty() {
		return diagramViewer.isDiagramChanged();
	}

	private final class DiagramSelectionToAttributesBinding implements ISelectionChangedListener, ITextListener {
		private boolean syncBack;
		private DiagramViewer source;

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			event.getSource();
			Object[] array = event.getStructuredSelection().toArray();
			if (array.length > 0) {
				if (syncBack) {
					DiagramViewer newSource = (DiagramViewer) event.getSource();
					if (source != newSource) {
						source = newSource;
					}
				}
				GridElement e = (GridElement) array[array.length - 1];
				propertiesTextViewer.getDocument().set(e.getPanelAttributes());
			}
			EclipseGUI.elementsSelected(array.length > 0);
			dirtyChanged();
		}

		public DiagramSelectionToAttributesBinding bidirectional() {
			syncBack = true;
			propertiesTextViewer.addTextListener(this);
			return this;
		}

		@Override
		public void textChanged(TextEvent event) {
			if (source != null) {
				source.setAttributesForSelected(propertiesTextViewer.getDocument().get());
			}
		}
	}

	private class PaneTypeOnFocus extends FocusAdapter {
		private final Pane paneType;
		private final DiagramViewer viewer;

		public PaneTypeOnFocus(Pane paneType) {
			this(paneType, null);
		}

		public PaneTypeOnFocus(Pane paneType, DiagramViewer viewer) {
			this.paneType = paneType;
			this.viewer = viewer;
		}

		@Override
		public void focusGained(FocusEvent e) {
			if (paneListener != null) {
				paneListener.paneSelected(paneType);
				if (viewer != null) {
					EclipseGUI.elementsSelected(!viewer.getSelection().isEmpty());
				}
			}
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		log.info("Call editor.createPartControl() " + uuid.toString());

		// create the three panels (to be decided if they should be moved to views...)
		// + sash (horizontal split)
		// +-- diagram pane (embed swing)
		// +-- sash (vertical split)
		// +----- palette pane : swt canvas
		// +----- properties, jface TextViewer
		SashForm mainForm = new SashForm(parent, SWT.HORIZONTAL);
		mainForm.setLayout(new FillLayout());
		diagramViewer = new DiagramViewer(mainForm);
		SashForm sidebarForm = new SashForm(mainForm, SWT.VERTICAL);
		paletteViewer = new DiagramViewer(sidebarForm);
		propertiesTextViewer = new TextViewer(sidebarForm, SWT.V_SCROLL | SWT.H_SCROLL);
		propertiesTextViewer.setInput(new Document("TODO: properties"));
		targetToTextBridge = new OperationTargetToTextBridge();

		diagramViewer.setInput(diagram);
		File paletteFile = MainPlugin.getDefault().getPaletteFile("UML Common Elements");
		setCurrentPalette(paletteFile, false);
		paletteViewer.setPaletteFor(diagramViewer);

		// set the pane type on focus events
		propertiesTextViewer.getControl().addFocusListener(new PaneTypeOnFocus(Pane.PROPERTY));
		paletteViewer.getControl().addFocusListener(new PaneTypeOnFocus(Pane.DIAGRAM, paletteViewer));
		diagramViewer.getControl().addFocusListener(new PaneTypeOnFocus(Pane.DIAGRAM, diagramViewer));

		// mark diagrams as exclusive (only one can have selected elements)
		diagramViewer.setExclusiveTo(paletteViewer);
		paletteViewer.setExclusiveTo(diagramViewer);

		// listen to selection changes in diagrams and update properties content
		diagramViewer.addSelectionChangedListener(new DiagramSelectionToAttributesBinding().bidirectional());
		paletteViewer.addSelectionChangedListener(new DiagramSelectionToAttributesBinding());

		createActions();

		// context menus
		MenuManager propertiesMM = new MenuManager();
		propertiesMM.add(copyAction);
		propertiesMM.add(cutAction);
		propertiesMM.add(pasteAction);
		propertiesMM.add(selectAllAction);
		Menu menu = propertiesMM.createContextMenu(propertiesTextViewer.getControl());
		propertiesTextViewer.getControl().setMenu(menu);

		MenuManager diagramMM = new MenuManager();
		menu = diagramMM.createContextMenu(diagramViewer.getControl());
		diagramViewer.getControl().setMenu(menu);
		diagramMM.setRemoveAllWhenShown(true);
		diagramMM.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				fillDiagramContextMenu(manager);
			}
		});

		MenuManager paletteMM = new MenuManager();
		paletteMM.add(copyAction);
		paletteMM.add(cutAction);
		paletteMM.add(pasteAction);
		paletteMM.add(selectAllAction);
		paletteMM.add(new Separator());
		paletteMM.add(createPalettesListSubmenu());
		menu = paletteMM.createContextMenu(paletteViewer.getControl());
		paletteViewer.getControl().setMenu(menu);
	}

	private void fillDiagramContextMenu(IMenuManager diagramMM) {
		diagramMM.add(deleteAction);
		diagramMM.add(copyAction);
		diagramMM.add(cutAction);
		diagramMM.add(pasteAction);
		diagramMM.add(selectAllAction);
		diagramMM.add(undoAction);
		diagramMM.add(redoAction);
		diagramMM.add(new Separator());
		if (!diagramViewer.getSelection().isEmpty()) {
			diagramMM.add(createColorSubmenu(true));
			diagramMM.add(createColorSubmenu(false));
		}
	}

	private void createActions() {
		deleteAction = ActionFactory.DELETE.create(getSite().getWorkbenchWindow());
		copyAction = ActionFactory.COPY.create(getSite().getWorkbenchWindow());
		cutAction = ActionFactory.CUT.create(getSite().getWorkbenchWindow());
		pasteAction = ActionFactory.PASTE.create(getSite().getWorkbenchWindow());
		selectAllAction = ActionFactory.SELECT_ALL.create(getSite().getWorkbenchWindow());
		undoAction = ActionFactory.UNDO.create(getSite().getWorkbenchWindow());
		redoAction = ActionFactory.REDO.create(getSite().getWorkbenchWindow());

		for (PredefinedColors color : ThemeFactory.getCurrentTheme().getColorMap().keySet()) {
			fgColorActions.put(color.name().toLowerCase(), new Action(color.name().toLowerCase()) {
				@Override
				public void run() {
					setColorOnSelection(getText(), true);
				}
			});
			bgColorActions.put(color.name().toLowerCase(), new Action(color.name().toLowerCase()) {
				@Override
				public void run() {
					setColorOnSelection(getText(), false);
				}
			});
		}
	}

	private void setColorOnSelection(String colorName, boolean fg) {
		IStructuredSelection selection = (IStructuredSelection) diagramViewer.getSelection();
		ArrayList<GridElement> selectedElements = new ArrayList<GridElement>();
		for (Object object : selection) {
			if (object instanceof GridElement) {
				GridElement ge = (GridElement) object;
				selectedElements.add(ge);
			}
		}
		int operation = fg ? IOperationTarget.SET_FG_COLOR : IOperationTarget.SET_BG_COLOR;
		diagramViewer.doOperation(operation, selectedElements, colorName);
	}

	private void disposeActions() {
		if (copyAction != null) {
			copyAction.dispose();
		}
		if (cutAction != null) {
			cutAction.dispose();
		}
		if (pasteAction != null) {
			pasteAction.dispose();
		}
		if (selectAllAction != null) {
			selectAllAction.dispose();
		}
	}

	private MenuManager createColorSubmenu(boolean fg) {
		MenuManager colorListMM = new MenuManager(fg ? "Foreground" : "Background");
		Collection<Action> colorActions = fg ? fgColorActions.values() : bgColorActions.values();
		for (Action setColorAction : colorActions) {
			colorListMM.add(setColorAction);
		}
		return colorListMM;
	}

	private MenuManager createPalettesListSubmenu() {
		MenuManager paletteListMM = new MenuManager("Palettes");
		List<String> paletteNames = MainPlugin.getDefault().getPaletteNames();
		for (String name : paletteNames) {
			paletteListMM.add(new Action(name) {
				@Override
				public void run() {
					String paletteName = getText();
					File paletteFile = MainPlugin.getDefault().getPaletteFile(paletteName);
					setCurrentPalette(paletteFile, true);
				}
			});
		}
		return paletteListMM;
	}

	private void setCurrentPalette(File paletteFile, boolean refreshViewer) {
		SWTDiagramHandler paletteDiagram = new SWTDiagramHandler();
		DiagramIO.readFromFile(paletteFile, paletteDiagram, factory);
		paletteViewer.setInput(paletteDiagram);
		if (refreshViewer) {
			paletteViewer.refresh();
		}
	}

	@Override
	public void setFocus() {
		log.info("Call editor.setFocus() " + uuid.toString());
		diagramViewer.getControl().setFocus();

	}

	@Override
	public void dispose() {
		super.dispose();
		disposeActions();
		log.info("Call editor.dispose( )" + uuid.toString());
	}

	void addPaneListener(IPaneListener listener) {
		// TODO@fab only one for now
		paneListener = listener;
	}

	void removePaneListener(IPaneListener listener) {
		// TODO@fab only one for now
		if (listener == paneListener) {
			paneListener = null;
		}
	}

	public ITextOperationTarget getPropertyPane() {
		return propertiesTextViewer;
	}

	private void dirtyChanged() {
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}

	public void diagramNameChanged() {
		org.eclipse.swt.widgets.Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				firePropertyChange(IWorkbenchPart.PROP_TITLE);
			}
		});
	}

	public CustomElementHandler getCustomElementHandler() {
		return null;
	}

	public void focusPropertyPane() {
		propertiesTextViewer.getControl().setFocus();
	}

	public IOperationTarget getOperationTarget() {
		if (paletteViewer.getControl().isFocusControl()) {
			return paletteViewer;
		}
		else if (propertiesTextViewer.getControl().isFocusControl()) {
			return targetToTextBridge;
		}
		return diagramViewer;
	}
}
