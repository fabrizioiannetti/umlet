package com.baselet.plugin.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
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

	private final class MouseHandler implements MouseListener, MouseMoveListener, MouseWheelListener {
		private Point mouseDownAt;
		private IDragMachine dragMachine;

		@Override
		public void mouseScrolled(MouseEvent e) {
			// TODO Auto-generated method stub
		}

		@Override
		public void mouseMove(MouseEvent e) {
			if (dragMachine == null &&
				mouseDownAt != null &&
				Math.min(Math.abs(e.x - mouseDownAt.x), Math.abs(e.x - mouseDownAt.y)) > gridSize) {
				if ((e.stateMask & SWT.MODIFIER_MASK) == SWT.MOD1) {
					dragMachine = new LassoMachine(mouseDownAt);
				}
				else {
					dragMachine = new DragMachine(mouseDownAt);
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
			// TODO Auto-generated method stub

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

	private interface IDragMachine {

		boolean dragTo(final Point newPos);

		void terminate();

		boolean isDone();

	}

	private class DragMachine implements IDragMachine {
		private Point oldPoint;
		private List<GridElement> elementsToDrag;
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
				moveCommands.add(new Move(Collections.<Direction> emptySet(), e, off.x, off.y, oldPoint, false, firstDrag, true, StickableMap.EMPTY_MAP));
			}
			controller.executeCommand(new Macro(moveCommands));
		}

		private Point snapToGrid(Point point) {
			return new Point((point.x + gridSize / 2) / gridSize * gridSize, (point.y + gridSize / 2) / gridSize * gridSize);
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
			for (GridElement gridElement : elements) {
				diagram.getGridElements().add(ELEMENT_FACTORY.create(gridElement, diagram));
			}
		}

		@Override
		public void undo() {
			// TODO Auto-generated method stub
		}
	}

	public class Delete extends Command {
		@Override
		public void execute() {
			// TODO Auto-generated method stub
		}

		@Override
		public void undo() {
			// TODO Auto-generated method stub
		}
	}
}
