package com.baselet.plugin.gui;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.EditorActionBarContributor;

import com.baselet.control.constants.MenuConstants;
import com.baselet.control.enums.Program;
import com.baselet.element.interfaces.GridElement;
import com.baselet.plugin.gui.EclipseGUI.Pane;
import com.baselet.plugin.gui.Editor.IPaneListener;

public class Contributor extends EditorActionBarContributor implements IPaneListener {

	public enum ActionName {
		COPY, CUT, PASTE, SELECTALL
	}

	private static Map<ActionName, Integer> actionNameToTextOperation;
	{
		actionNameToTextOperation = new HashMap<Contributor.ActionName, Integer>();
		actionNameToTextOperation.put(ActionName.COPY, ITextOperationTarget.COPY);
		actionNameToTextOperation.put(ActionName.CUT, ITextOperationTarget.CUT);
		actionNameToTextOperation.put(ActionName.PASTE, ITextOperationTarget.PASTE);
		actionNameToTextOperation.put(ActionName.SELECTALL, ITextOperationTarget.SELECT_ALL);
	}

	private final MenuFactoryEclipse menuFactory = MenuFactoryEclipse.getInstance();

	private IAction customnew;
	private IAction customedit;
	private IAction undoActionGlobal;
	private IAction redoActionGlobal;
	private IAction printActionGlobal;
	private IAction cutActionDiagram;
	private IAction pasteActionDiagram;
	private IAction deleteActionDiagram;
	private IAction searchActionDiagram;
	private IAction copyActionPropPanel;
	private IAction cutActionPropPanel;
	private IAction pasteActionPropPanel;
	private IAction selectAllActionPropPanel;

	private List<IAction> exportAsActionList;

	private boolean customPanelEnabled;
	private boolean custom_element_selected;

	private IMenuManager zoomMenu;

	private Editor targetEditor;

	private Action copyActionDiagPanel;

	private Action cutActionDiagPanel;

	private Action pasteActionDiagPanel;

	private Action selectAllActionDiagPanel;

	public Contributor() {
		customPanelEnabled = false;
		custom_element_selected = false;
	}

	private Action createPanelAction(final ActionName action) {
		Action propertyAction = new Action() {
			@Override
			public void run() {
				if (targetEditor != null) {
					ITextOperationTarget propertyPane = targetEditor.getPropertyPane();
					Integer textOperation = actionNameToTextOperation.get(action);
					if (textOperation != null) {
						if (propertyPane.canDoOperation(textOperation)) {
							propertyPane.doOperation(textOperation);
						}
					}
				}
			}
		};
		return propertyAction;
	}

	private Action createDiagramAction(final ActionName action) {
		Action propertyAction = new Action() {
			@Override
			public void run() {
				if (targetEditor != null) {
					IOperationTarget target = targetEditor.getOperationTarget();
					// Note: operation target has the same codes as text operation target
					Integer textOperation = actionNameToTextOperation.get(action);
					if (textOperation != null) {
						if (target.canDoOperation(textOperation)) {
							target.doOperation(textOperation);
						}
					}
				}
			}
		};
		return propertyAction;
	}

	@Override
	public void init(IActionBars actionBars) {
		super.init(actionBars);

		customedit = menuFactory.createEditSelected();
		customedit.setEnabled(false);

		undoActionGlobal = menuFactory.createUndo();
		redoActionGlobal = menuFactory.createRedo();
		printActionGlobal = menuFactory.createPrint();

		cutActionDiagram = menuFactory.createCut();
		cutActionDiagram.setEnabled(false);
		pasteActionDiagram = menuFactory.createPaste();
		pasteActionDiagram.setEnabled(false);
		deleteActionDiagram = menuFactory.createDelete();
		deleteActionDiagram.setEnabled(false);
		searchActionDiagram = menuFactory.createSearch();
		menuFactory.createCopy();
		menuFactory.createSelectAll();

		copyActionPropPanel = createPanelAction(ActionName.COPY);
		cutActionPropPanel = createPanelAction(ActionName.CUT);
		pasteActionPropPanel = createPanelAction(ActionName.PASTE);
		selectAllActionPropPanel = createPanelAction(ActionName.SELECTALL);

		copyActionDiagPanel = createDiagramAction(ActionName.COPY);
		cutActionDiagPanel = createDiagramAction(ActionName.CUT);
		pasteActionDiagPanel = createDiagramAction(ActionName.PASTE);
		selectAllActionDiagPanel = createDiagramAction(ActionName.SELECTALL);

		setGlobalActionHandlers(Pane.DIAGRAM);
	}

	@Override
	public void contributeToMenu(IMenuManager manager) {
		EclipseGUI.setContributor(this);

		IMenuManager menu = new MenuManager(Program.getInstance().getProgramName().toString());
		IMenuManager custom = new MenuManager(MenuConstants.CUSTOM_ELEMENTS);
		IMenuManager help = new MenuManager(MenuConstants.HELP);
		manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, menu);

