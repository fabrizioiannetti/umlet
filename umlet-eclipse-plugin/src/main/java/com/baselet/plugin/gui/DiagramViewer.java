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
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tracker;

import com.baselet.command.Command;
import com.baselet.control.basics.geom.Point;
import com.baselet.control.constants.Constants;
import com.baselet.control.enums.Direction;
import com.baselet.element.Selector;
import com.baselet.element.interfaces.GridElement;
import com.baselet.element.interfaces.HasGridElements;
import com.baselet.element.sticking.StickableMap;
import com.baselet.plugin.core.DiagramModel;
import com.baselet.plugin.core.controller.DiagramController;
import com.baselet.plugin.core.controller.Macro;
import com.baselet.plugin.core.controller.Move;
import com.baselet.plugin.swt.SWTClipBoard;
import com.baselet.plugin.swt.SWTComponent;
import com.baselet.plugin.swt.SWTConverter;
import com.baselet.plugin.swt.SWTElementFactory;

public class DiagramViewer extends Viewer implements IOperationTarget {

	private static final Color DEFAULT_BACKGROUND = new Color(Display.getDefault(), 255, 255, 255);
	static final SWTElementFactory ELEMENT_FACTORY = new SWTElementFactory();
	private final Canvas canvas;
	private DiagramModel diagram;
	private Selector selector;
	private DiagramViewer exclusiveTo;
	private DiagramController controller;
	private final int gridSize = 10;
	private int zoomPercent = 100;
	private Point origin = new Point(0, 0);
	private final Point pan = new Point(0, 0);
	private DiagramViewer editableDiagram;
	private boolean readOnly;
	private final Runnable canvasRedraw = new Runnable() {
		@Override
		public void run() {
			if (canvas != null && !canvas.isDisposed()) {
				canvas.redraw();
			}
		}
	};

	private final class InputHandler implements MouseListener, MouseMoveListener, MouseWheelListener, KeyListener {
		private Point mouseDownAt;
		private IDragMachine dragMachine;
		private Set<Direction> resizeDirections = Collections.emptySet();

		private Point getModelCoordinates(MouseEvent e) {
			return new Point(e.x * 100 / zoomPercent - origin.x, e.y * 100 / zoomPercent - origin.y);
		}

		@Override
		public void mouseScrolled(MouseEvent e) {
			boolean isUp = e.count > 0;
			int modifier = e.stateMask & SWT.MODIFIER_MASK;
			if (modifier == SWT.MOD1) {
				Integer newZoom = Integer.valueOf(zoomPercent + (isUp ? 10 : -10));
				doOperation(IOperationTarget.SET_ZOOM, null, newZoom);
			}
			else if (modifier == 0) {
				doOperation(isUp ? IOperationTarget.SCROLL_DOWN : IOperationTarget.SCROLL_UP, null, null);
			}
		}

