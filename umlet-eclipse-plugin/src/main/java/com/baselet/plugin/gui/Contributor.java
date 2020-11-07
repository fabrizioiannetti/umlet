package com.baselet.plugin.gui;

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
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.EditorActionBarContributor;

import com.baselet.control.constants.MenuConstants;
import com.baselet.control.enums.Program;

public class Contributor extends EditorActionBarContributor {

	public enum ActionName {
		COPY, CUT, PASTE, SELECTALL, DELETE, UNDO, REDO
	}

	private static Map<ActionName, Integer> actionNameToTextOperation;
	{
		actionNameToTextOperation = new HashMap<Contributor.ActionName, Integer>();
		actionNameToTextOperation.put(ActionName.COPY, IOperationTarget.COPY);
		actionNameToTextOperation.put(ActionName.CUT, IOperationTarget.CUT);
		actionNameToTextOperation.put(ActionName.PASTE, IOperationTarget.PASTE);
		actionNameToTextOperation.put(ActionName.SELECTALL, IOperationTarget.SELECT_ALL);
		actionNameToTextOperation.put(ActionName.DELETE, IOperationTarget.DELETE);
		actionNameToTextOperation.put(ActionName.UNDO, IOperationTarget.UNDO);
		actionNameToTextOperation.put(ActionName.REDO, IOperationTarget.REDO);
	}

	private final MenuFactoryEclipse menuFactory = MenuFactoryEclipse.getInstance();

	private IAction undoActionGlobal;
	private IAction redoActionGlobal;
	private IAction printActionGlobal;

	// actions to execute on a pane
	private IAction copyAction;
	private IAction cutAction;
	private IAction pasteAction;
	private IAction selectAllAction;
	private Action deleteAction;

	private List<IAction> exportAsActionList;

	private IMenuManager zoomMenu;

	private Editor targetEditor;

	private Action createOperationTargetAction(final ActionName action) {
		Action propertyAction = new Action() {
			@Override
			public void run() {
				if (targetEditor != null) {
					IOperationTarget target = targetEditor.getOperationTarget();
					// Note: operation target has the same codes as text operation target
					Integer textOperation = actionNameToTextOperation.get(action);
					if (textOperation != null) {
						if (target.canDoOperation(textOperation)) {
							target.doOperation(textOperation, null, null);
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

		undoActionGlobal = createOperationTargetAction(ActionName.UNDO);
		redoActionGlobal = createOperationTargetAction(ActionName.REDO);
		printActionGlobal = menuFactory.createPrint();

		deleteAction = createOperationTargetAction(ActionName.DELETE);
		copyAction = createOperationTargetAction(ActionName.COPY);
		cutAction = createOperationTargetAction(ActionName.CUT);
		pasteAction = createOperationTargetAction(ActionName.PASTE);
		selectAllAction = createOperationTargetAction(ActionName.SELECTALL);

		undoActionGlobal.setEnabled(false);
		redoActionGlobal.setEnabled(false);
		deleteAction.setEnabled(false);
		copyAction.setEnabled(false);
		cutAction.setEnabled(false);
		pasteAction.setEnabled(false);

		setGlobalActionHandlers();
	}

	@Override
	public void contributeToMenu(IMenuManager manager) {
		EclipseGUI.setContributor(this);

		IMenuManager menu = new MenuManager(Program.getInstance().getProgramName().toString());
		IMenuManager custom = new MenuManager(MenuConstants.CUSTOM_ELEMENTS);
		IMenuManager help = new MenuManager(MenuConstants.HELP);
		manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, menu);

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
		pasteAction.setEnabled(value);
	}

	public void setElementsSelected(boolean selected) {
		deleteAction.setEnabled(selected);
		copyAction.setEnabled(selected);
		cutAction.setEnabled(selected);
	}

	public void setUndoRedoAvailable(boolean undoAvailable, boolean redoAvailable) {
		undoActionGlobal.setEnabled(undoAvailable);
		redoActionGlobal.setEnabled(redoAvailable);
	}

	private void setGlobalActionHandlers() {

		// Global actions which are always the same
		getActionBars().setGlobalActionHandler(ActionFactory.UNDO.getId(), undoActionGlobal);
		getActionBars().setGlobalActionHandler(ActionFactory.REDO.getId(), redoActionGlobal);
		getActionBars().setGlobalActionHandler(ActionFactory.PRINT.getId(), printActionGlobal);

		getActionBars().setGlobalActionHandler(ActionFactory.COPY.getId(), copyAction);
		getActionBars().setGlobalActionHandler(ActionFactory.CUT.getId(), cutAction);
		getActionBars().setGlobalActionHandler(ActionFactory.PASTE.getId(), pasteAction);
		getActionBars().setGlobalActionHandler(ActionFactory.DELETE.getId(), deleteAction);
		getActionBars().setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), selectAllAction);
		getActionBars().setGlobalActionHandler(ActionFactory.FIND.getId(), null);

		getActionBars().updateActionBars();
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
			setGlobalActionHandlers();
		}
		else {
			this.targetEditor = null;
		}
	}
}