		custom.add(customnew = menuFactory.createNewCustomElement());
		custom.add(menuFactory.createNewCustomElementFromTemplate(this));
		custom.add(new Separator());
		custom.add(menuFactory.createCustomElementsTutorial());

		help.add(menuFactory.createOnlineHelp());
		help.add(menuFactory.createOnlineSampleDiagrams());
		help.add(menuFactory.createVideoTutorial());
		help.add(new Separator());
		help.add(menuFactory.createProgramHomepage());
		help.add(menuFactory.createRateProgram());
		help.add(new Separator());
		help.add(menuFactory.createAboutProgram());

		menu.add(menuFactory.createGenerate());
		menu.add(menuFactory.createGenerateOptions());

		zoomMenu = menuFactory.createZoom();
		menu.add(zoomMenu);

		exportAsActionList = menuFactory.createExportAsActions();
		IMenuManager export = new MenuManager("Export as");
		for (IAction action : exportAsActionList) {
			export.add(action);
		}
		menu.add(export);

		menu.add(menuFactory.createEditCurrentPalette());
		menu.add(custom);
		menu.add(menuFactory.createMailTo());
		menu.add(new Separator());
		menu.add(help);
		menu.add(menuFactory.createOptions());
	}

	public void setExportAsEnabled(boolean enabled) {
		// AB: We cannot disable the MenuManager, so we have to disable every entry in the export menu :P
		for (IAction action : exportAsActionList) {
			action.setEnabled(enabled);
		}
	}

	public void setPaste(boolean value) {
		pasteActionDiagram.setEnabled(value);
	}

	public void setCustomElementSelected(boolean selected) {
		custom_element_selected = selected;
		customedit.setEnabled(selected && !customPanelEnabled);
	}

	public void setElementsSelected(Collection<GridElement> selectedElements) {
		if (selectedElements.isEmpty()) {
			deleteActionDiagram.setEnabled(false);
			cutActionDiagram.setEnabled(false);
		}
		else {
			cutActionDiagram.setEnabled(true);
			deleteActionDiagram.setEnabled(true);
		}
	}

	public boolean isCustomPanelEnabled() {
		return customPanelEnabled;
	}

	public void setCustomPanelEnabled(boolean enable) {
		customPanelEnabled = enable;
		customedit.setEnabled(!enable && custom_element_selected);
		customnew.setEnabled(!enable);
		searchActionDiagram.setEnabled(!enable);
	}

	public void setGlobalActionHandlers(Pane focusedPane) {

		// Global actions which are always the same
		getActionBars().setGlobalActionHandler(ActionFactory.UNDO.getId(), undoActionGlobal);
		getActionBars().setGlobalActionHandler(ActionFactory.REDO.getId(), redoActionGlobal);
		getActionBars().setGlobalActionHandler(ActionFactory.PRINT.getId(), printActionGlobal);

		// Specific actions depending on the active pane}
		if (focusedPane == Pane.DIAGRAM) {
			getActionBars().setGlobalActionHandler(ActionFactory.COPY.getId(), copyActionDiagPanel);
			getActionBars().setGlobalActionHandler(ActionFactory.CUT.getId(), cutActionDiagPanel);
			getActionBars().setGlobalActionHandler(ActionFactory.PASTE.getId(), pasteActionDiagPanel);
			getActionBars().setGlobalActionHandler(ActionFactory.DELETE.getId(), null);
			getActionBars().setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), selectAllActionDiagPanel);
			getActionBars().setGlobalActionHandler(ActionFactory.FIND.getId(), null);
		}
		else if (focusedPane == Pane.CUSTOMCODE) {
			// TODO@fab unsupported for now
		}
		else if (focusedPane == Pane.PROPERTY) {
			getActionBars().setGlobalActionHandler(ActionFactory.COPY.getId(), copyActionPropPanel);
			getActionBars().setGlobalActionHandler(ActionFactory.CUT.getId(), cutActionPropPanel);
			getActionBars().setGlobalActionHandler(ActionFactory.PASTE.getId(), pasteActionPropPanel);
			getActionBars().setGlobalActionHandler(ActionFactory.DELETE.getId(), null);
			getActionBars().setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), selectAllActionPropPanel);
			getActionBars().setGlobalActionHandler(ActionFactory.FIND.getId(), null);
		}

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				getActionBars().updateActionBars();
			}
		});
	}

	public void updateZoomMenuRadioButton(int newGridSize) {
		for (IContributionItem item : zoomMenu.getItems()) {
			IAction action = ((ActionContributionItem) item).getAction();
			int actionGridSize = Integer.parseInt(action.getText().substring(0, action.getText().length() - 2));
			if (actionGridSize == newGridSize) {
				action.setChecked(true);
			}
			else {
				action.setChecked(false);
			}
		}
	}

	@Override
	public void setActiveEditor(IEditorPart targetEditor) {
		if (targetEditor instanceof Editor) {
			this.targetEditor = (Editor) targetEditor;
			this.targetEditor.addPaneListener(this);
		}
		else {
			this.targetEditor = null;
		}
	}

	@Override
	public void paneSelected(Pane paneType) {
		setGlobalActionHandlers(paneType);
	}
}