		@Override
		public void mouseMove(MouseEvent e) {
			Point point = getModelCoordinates(e);
			if (mouseDownAt == null) {
				// plain mouse move
				GridElement onElement = getElementAtPosition(point);
				if (onElement != null) {
					resizeDirections = onElement.getResizeArea(point.x - onElement.getRectangle().x, point.y - onElement.getRectangle().y);
					canvas.setCursor(SWTConverter.cursor(onElement.getCursor(point, resizeDirections)));
				}
				else {
					resizeDirections = Collections.emptySet();
					canvas.setCursor(getControl().getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
				}
			}
			else {
				// mouse drag
				if (dragMachine == null && Math.max(Math.abs(point.x - mouseDownAt.x), Math.abs(point.y - mouseDownAt.y)) >= gridSize) {
					if ((e.stateMask & SWT.MODIFIER_MASK) == SWT.MOD1) {
						dragMachine = new LassoMachine(mouseDownAt);
					}
					else if ((e.stateMask & SWT.BUTTON_MASK) == SWT.BUTTON2) {
						dragMachine = new PanMachine(mouseDownAt);
					}
					else if (!readOnly) {
						boolean isShiftKeyDown = (e.stateMask & SWT.MODIFIER_MASK) == SWT.MOD2;
						if (resizeDirections.isEmpty()) {
							dragMachine = new DragMachine(mouseDownAt, isShiftKeyDown);
						}
						else {
							GridElement onElement = getElementAtPosition(mouseDownAt);
							selector.selectOnly(onElement);
							dragMachine = new ResizeMachine(mouseDownAt, resizeDirections, isShiftKeyDown);
						}
					}
				}
			}
			if (dragMachine != null) {
				if (dragMachine.dragTo(point)) {
					canvas.redraw();
				}
				if (dragMachine.isDone()) {
					dragMachine.terminate();
					dragMachine = null;
					mouseDownAt = null;
					EclipseGUI.setUndoRedoAvailable(controller.isUndoable(), controller.isRedoable());
					fireCurrentSelection();
				}
			}
		}

		@Override
		public void mouseDoubleClick(MouseEvent e) {
			if (!selector.getSelectedElements().isEmpty()) {
				if (editableDiagram != null) {
					editableDiagram.doOperation(INSERT, selector.getSelectedElements(), null);
				}
				else {
					doOperation(DUPLICATE, null, null);
				}
			}
			else {
				origin.x = 0;
				origin.y = 0;
				doOperation(SET_ZOOM, null, Integer.valueOf(100));
			}
		}

		@Override
		public void mouseDown(MouseEvent e) {
			Point point = getModelCoordinates(e);
			canvas.setFocus();
			if (e.button == 1) {
				mouseDownAt = point;
				selectAtMouseDownPosition((e.stateMask & SWT.MODIFIER_MASK) == SWT.MOD1);
			}
			else if (e.button == 2) {
				// only start dragging (the whole diagram), no selection
				mouseDownAt = point;
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
				EclipseGUI.setUndoRedoAvailable(controller.isUndoable(), controller.isRedoable());
				fireCurrentSelection();
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
				boolean isShiftKeyDown = (e.stateMask & SWT.MODIFIER_MASK) == SWT.MOD2;
				DragMachine dm = new DragMachine(new Point(0, 0), isShiftKeyDown);
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
			// deselect elements from associated exclusive viewer
			if (exclusiveTo != null) {
				exclusiveTo.selector.deselectAllWithoutAfterAction();
				exclusiveTo.canvas.redraw();
			}
			// note: the last selected element is at end of the list
			fireCurrentSelection();
		}
	}

	public DiagramViewer(Composite parent) {
		canvas = new Canvas(parent, SWT.NONE);
		canvas.setBackground(DEFAULT_BACKGROUND);
		diagram = new DiagramModel();
		selector = new DiagramElementSelector(diagram);
		initCanvas();
	}

	private void initCanvas() {
		canvas.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				float s = zoomPercent / 100.0f;
				float tx = s * (origin.x + pan.x);
				float ty = s * (origin.y + pan.y);
				Transform transform = new Transform(canvas.getDisplay(), s, 0, 0, s, tx, ty);
				e.gc.setTransform(transform);
				e.gc.setBackground(new Color(canvas.getDisplay(), new RGB(255, 255, 255)));
				e.gc.fillRectangle(e.x, e.y, e.width, e.height);
				List<GridElement> gridElements = diagram.getGridElements();
				for (GridElement gridElement : gridElements) {
					SWTComponent swtComp = (SWTComponent) gridElement.getComponent();
					swtComp.drawOn(e.gc, selector.isSelected(gridElement));
				}
				e.gc.setTransform(null);
				transform.dispose();
			}
		});
		InputHandler listener = new InputHandler();
		canvas.addMouseListener(listener);
		canvas.addMouseMoveListener(listener);
		canvas.addMouseWheelListener(listener);
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
		if (input instanceof DiagramModel) {
			diagram = (DiagramModel) input;
			selector = new DiagramElementSelector(diagram);
			controller = new DiagramController(diagram, selector, canvasRedraw, ELEMENT_FACTORY, SWTClipBoard.get());
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
		private final boolean isShiftKeyDown;

		public DragMachine(Point startPoint, boolean isShiftKeyDown) {
			this.isShiftKeyDown = isShiftKeyDown;
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
				if (!isShiftKeyDown) {
					StickableMap stickables = diagram.getStickables(gridElement, elementsToDrag);
					stickablesMap.put(gridElement, stickables);
				}
				else {
					stickablesMap.put(gridElement, StickableMap.EMPTY_MAP);
				}
			}
			firstDrag = true;
		}

		@Override
		public void terminate() {
			for (GridElement e : elementsToDrag) {
				e.dragEnd();
			}
		}

		@Override
		public boolean dragTo(final Point newPos) {
			Point snapPos = snapToGrid(newPos);
			Point off = new Point(snapPos.x - oldPoint.x, snapPos.y - oldPoint.y);
			if (off.x != 0 || off.y != 0) {
				controller.executeCommand(dragElements(off));
				oldPoint = snapPos;
				firstDrag = false;
				return true; // moved
			}
			return false;
		}

		private Command dragElements(Point off) {
			if (elementsToDrag.size() == 1) {
				GridElement e = elementsToDrag.get(0);
				return new Move(Collections.<Direction> emptySet(), e, off.x, off.y, oldPoint, isShiftKeyDown, firstDrag, false, stickablesMap.get(e));
			}
			else {
				Vector<Command> moveCommands = new Vector<Command>();
				for (GridElement e : elementsToDrag) {
					moveCommands.add(new Move(Collections.<Direction> emptySet(), e, off.x, off.y, oldPoint, isShiftKeyDown, firstDrag, true, stickablesMap.get(e)));
				}
				return new Macro(moveCommands);
			}
		}

