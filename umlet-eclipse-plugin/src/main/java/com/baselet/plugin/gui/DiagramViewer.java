package com.baselet.plugin.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tracker;

import com.baselet.command.Command;
import com.baselet.command.Controller;
import com.baselet.control.basics.geom.Point;
import com.baselet.control.constants.Constants;
import com.baselet.control.enums.Direction;
import com.baselet.element.Selector;
import com.baselet.element.interfaces.GridElement;
import com.baselet.element.interfaces.HasGridElements;
import com.baselet.element.sticking.StickableMap;
import com.baselet.plugin.command.Macro;
import com.baselet.plugin.command.Move;
import com.baselet.plugin.swt.SWTClipBoard;
import com.baselet.plugin.swt.SWTComponent;
import com.baselet.plugin.swt.SWTConverter;
import com.baselet.plugin.swt.SWTDiagramHandler;
import com.baselet.plugin.swt.SWTElementFactory;

public class DiagramViewer extends Viewer implements IOperationTarget {

	private static final Color DEFAULT_BACKGROUND = new Color(255, 255, 255);
	private static final SWTElementFactory ELEMENT_FACTORY = new SWTElementFactory();
	private final Canvas canvas;
	private SWTDiagramHandler diagram;
	private Selector selector;
	private DiagramViewer exclusiveTo;
	private final CommandInvoker controller = new CommandInvoker();
	private final int gridSize = 10;
	private DiagramViewer editableDiagram;

	private final class MouseHandler implements MouseListener, MouseMoveListener, MouseWheelListener, KeyListener {
		private Point mouseDownAt;
		private IDragMachine dragMachine;
		private Set<Direction> resizeDirections = Collections.emptySet();

		@Override
		public void mouseScrolled(MouseEvent e) {
			// TODO Auto-generated method stub
		}

