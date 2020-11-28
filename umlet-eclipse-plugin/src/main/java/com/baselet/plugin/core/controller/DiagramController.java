package com.baselet.plugin.core.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.baselet.command.Command;
import com.baselet.control.constants.Constants;
import com.baselet.element.Selector;
import com.baselet.element.interfaces.GridElement;
import com.baselet.plugin.core.DiagramModel;
import com.baselet.plugin.core.IClipboard;
import com.baselet.plugin.core.IElementFactory;

/**
 * Controller to apply modifications to the diagram.
 *
 * It extends the Baselet common undo/redo operations.
 */
public class DiagramController extends Controller {
	private final DiagramModel diagram;
	private final Selector selector;
	private final Runnable uiUpdater;
	private final IElementFactory elementFactory;
	private final IClipboard clipboard;

	public DiagramController(DiagramModel diagram, Selector selector, Runnable uiUpdater, IElementFactory elementFactory, IClipboard clipboard) {
		super();
		this.diagram = diagram;
		this.selector = selector;
		this.uiUpdater = uiUpdater;
		this.elementFactory = elementFactory;
		this.clipboard = clipboard;
	}

	public class Paste extends Command {
		private final List<GridElement> elements = new ArrayList<GridElement>();

		public Paste() {
			clipboard.pasteElements(elements, diagram);
			Selector.replaceGroupsWithNewGroups(elements, selector);
			com.baselet.control.basics.geom.Rectangle boundingBox = diagram.getBoundingBox(0, elements);
			int xOffset = -boundingBox.x + Constants.PASTE_DISPLACEMENT_GRIDS * Constants.DEFAULTGRIDSIZE;
			int yOffset = -boundingBox.y + Constants.PASTE_DISPLACEMENT_GRIDS * Constants.DEFAULTGRIDSIZE;
			for (GridElement element : elements) {
				element.setLocationDifference(xOffset, yOffset);
			}
		}

		@Override
		public void execute() {
			diagram.getGridElements().addAll(elements);
			selector.selectOnly(elements);
			uiUpdater.run();
		}

		@Override
		public void undo() {
			diagram.getGridElements().removeAll(elements);
		}
	}

	public class Delete extends Command {
		private final List<GridElement> elements = new ArrayList<GridElement>();

		@Override
		public void execute() {
			List<GridElement> selectedElements = selector.getSelectedElements();
			if (!selectedElements.isEmpty()) {
				diagram.getGridElements().removeAll(selectedElements);
				uiUpdater.run();
			}
			elements.addAll(selectedElements);
		}

		@Override
		public void undo() {
			diagram.getGridElements().addAll(elements);
		}
	}

	public class Duplicate extends Command {
		private final List<GridElement> elements = new ArrayList<GridElement>();

		public Duplicate() {
			this(selector.getSelectedElements(), false);
		}

		public Duplicate(List<GridElement> sourceElements, boolean moveToOrigin) {
			for (GridElement element : sourceElements) {
				elements.add(elementFactory.create(element, diagram));
			}
			Selector.replaceGroupsWithNewGroups(elements, selector);
			int xOffset = Constants.PASTE_DISPLACEMENT_GRIDS * Constants.DEFAULTGRIDSIZE;
			int yOffset = Constants.PASTE_DISPLACEMENT_GRIDS * Constants.DEFAULTGRIDSIZE;
			for (GridElement element : elements) {
				if (moveToOrigin) {
					element.setLocation(xOffset, yOffset);
				}
				else {
					element.setLocationDifference(xOffset, yOffset);
				}
			}
		}

		@Override
		public void execute() {
			diagram.getGridElements().addAll(elements);
			selector.selectOnly(elements);
			uiUpdater.run();
		}

		@Override
		public void undo() {
			diagram.getGridElements().removeAll(elements);
		}
	}

	public static class ChangeElementSettings extends Command {
		private final static String ATTRIBUTES_TEXT_KEY = "allAttributes";

		private final String key;
		private final Map<GridElement, String> elementValueMap;
		private Map<GridElement, String> oldValue;

		public ChangeElementSettings(String attributes, Collection<GridElement> element) {
			this(ATTRIBUTES_TEXT_KEY, createSingleValueMap(attributes, element));
		}

		public ChangeElementSettings(String key, String value, Collection<GridElement> element) {
			this(key, createSingleValueMap(value, element));
		}

		public ChangeElementSettings(String key, Map<GridElement, String> elementValueMap) {
			this.key = key;
			this.elementValueMap = elementValueMap;
		}

		@Override
		public void execute() {
			oldValue = new HashMap<GridElement, String>();

			for (Entry<GridElement, String> entry : elementValueMap.entrySet()) {
				GridElement e = entry.getKey();
				if (key == ATTRIBUTES_TEXT_KEY) {
					oldValue.put(e, e.getPanelAttributes());
					e.setPanelAttributes(entry.getValue());
				}
				else {
					oldValue.put(e, e.getSetting(key));
					e.setProperty(key, entry.getValue());
				}
			}
		}

		@Override
		public void undo() {
			for (Entry<GridElement, String> entry : oldValue.entrySet()) {
				if (key == ATTRIBUTES_TEXT_KEY) {
					entry.getKey().setPanelAttributes(entry.getValue());
				}
				else {
					entry.getKey().setProperty(key, entry.getValue());
				}
			}
		}

		private static Map<GridElement, String> createSingleValueMap(String value, Collection<GridElement> elements) {
			Map<GridElement, String> singleValueMap = new HashMap<GridElement, String>(1);
			for (GridElement e : elements) {
				singleValueMap.put(e, value);
			}
			return singleValueMap;
		}
	}

	// TODO@fab: needed only for drag (Move/Macro commands)
	@Override
	public void executeCommand(Command newCommand) {
		super.executeCommand(newCommand);
	}

	public void deleteSelected() {
		executeCommand(new Delete());
	}

	public void copy() {
		clipboard.copyElements(selector.getSelectedElements());
	}

	public void paste() {
		executeCommand(new Paste());
	}

	public void duplicate() {
		executeCommand(new Duplicate());
	}

	public void insert(List<GridElement> elements) {
		if (!elements.isEmpty()) {
			executeCommand(new Duplicate(elements, true));
		}
	}

	public void setFgColor(String colorName, Collection<GridElement> elements) {
		if (!elements.isEmpty()) {
			executeCommand(new ChangeElementSettings("fg", colorName, elements));
		}
	}

	public void setBgColor(String colorName, Collection<GridElement> elements) {
		if (!elements.isEmpty()) {
			executeCommand(new ChangeElementSettings("bg", colorName, elements));
		}
	}

	public void setAttributes(String attributesText, List<GridElement> elements) {
		if (!elements.isEmpty()) {
			// do not queue a modification command if there is
			// no change in attributes
			for (GridElement element : elements) {
				if (!element.getPanelAttributes().equals(attributesText)) {
					executeCommand(new ChangeElementSettings(attributesText, elements));
					break;
				}
			}
		}
	}

}