		@Override
		public boolean isDone() {
			return false;
		}
	}

	private class ResizeMachine implements IDragMachine {
		private Point oldPoint;
		private final List<GridElement> elementsToResize;
		private final Map<GridElement, StickableMap> stickablesMap = new HashMap<GridElement, StickableMap>();
		boolean firstDrag;
		private final Set<Direction> resizeDirections;
		private final boolean keepProportions;

		public ResizeMachine(Point startPoint, Set<Direction> resizeDirections, boolean keepProportions) {
			this.resizeDirections = resizeDirections;
			this.keepProportions = keepProportions;
			oldPoint = snapToGrid(startPoint);
			List<GridElement> selectedElements = selector.getSelectedElements();
			elementsToResize = new ArrayList<GridElement>(selectedElements);
			for (GridElement gridElement : elementsToResize) {
				StickableMap stickables = diagram.getStickables(gridElement, elementsToResize);
				stickablesMap.put(gridElement, stickables);
			}
			firstDrag = true;
		}

		@Override
		public void terminate() {
			for (GridElement e : elementsToResize) {
				e.dragEnd();
			}
		}

		@Override
		public boolean dragTo(final Point newPos) {
			Point snapPos = snapToGrid(newPos);
			Point off = new Point(snapPos.x - oldPoint.x, snapPos.y - oldPoint.y);
			if (off.x != 0 || off.y != 0) {
				resizeElements(off);
				oldPoint = snapPos;
				firstDrag = false;
				return true; // moved
			}
			return false;
		}

		private void resizeElements(Point off) {
			Vector<Command> moveCommands = new Vector<Command>();
			for (GridElement e : elementsToResize) {
				moveCommands.add(new Move(resizeDirections, e, off.x, off.y, oldPoint, keepProportions, firstDrag, false, stickablesMap.get(e)));
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

	private class PanMachine implements IDragMachine {
		private final Point initial;

		public PanMachine(Point oldPoint) {
			initial = oldPoint.copy();
			origin.copy();
		}

		@Override
		public boolean dragTo(Point newPos) {
			pan.x = newPos.x - initial.x;
			pan.y = newPos.y - initial.y;
			return true;
		}

		@Override
		public void terminate() {
			origin.x += pan.x;
			origin.y += pan.y;
			pan.x = 0;
			pan.y = 0;
		}

		@Override
		public boolean isDone() {
			return false;
		}

	}

	public void setZoom(int zoomPercent) {
		if (zoomPercent < 10) {
			zoomPercent = 10;
		}
		else if (zoomPercent > 200) {
			zoomPercent = 200;
		}
		this.zoomPercent = zoomPercent / 10 * 10;
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
			case IOperationTarget.SELECT_ALL:
			case IOperationTarget.DUPLICATE:
			case IOperationTarget.SCROLL_UP:
			case IOperationTarget.SCROLL_DOWN:
				return true;
			case IOperationTarget.DELETE:
			case IOperationTarget.PASTE:
			case IOperationTarget.UNDO:
			case IOperationTarget.REDO:
			case IOperationTarget.INSERT:
			case IOperationTarget.SET_BG_COLOR:
			case IOperationTarget.SET_FG_COLOR:
			case IOperationTarget.SET_ATTRIBUTES:
			case IOperationTarget.SET_ZOOM:
				return !readOnly;
			default:
				break;
		}
		return false;
	}

	@Override
	public void doOperation(int operation, List<GridElement> elements, Object value) {
		if (!canDoOperation(operation)) {
			return;
		}
		switch (operation) {
			case IOperationTarget.DELETE:
				controller.deleteSelected();
				break;
			case IOperationTarget.COPY:
				controller.copy();
				break;
			case IOperationTarget.PASTE:
				controller.paste();
				break;
			case IOperationTarget.SELECT_ALL:
				selector.select(diagram.getGridElements());
				break;
			case IOperationTarget.UNDO:
				controller.undo();
				break;
			case IOperationTarget.REDO:
				controller.redo();
				break;
			case IOperationTarget.DUPLICATE:
				controller.duplicate();
				break;
			case IOperationTarget.INSERT:
				controller.insert(elements);
				break;
			case IOperationTarget.SET_FG_COLOR:
				controller.setFgColor(value.toString(), elements);
				break;
			case IOperationTarget.SET_BG_COLOR:
				controller.setBgColor(value.toString(), elements);
				break;
			case IOperationTarget.SET_ATTRIBUTES:
				controller.setAttributes(value.toString(), elements);
				break;
			case IOperationTarget.SET_ZOOM:
				setZoom(((Integer) value).intValue());
				break;
			case IOperationTarget.SCROLL_UP:
				origin = new Point(origin.x, origin.y - 2 * gridSize);
				break;
			case IOperationTarget.SCROLL_DOWN:
				origin = new Point(origin.x, origin.y + 2 * gridSize);
				break;
			default:
				break;
		}

		canvas.redraw();
		fireCurrentSelection();
		EclipseGUI.setUndoRedoAvailable(controller.isUndoable(), controller.isRedoable());
	}

	private void fireCurrentSelection() {
		fireSelectionChanged(new SelectionChangedEvent(DiagramViewer.this, new StructuredSelection(selector.getSelectedElements())));
	}

	public boolean isDiagramChanged() {
		return controller.isChanged();
	}

	public int getGridSize() {
		return gridSize;
	}

	public void setSaved() {
		controller.setChangeOrigin();
		fireCurrentSelection();
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public int getZoom() {
		return zoomPercent;
	}
}
