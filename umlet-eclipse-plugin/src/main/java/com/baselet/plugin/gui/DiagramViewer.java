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
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.baselet.command.Command;
import com.baselet.command.Controller;
import com.baselet.control.basics.geom.Point;
import com.baselet.control.enums.Direction;
import com.baselet.element.Selector;
import com.baselet.element.interfaces.GridElement;
import com.baselet.element.interfaces.HasGridElements;
import com.baselet.element.sticking.StickableMap;
import com.baselet.plugin.command.Macro;
import com.baselet.plugin.command.Move;
import com.baselet.plugin.swt.SWTComponent;
import com.baselet.plugin.swt.SWTDiagramHandler;

public class DiagramViewer extends Viewer {

	private static final Color DEFAULT_BACKGROUND = new Color(255, 255, 255);
	private final Canvas canvas;
	private SWTDiagramHandler diagram;
	private Selector selector;
	private DiagramViewer exclusiveTo;
	private final CommandInvoker controller = new CommandInvoker();
	private final int gridSize = 10;

	private final class MouseHandler implements MouseListener, MouseMoveListener, MouseWheelListener {
		private Point mouseDownAt;
		private DragMachine dragMachine;

		@Override
		public void mouseScrolled(MouseEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void mouseMove(MouseEvent e) {
			if (dragMachine == null &&
				mouseDownAt != null &&
				Math.min(Math.abs(e.x - mouseDownAt.x), Math.abs(e.x - mouseDownAt.y)) > gridSize) {
				dragMachine = new DragMachine(mouseDownAt);
			}
			if (dragMachine != null) {
				if (dragMachine.dragTo(new Point(e.x, e.y))) {
					canvas.redraw();
				}
			}
		}

		@Override
		public void mouseDoubleClick(MouseEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void mouseDown(MouseEvent e) {
			mouseDownAt = new Point(e.x, e.y);
			if (e.button == 1) {
				selectAtMouseDownPosition();
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

		private void selectAtMouseDownPosition() {
			GridElement selectedElement = getElementAtPosition(mouseDownAt);
			if (selectedElement != null) {
				if (selector.isSelected(selectedElement)) {
					selector.moveToLastPosInList(selectedElement);
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
					com.baselet.element.interfaces.Component component = gridElement.getComponent();
					SWTComponent swtComp = (SWTComponent) component;
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

	private class DragMachine {
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

		public void terminate() {
		}

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
			// TODO@fab needed?
			// if (disableElementMovement()) {
			// return;
			// }

			Vector<Command> moveCommands = new Vector<Command>();
			for (GridElement e : elementsToDrag) {
				moveCommands.add(new Move(Collections.<Direction> emptySet(), e, off.x, off.y, oldPoint, false, firstDrag, true, StickableMap.EMPTY_MAP));
			}
			controller.executeCommand(new Macro(moveCommands));
		}

		private Point snapToGrid(Point point) {
			return new Point((point.x + gridSize / 2) / gridSize * gridSize, (point.y + gridSize / 2) / gridSize * gridSize);
		}

		// only call after mouseDragged
		// TODO@fab needed?
		// protected final boolean disableElementMovement() {
		// return disableElementMovement;
		// }

	}

	private class CommandInvoker extends Controller {
		@Override
		public void executeCommand(Command command) {
			super.executeCommand(command);
		}
	}
}
