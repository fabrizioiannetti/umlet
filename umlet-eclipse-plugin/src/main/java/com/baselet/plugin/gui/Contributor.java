package com.baselet.plugin.gui;

import java.io.File;
import java.util.ArrayList;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.EditorActionBarContributor;

import com.baselet.control.constants.Constants;
import com.baselet.control.constants.MenuConstants;
import com.baselet.control.enums.Program;
import com.baselet.plugin.swt.SWTOutputHandler;

public class Contributor extends EditorActionBarContributor {

	public enum ActionName {
		COPY, CUT, PASTE, SELECTALL, DELETE, UNDO, REDO, ZOOM
	}

	private static Map<ActionName, Integer> actionNameToOperation;
	{
		actionNameToOperation = new HashMap<Contributor.ActionName, Integer>();
		actionNameToOperation.put(ActionName.COPY, IOperationTarget.COPY);
		actionNameToOperation.put(ActionName.CUT, IOperationTarget.CUT);
		actionNameToOperation.put(ActionName.PASTE, IOperationTarget.PASTE);
		actionNameToOperation.put(ActionName.SELECTALL, IOperationTarget.SELECT_ALL);
		actionNameToOperation.put(ActionName.DELETE, IOperationTarget.DELETE);
		actionNameToOperation.put(ActionName.UNDO, IOperationTarget.UNDO);
		actionNameToOperation.put(ActionName.REDO, IOperationTarget.REDO);
	}

	private final MenuFactoryEclipse menuFactory = MenuFactoryEclipse.getInstance();

	private IAction undoAction;
	private IAction redoAction;
	private IAction printAction;

	// actions to execute on a pane
	private IAction copyAction;
	private IAction cutAction;
	private IAction pasteAction;
	private IAction selectAllAction;
	private IAction deleteAction;

	private List<IAction> exportAsActionList;

	private IMenuManager zoomMenu;

	private Editor targetEditor;

	private IAction createOperationTargetAction(final ActionName action, final Object value, String name, int style) {
		Action propertyAction = new Action(name, style) {
			@Override
			public void run() {
				if (targetEditor != null) {
					IOperationTarget target = targetEditor.getOperationTarget();
					// Note: operation target has the same codes as text operation target
					Integer textOperation = actionNameToOperation.get(action);
					if (textOperation != null) {
						if (target.canDoOperation(textOperation)) {
							target.doOperation(textOperation, null, value);
						}
					}
				}
			}
		};
		return propertyAction;
	}

	private IAction createOperationTargetAction(ActionName action) {
		return createOperationTargetAction(action, null, null, IAction.AS_PUSH_BUTTON);
	}

	@Override
	public void init(IActionBars actionBars) {
		super.init(actionBars);

		undoAction = createOperationTargetAction(ActionName.UNDO);
		redoAction = createOperationTargetAction(ActionName.REDO);
		printAction = menuFactory.createPrint();

		deleteAction = createOperationTargetAction(ActionName.DELETE);
		copyAction = createOperationTargetAction(ActionName.COPY);
		cutAction = createOperationTargetAction(ActionName.CUT);
		pasteAction = createOperationTargetAction(ActionName.PASTE);
		selectAllAction = createOperationTargetAction(ActionName.SELECTALL);

		undoAction.setEnabled(false);
		redoAction.setEnabled(false);
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

		zoomMenu = createZoom();
		menu.add(zoomMenu);

		exportAsActionList = createExportAsActions();
		IMenuManager export = new MenuManager("Export as");
		for (IAction action : exportAsActionList) {
			export.add(action);
		}
		menu.add(export);

		menu.add(menuFactory.createEditCurrentPalette());
		menu.add(custom);
		menu.add(new Separator());
		menu.add(help);
		menu.add(menuFactory.createOptions());
	}

	private IMenuManager createZoom() {
		final IMenuManager zoomMenu = new MenuManager("Zoom");
		for (int zoom = 10; zoom <= 200; zoom += 10) {
			IAction action = createOperationTargetAction(ActionName.ZOOM, Integer.valueOf(zoom), zoom + "%", IAction.AS_RADIO_BUTTON);
			action.setChecked(zoom == 100);
			zoomMenu.add(action);
		}
		return zoomMenu;
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
		undoAction.setEnabled(undoAvailable);
		redoAction.setEnabled(redoAvailable);
	}

	private void setGlobalActionHandlers() {

		// Global actions which are always the same
		getActionBars().setGlobalActionHandler(ActionFactory.UNDO.getId(), undoAction);
		getActionBars().setGlobalActionHandler(ActionFactory.REDO.getId(), redoAction);
		getActionBars().setGlobalActionHandler(ActionFactory.PRINT.getId(), printAction);

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

	public List<IAction> createExportAsActions() {
		List<IAction> actions = new ArrayList<IAction>();
		for (final String format : Constants.exportFormatList) {
			Action action = new Action(format.toUpperCase() + "...") {
				@Override
				public void run() {
					FileDialog d = new FileDialog(getPage().getWorkbenchWindow().getShell(), SWT.SAVE);
					d.setText("Export to...");
					String exportFile = d.open();
					if (exportFile != null) {
						try {
							File file = new File(exportFile);
							SWTOutputHandler.createAndOutputToFile(format, file, targetEditor.getSelectedOrAllInDiagram());
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			};
			actions.add(action);
		}
		return actions;
	}

}
