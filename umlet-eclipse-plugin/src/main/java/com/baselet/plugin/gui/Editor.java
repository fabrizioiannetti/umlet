package com.baselet.plugin.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.text.TextViewer;
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
import org.eclipse.ui.part.EditorPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baselet.element.Selector;
import com.baselet.element.interfaces.Diagram;
import com.baselet.element.interfaces.GridElement;
import com.baselet.element.interfaces.HasGridElements;
import com.baselet.element.old.custom.CustomElementHandler;
import com.baselet.plugin.MainPlugin;
import com.baselet.plugin.gui.EclipseGUI.Pane;
import com.baselet.plugin.swt.DiagramIO;
import com.baselet.plugin.swt.IElementFactory;
import com.baselet.plugin.swt.SWTDiagramHandler;
import com.baselet.plugin.swt.SWTElementFactory;

public class Editor extends EditorPart {

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

	private SashForm mainForm;

	private SashForm sidebarForm;

	private TextViewer propertiesTextViewer;

	private SWTOwnPropertyPane swtOwnPropertyPane;

	private final Diagram diagram = new SWTDiagramHandler();

	private final IElementFactory factory = new SWTElementFactory();

	private DiagramViewer paletteViewer;

	private DiagramViewer diagramViewer;

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
		// TODO@fab
		return false;
	}

	public class EclipseEditorSelector extends Selector {

		private final HasGridElements gridElementProvider;

		public EclipseEditorSelector(HasGridElements gridElementProvider) {
			this.gridElementProvider = gridElementProvider;
		}

		private final List<GridElement> selectedElements = new ArrayList<GridElement>();

		@Override
		public List<GridElement> getSelectedElements() {
			return selectedElements;
		}

		@Override
		public List<GridElement> getAllElements() {
			return gridElementProvider.getGridElements();
		}

		@Override
		public void doAfterSelect(GridElement e) {
			super.doAfterSelect(e);
			swtOwnPropertyPane.switchToElement(e);
		}
	}

	private class PaneTypeOnFocus extends FocusAdapter {
		private final Pane pane;

		public PaneTypeOnFocus(Pane pane) {
			super();
			this.pane = pane;
		}

		@Override
		public void focusGained(FocusEvent e) {
			getGui().setPaneFocused(pane);
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		getGui().setCurrentEditor(Editor.this); // must be done before initialization of DiagramHandler (eg: to set propertypanel text)

		log.info("Call editor.createPartControl() " + uuid.toString());

		// create the three panels (to be decided if they should be moved to views...)
		// + sash (horizontal split)
		// +-- diagram pane (embed swing)
		// +-- sash (vertical split)
		// +----- palette pane : swt canvas
		// +----- properties, jface TextViewer
		mainForm = new SashForm(parent, SWT.HORIZONTAL);
		mainForm.setLayout(new FillLayout());
		diagramViewer = new DiagramViewer(mainForm);
		sidebarForm = new SashForm(mainForm, SWT.VERTICAL);
		paletteViewer = new DiagramViewer(sidebarForm);
		propertiesTextViewer = new TextViewer(sidebarForm, SWT.V_SCROLL | SWT.H_SCROLL);
		propertiesTextViewer.setInput(new Document("TODO: properties"));
		swtOwnPropertyPane = new SWTOwnPropertyPane(propertiesTextViewer);
		propertiesTextViewer.addTextListener(new ITextListener() {
			@Override
			public void textChanged(TextEvent event) {
				swtOwnPropertyPane.updateGridElement();
			}
		});
		diagramViewer.setInput(diagram);
		File paletteFile = MainPlugin.getDefault().getPaletteFile("UML Common Elements");
		setCurrentPalette(paletteFile, false);

		// set the pane type on focus events
		propertiesTextViewer.getControl().addFocusListener(new PaneTypeOnFocus(Pane.PROPERTY));
		paletteViewer.getControl().addFocusListener(new PaneTypeOnFocus(Pane.DIAGRAM));
		diagramViewer.getControl().addFocusListener(new PaneTypeOnFocus(Pane.DIAGRAM));

		MenuManager propertiesMM = new MenuManager();
		propertiesMM.add(ActionFactory.COPY.create(getSite().getWorkbenchWindow()));
		propertiesMM.add(ActionFactory.CUT.create(getSite().getWorkbenchWindow()));
		propertiesMM.add(ActionFactory.PASTE.create(getSite().getWorkbenchWindow()));
		propertiesMM.add(ActionFactory.SELECT_ALL.create(getSite().getWorkbenchWindow()));
		Menu menu = propertiesMM.createContextMenu(propertiesTextViewer.getControl());
		propertiesTextViewer.getControl().setMenu(menu);

		MenuManager diagramMM = new MenuManager();
		diagramMM.add(ActionFactory.COPY.create(getSite().getWorkbenchWindow()));
		diagramMM.add(ActionFactory.CUT.create(getSite().getWorkbenchWindow()));
		diagramMM.add(ActionFactory.PASTE.create(getSite().getWorkbenchWindow()));
		diagramMM.add(ActionFactory.SELECT_ALL.create(getSite().getWorkbenchWindow()));
		menu = diagramMM.createContextMenu(diagramViewer.getControl());
		diagramViewer.getControl().setMenu(menu);

		MenuManager paletteMM = new MenuManager();
		paletteMM.add(ActionFactory.COPY.create(getSite().getWorkbenchWindow()));
		paletteMM.add(ActionFactory.CUT.create(getSite().getWorkbenchWindow()));
		paletteMM.add(ActionFactory.PASTE.create(getSite().getWorkbenchWindow()));
		paletteMM.add(ActionFactory.SELECT_ALL.create(getSite().getWorkbenchWindow()));
		paletteMM.add(new Separator());
		MenuManager paletteListMM = new MenuManager("Palettes");
		paletteMM.add(paletteListMM);
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
		menu = paletteMM.createContextMenu(paletteViewer.getControl());
		paletteViewer.getControl().setMenu(menu);
	}

	private void setCurrentPalette(File paletteFile, boolean refreshViewer) {
		SWTDiagramHandler paletteDiagram = new SWTDiagramHandler();
		DiagramIO.readFromFile(paletteFile, paletteDiagram, factory);
		paletteViewer.setInput(paletteDiagram);
		if (refreshViewer) {
			paletteViewer.refresh();
		}
	}

	private EclipseGUI getGui() {
		return EclipseGUI.getCurrent();
	}

	@Override
	public void setFocus() {
		log.info("Call editor.setFocus() " + uuid.toString());
		getGui().setCurrentEditor(this);
	}

	@Override
	public void dispose() {
		super.dispose();
		log.info("Call editor.dispose( )" + uuid.toString());
	}

	public SWTOwnPropertyPane getPropertyPane() {
		return swtOwnPropertyPane;
	}

	public void dirtyChanged() {
		org.eclipse.swt.widgets.Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				firePropertyChange(IEditorPart.PROP_DIRTY);
			}
		});
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
}