		@Override
		public void mouseMove(MouseEvent e) {
			if (mouseDownAt == null) {
				// plain mouse move
				Point point = new Point(e.x, e.y);
				GridElement onElement = getElementAtPosition(point);
				if (onElement != null) {
					resizeDirections = onElement.getResizeArea(e.x - onElement.getRectangle().x, e.y - onElement.getRectangle().y);
					canvas.setCursor(SWTConverter.cursor(onElement.getCursor(point, resizeDirections)));
				}
				else {
					resizeDirections = Collections.emptySet();
					canvas.setCursor(getControl().getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
				}
			}
			else {
				// mouse drag
				if (dragMachine == null && Math.max(Math.abs(e.x - mouseDownAt.x), Math.abs(e.x - mouseDownAt.y)) >= gridSize) {
					if ((e.stateMask & SWT.MODIFIER_MASK) == SWT.MOD1) {
						dragMachine = new LassoMachine(mouseDownAt);
					}
					else if (resizeDirections.isEmpty()) {
						dragMachine = new DragMachine(mouseDownAt);
					}
					else {
						GridElement onElement = getElementAtPosition(mouseDownAt);
						selector.selectOnly(onElement);
						dragMachine = new ResizeMachine(mouseDownAt, resizeDirections, (e.stateMask & SWT.MODIFIER_MASK) == SWT.MOD2);
					}
				}
			}
			if (dragMachine != null) {
				if (dragMachine.dragTo(new Point(e.x, e.y))) {
					canvas.redraw();
				}
				if (dragMachine.isDone()) {
					dragMachine.terminate();
					dragMachine = null;
					mouseDownAt = null;
				}
			}
		}

		@Override
		public void mouseDoubleClick(MouseEvent e) {
			if (!selector.getSelectedElements().isEmpty()) {
				if (editableDiagram != null) {
					editableDiagram.addElements(selector.getSelectedElements());
				}
				else {
					controller.executeCommand(new Duplicate());
				}
			}
		}

		@Override
		public void mouseDown(MouseEvent e) {
			Point point = new Point(e.x, e.y);
			canvas.setFocus();
			if (e.button == 1) {
				mouseDownAt = point;
				selectAtMouseDownPosition((e.stateMask & SWT.MODIFIER_MASK) == SWT.MOD1);
			}
			else if (e.button == 3) {
				GridElement selectedElement = getElementAtPosition(point);
				if (selectedElement != null && !selector.isSelected(selectedElement)) {
					selector.selectOnly(selectedElement);
					canvas.redraw();
				}
			}
		}

		@Override
		public void mouseUp(MouseEvent e) {
			if (dragMachine != null) {
				dragMachine.terminate();
				dragMachine = null;
			}
			mouseDownAt = null;
		}

		private void selectAtMouseDownPosition(boolean addToSelection) {
			GridElement selectedElement = getElementAtPosition(mouseDownAt);
			if (selectedElement != null) {
				if (selector.isSelected(selectedElement)) {
					if (addToSelection) {
						selector.deselect(selectedElement);
					}
					else {
						selector.moveToLastPosInList(selectedElement);
					}
				}
				else if (addToSelection) {
					selector.select(selectedElement);
				}
				else {
					selector.selectOnly(selectedElement);
				}
			}
			else {
				selector.deselectAll();
			}
			canvas.redraw();
		}

		private GridElement getElementAtPosition(final Point position) {
			GridElement selectedElement = null;
			for (GridElement ge : diagram.getGridElementsByLayer(false)) { // get elements, highest layer first
				if (selectedElement != null && selectedElement.getLayer() > ge.getLayer()) {
					break; // because the following elements have lower layers, break if a valid higher layered element has been found
				}
				if (ge.isSelectableOn(position)) {
					if (selectedElement == null) {
						selectedElement = ge;
					}
					else {
						boolean newIsSelectedOldNot = selector.isSelected(ge) && !selector.isSelected(selectedElement);
						boolean oldContainsNew = selectedElement.getRectangle().contains(ge.getRectangle());
						if (newIsSelectedOldNot || oldContainsNew) {
							selectedElement = ge;
						}
					}
				}
			}
			return selectedElement;
		}

		@Override
		public void keyPressed(KeyEvent e) {
			int xOffset = 0;
			int yOffset = 0;
			switch (e.keyCode) {
				case SWT.ARROW_DOWN:
					yOffset = Constants.DEFAULTGRIDSIZE;
					break;
				case SWT.ARROW_UP:
					yOffset = -Constants.DEFAULTGRIDSIZE;
					break;
				case SWT.ARROW_LEFT:
					xOffset = -Constants.DEFAULTGRIDSIZE;
					break;
				case SWT.ARROW_RIGHT:
					xOffset = Constants.DEFAULTGRIDSIZE;
					break;
			}
			if (xOffset != 0 || yOffset != 0) {
				DragMachine dm = new DragMachine(new Point(0, 0));
				dm.dragTo(new Point(xOffset, yOffset));
				dm.terminate();
				canvas.redraw();
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
			// TODO Auto-generated method stub

		}
	}

	private class DiagramElementSelector extends Selector {
		private final HasGridElements gridElementProvider;
		private final List<GridElement> selectedElements = new ArrayList<GridElement>();

		public DiagramElementSelector(HasGridElements gridElementProvider) {
			this.gridElementProvider = gridElementProvider;
		}

		@Override
		public List<GridElement> getSelectedElements() {
			return selectedElements;
		}

		@Override
		public List<GridElement> getAllElements() {
			return gridElementProvider.getGridElements();
		}

		@Override
		public void doAfterSelectionChanged() {
			ArrayList<GridElement> elements = new ArrayList<GridElement>(getSelectedElements());
			// deselect elements from associated exclusive viewer
			if (exclusiveTo != null) {
				exclusiveTo.selector.deselectAllWithoutAfterAction();
				exclusiveTo.canvas.redraw();
			}
			// note: the last selected element is at end of the list
			fireSelectionChanged(new SelectionChangedEvent(DiagramViewer.this, new StructuredSelection(elements)));
		}
	}

	public DiagramViewer(Composite parent) {
		canvas = new Canvas(parent, SWT.NONE);
		canvas.setBackground(DEFAULT_BACKGROUND);
		diagram = new SWTDiagramHandler();
		selector = new DiagramElementSelector(diagram);
		initCanvas();
	}

	private void initCanvas() {
		canvas.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				e.gc.setBackground(new Color(new RGB(255, 255, 255)));
				e.gc.fillRectangle(e.x, e.y, e.width, e.height);
				List<GridElement> gridElements = diagram.getGridElements();
				for (GridElement gridElement : gridElements) {
					SWTComponent swtComp = (SWTComponent) gridElement.getComponent();
					swtComp.drawOn(e.gc, selector.isSelected(gridElement), 1d);
				}
			}
		});
		MouseHandler listener = new MouseHandler();
		canvas.addMouseListener(listener);
		canvas.addMouseMoveListener(listener);
		canvas.addKeyListener(listener);
	}

	@Override
	public Control getControl() {
		return canvas;
	}

	@Override
	public Object getInput() {
		return diagram;
	}

	@Override
	public ISelection getSelection() {
		return new StructuredSelection(selector.getSelectedElements());
	}

	@Override
	public void refresh() {
		canvas.redraw();
	}

	@Override
	public void setInput(Object input) {
		if (input instanceof SWTDiagramHandler) {
			diagram = (SWTDiagramHandler) input;
			selector = new DiagramElementSelector(diagram);
			controller.clear();
		}
	}

	@Override
	public void setSelection(ISelection selection, boolean reveal) {
		ArrayList<GridElement> selectedElements = new ArrayList<GridElement>();
		IStructuredSelection ss = (IStructuredSelection) selection;
		Object[] selected = ss.toArray();
		for (Object object : selected) {
			if (object instanceof GridElement) {
				GridElement ge = (GridElement) object;
				selectedElements.add(ge);
			}
		}
		List<GridElement> previouslySelected = selector.getSelectedElements();
		if (!selectedElements.equals(previouslySelected)) {
			selector.selectOnly(selectedElements);
			canvas.redraw();
		}
	}

	public void setAttributesForSelected(String newAttributes) {
		List<GridElement> selectedElements = selector.getSelectedElements();
		if (!selectedElements.isEmpty()) {
			GridElement gridElement = selectedElements.get(selectedElements.size() - 1);
			String currentAttributes = gridElement.getPanelAttributes();
			if (!newAttributes.equals(currentAttributes)) {
				gridElement.setPanelAttributes(newAttributes);
				canvas.redraw();
			}
		}
	}

	public void setExclusiveTo(DiagramViewer other) {
		exclusiveTo = other;
	}

	public void setPaletteFor(DiagramViewer editableDiagram) {
		this.editableDiagram = editableDiagram;
	}

	private interface IDragMachine {

		boolean dragTo(final Point newPos);

		void terminate();

		boolean isDone();

	}

	private class DragMachine implements IDragMachine {
		private Point oldPoint;
		private List<GridElement> elementsToDrag;
		private final Map<GridElement, StickableMap> stickablesMap = new HashMap<GridElement, StickableMap>();
		boolean firstDrag;

		public DragMachine(Point startPoint) {
			oldPoint = snapToGrid(startPoint);
			List<GridElement> selectedElements = selector.getSelectedElements();
			if (selectedElements.isEmpty()) {
				// drag diagram : move all elements
				elementsToDrag = new ArrayList<GridElement>(selector.getAllElements());
			}
			else {
				elementsToDrag = new ArrayList<GridElement>(selectedElements);
			}
			for (GridElement gridElement : elementsToDrag) {
				StickableMap stickables = diagram.getStickables(gridElement, elementsToDrag);
				stickablesMap.put(gridElement, stickables);
			}
			firstDrag = true;
		}

		@Override
		public void terminate() {
		}

		@Override
		public boolean dragTo(final Point newPos) {
			Point snapPos = snapToGrid(newPos);
			Point off = new Point(snapPos.x - oldPoint.x, snapPos.y - oldPoint.y);
			if (off.x != 0 || off.y != 0) {
				dragElements(off);
				oldPoint = snapPos;
				firstDrag = false;
				return true; // moved
			}
			return false;
		}

		private void dragElements(Point off) {
			Vector<Command> moveCommands = new Vector<Command>();
			for (GridElement e : elementsToDrag) {
				moveCommands.add(new Move(Collections.<Direction> emptySet(), e, off.x, off.y, oldPoint, false, firstDrag, true, stickablesMap.get(e)));
			}
			controller.executeCommand(new Macro(moveCommands));
		}

		@Override
		public boolean isDone() {
			return false;
		}
	}

	private class ResizeMachine implements IDragMachine {
		private Point oldPoint;
		private final List<GridElement> elementsToResize;
		boolean firstDrag;
		private final Set<Direction> resizeDirections;
		private final boolean keepProportions;

		public ResizeMachine(Point startPoint, Set<Direction> resizeDirections, boolean keepProportions) {
			this.resizeDirections = resizeDirections;
			this.keepProportions = keepProportions;
			oldPoint = snapToGrid(startPoint);
			List<GridElement> selectedElements = selector.getSelectedElements();
			elementsToResize = new ArrayList<GridElement>(selectedElements);
			firstDrag = true;
		}

		@Override
		public void terminate() {
		}

		@Override
		public boolean dragTo(final Point newPos) {
			Point snapPos = snapToGrid(newPos);
			Point off = new Point(snapPos.x - oldPoint.x, snapPos.y - oldPoint.y);
			if (off.x != 0 || off.y != 0) {
				dragElements(off);
				oldPoint = snapPos;
				firstDrag = false;
				return true; // moved
			}
			return false;
		}

		private void dragElements(Point off) {
			Vector<Command> moveCommands = new Vector<Command>();
			for (GridElement e : elementsToResize) {
				moveCommands.add(new Move(resizeDirections, e, off.x, off.y, oldPoint, keepProportions, firstDrag, false, StickableMap.EMPTY_MAP));
			}
			controller.executeCommand(new Macro(moveCommands));
		}

		@Override
		public boolean isDone() {
			return false;
		}
	}

	private class LassoMachine implements IDragMachine, ControlListener {

		private final Point initialPos;
		private final Tracker tracker;
		private final ArrayList<GridElement> initiallySelected;
		private boolean done;

		public LassoMachine(Point mouseDownAt) {
			initialPos = mouseDownAt;
			tracker = new Tracker(canvas, SWT.RESIZE);
			initiallySelected = new ArrayList<GridElement>(selector.getSelectedElements());
			done = false;
		}

		@Override
		public boolean dragTo(Point newPos) {
			tracker.setRectangles(new Rectangle[] { new Rectangle(initialPos.x, initialPos.y, Math.abs(newPos.x - initialPos.x), Math.abs(newPos.y - initialPos.y)) });
			tracker.addControlListener(this);
			if (!tracker.open()) {
				// tracker cancelled, revert selection
				selector.selectOnly(initiallySelected);
			}
			done = true;
			return true;
		}

		@Override
		public void terminate() {
			tracker.dispose();
		}

		@Override
		public boolean isDone() {
			return done;
		}

		@Override
		public void controlMoved(ControlEvent e) {
			// nothing to do: only resize
		}

		@Override
		public void controlResized(ControlEvent e) {
			Rectangle r = tracker.getRectangles()[0];
			com.baselet.control.basics.geom.Rectangle rectangle = new com.baselet.control.basics.geom.Rectangle(r.x, r.y, r.width, r.height);
			List<GridElement> toSelect = new ArrayList<GridElement>();
			for (GridElement element : selector.getAllElements()) {
				if (element.isInRange(rectangle)) {
					toSelect.add(element);
				}
			}
			selector.selectOnly(toSelect);
			canvas.redraw();
		}

	}

	private class CommandInvoker extends Controller {
		@Override
		public void executeCommand(Command command) {
			super.executeCommand(command);
		}
	}

	private Point snapToGrid(Point point) {
		int halfX = point.x > 0 ? gridSize / 2 : -gridSize / 2;
		int halfY = point.y > 0 ? gridSize / 2 : -gridSize / 2;
		return new Point((point.x + halfX) / gridSize * gridSize, (point.y + halfY) / gridSize * gridSize);
	}

	@Override
	public boolean canDoOperation(int operation) {
		switch (operation) {
			case IOperationTarget.COPY:
			case IOperationTarget.DELETE:
			case IOperationTarget.PASTE:
			case IOperationTarget.SELECT_ALL:
				return true;
			default:
				break;
		}
		return false;
	}

	@Override
	public void doOperation(int operation) {
		switch (operation) {
			case IOperationTarget.DELETE:
				controller.executeCommand(new Delete());
				break;
			case IOperationTarget.COPY:
				SWTClipBoard.copyElements(selector.getSelectedElements());
				break;
			case IOperationTarget.PASTE:
				controller.executeCommand(new Paste());
				break;
			case IOperationTarget.SELECT_ALL:
				selector.select(diagram.getGridElements());
				break;
			default:
				break;
		}
		canvas.redraw();
	}

	private void addElements(List<GridElement> sourceElements) {
		controller.executeCommand(new Duplicate(sourceElements, true));
	}

	public class Paste extends Command {
		private final List<GridElement> elements = new ArrayList<GridElement>();

		public Paste() {
			SWTClipBoard.pasteElements(elements, diagram);
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
			List<GridElement> newElements = new ArrayList<GridElement>();
			for (GridElement gridElement : elements) {
				newElements.add(ELEMENT_FACTORY.create(gridElement, diagram));
			}
			diagram.getGridElements().addAll(newElements);
			selector.selectOnly(newElements);
			canvas.redraw();
		}

		@Override
		public void undo() {
			// TODO Auto-generated method stub
		}
	}

	public class Delete extends Command {
		@Override
		public void execute() {
			List<GridElement> selectedElements = selector.getSelectedElements();
			if (!selectedElements.isEmpty()) {
				diagram.getGridElements().removeAll(selectedElements);
				canvas.redraw();
			}
		}

		@Override
		public void undo() {
			// TODO Auto-generated method stub
		}
	}

	public class Duplicate extends Command {
		private final List<GridElement> elements = new ArrayList<GridElement>();

		public Duplicate() {
			this(selector.getSelectedElements(), false);
		}

		public Duplicate(List<GridElement> sourceElements, boolean moveToOrigin) {
			for (GridElement element : sourceElements) {
				elements.add(ELEMENT_FACTORY.create(element, diagram));
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
			List<GridElement> newElements = new ArrayList<GridElement>();
			for (GridElement gridElement : elements) {
				newElements.add(ELEMENT_FACTORY.create(gridElement, diagram));
			}
			diagram.getGridElements().addAll(newElements);
			selector.selectOnly(newElements);
			canvas.redraw();
		}

		@Override
		public void undo() {
			// TODO Auto-generated method stub
		}
	}
}
