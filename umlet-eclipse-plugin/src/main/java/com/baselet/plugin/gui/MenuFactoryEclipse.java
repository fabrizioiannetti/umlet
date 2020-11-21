package com.baselet.plugin.gui;

import static com.baselet.control.constants.MenuConstants.ABOUT_PROGRAM;
import static com.baselet.control.constants.MenuConstants.CUSTOM_ELEMENTS_TUTORIAL;
import static com.baselet.control.constants.MenuConstants.EDIT_CURRENT_PALETTE;
import static com.baselet.control.constants.MenuConstants.EDIT_SELECTED;
import static com.baselet.control.constants.MenuConstants.EXPORT_AS;
import static com.baselet.control.constants.MenuConstants.GENERATE_CLASS;
import static com.baselet.control.constants.MenuConstants.GENERATE_CLASS_OPTIONS;
import static com.baselet.control.constants.MenuConstants.NEW_CE;
import static com.baselet.control.constants.MenuConstants.NEW_FROM_TEMPLATE;
import static com.baselet.control.constants.MenuConstants.ONLINE_HELP;
import static com.baselet.control.constants.MenuConstants.ONLINE_SAMPLE_DIAGRAMS;
import static com.baselet.control.constants.MenuConstants.OPTIONS;
import static com.baselet.control.constants.MenuConstants.PRINT;
import static com.baselet.control.constants.MenuConstants.PROGRAM_HOMEPAGE;
import static com.baselet.control.constants.MenuConstants.RATE_PROGRAM;
import static com.baselet.control.constants.MenuConstants.SEARCH;
import static com.baselet.control.constants.MenuConstants.VIDEO_TUTORIAL;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.program.Program;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baselet.control.constants.Constants;

public class MenuFactoryEclipse {

	private static final Logger log = LoggerFactory.getLogger(MenuFactoryEclipse.class);

	private static MenuFactoryEclipse instance = null;

	public static MenuFactoryEclipse getInstance() {
		if (instance == null) {
			instance = new MenuFactoryEclipse();
		}
		return instance;
	}

	public void doAction(final String menuItem, @SuppressWarnings("unused") final Object param) {
		log.info("doAction " + menuItem);
		// DiagramHandler actualHandler = CurrentDiagram.getInstance().getDiagramHandler();
		// Edit Palette cannot be put in a separate invokeLater thread, or otherwise getActivePage() will be null!
		if (menuItem.equals(EDIT_CURRENT_PALETTE)) {
		}
		else if (menuItem.equals(SEARCH)) {
			// TODO@fab
			// SwingUtilities.invokeLater(new Runnable() {
			// @Override
			// public void run() {
			// CurrentGui.getInstance().getGui().enableSearch(true);
			// }
			// });
		}
		// If the action is not overwritten, it is part of the default actions
		else {
			log.debug("super.doAction");
			if (menuItem.equals(ONLINE_HELP)) {
				openBrowser(com.baselet.control.enums.Program.getInstance().getWebsite() + "/faq.htm");
			}
			else if (menuItem.equals(ONLINE_SAMPLE_DIAGRAMS)) {
				openBrowser("http://www.itmeyer.at/umlet/uml2/");
			}
			else if (menuItem.equals(VIDEO_TUTORIAL)) {
				openBrowser("http://www.youtube.com/watch?v=3UHZedDtr28");
			}
			else if (menuItem.equals(PROGRAM_HOMEPAGE)) {
				openBrowser(com.baselet.control.enums.Program.getInstance().getWebsite());
			}
			else if (menuItem.equals(RATE_PROGRAM)) {
				openBrowser("http://marketplace.eclipse.org/content/umlet-uml-tool-fast-uml-diagrams");
			}
			else if (menuItem.equals(ABOUT_PROGRAM)) {
				MessageDialog.openInformation(null, "About UMLet", "UMLet version \n\nReleased under the terms of the\nGNU General Public License");
			}
			// super.doAction(menuItem, param);
			log.debug("super.doAction complete");
		}
		log.debug("doAction complete");
	}

	private void openBrowser(String url) {
		Program program = Program.findProgram("html");
		program.execute(url);
	}

	public Action createOptions() {
		return createAction(OPTIONS, null);
	}

	public Action createOnlineHelp() {
		return createAction(ONLINE_HELP, null);
	}

	public Action createOnlineSampleDiagrams() {
		return createAction(ONLINE_SAMPLE_DIAGRAMS, null);
	}

	public Action createVideoTutorial() {
		return createAction(VIDEO_TUTORIAL, null);
	}

	public Action createProgramHomepage() {
		return createAction(PROGRAM_HOMEPAGE, null);
	}

	public Action createRateProgram() {
		return createAction(RATE_PROGRAM, null);
	}

	public Action createAboutProgram() {
		return createAction(ABOUT_PROGRAM, null);
	}

	public Action createNewCustomElement() {
		return createAction(NEW_CE, null);
	}

	public Action createCustomElementsTutorial() {
		return createAction(CUSTOM_ELEMENTS_TUTORIAL, null);
	}

	public Action createPrint() {
		return createAction(PRINT, null);
	}

	public Action createSearch() {
		return createAction(SEARCH, null);
	}

	public Action createEditSelected() {
		return createAction(EDIT_SELECTED, null);
	}

	public Action createEditCurrentPalette() {
		return createAction(EDIT_CURRENT_PALETTE, null);
	}

	public List<IAction> createExportAsActions() {
		List<IAction> actions = new ArrayList<IAction>();

		for (final String format : Constants.exportFormatList) {
			actions.add(createAction(format.toUpperCase() + "...", EXPORT_AS, format));
		}

		return actions;
	}

	public IAction createGenerate() {
		return createAction(GENERATE_CLASS, null);
	}

	public IAction createGenerateOptions() {
		return createAction(GENERATE_CLASS_OPTIONS, null);
	}

	private final List<Action> aList = new ArrayList<Action>();

	public IMenuManager createNewCustomElementFromTemplate(@SuppressWarnings("unused") final Contributor con) {
		IMenuManager menu = new MenuManager(NEW_FROM_TEMPLATE);
		// TODO@fab no Main
		String[] templates = { "no templates yet" };
		for (String template : /* Main.getInstance().getTemplateNames() */templates) {
			Action a = createAction(template, NEW_FROM_TEMPLATE, template);
			menu.add(a);
			aList.add(a);
		}
		menu.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				// for (Action a : aList) {
				// // TODO@fab
				// a.setEnabled(!con.isCustomPanelEnabled());
				// }
			}
		});
		return menu;
	}

	private Action createAction(final String name, final String param) {
		return createAction(name, name, param);
	}

	private Action createAction(final String menuName, final String actionName, final Object param) {
		return createAction(menuName, actionName, param, IAction.AS_UNSPECIFIED);
	}

	private Action createAction(final String menuName, final String actionName, final Object param, int style) {
		return new Action(menuName, style) {
			@Override
			public void run() {
				doAction(actionName, param);
			}
		};
	}
}
