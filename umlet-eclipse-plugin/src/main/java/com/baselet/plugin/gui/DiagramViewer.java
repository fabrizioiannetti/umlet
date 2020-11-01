package com.baselet.plugin.gui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.baselet.control.basics.geom.Point;
import com.baselet.element.Selector;
import com.baselet.element.interfaces.GridElement;
import com.baselet.element.interfaces.HasGridElements;
import com.baselet.plugin.swt.SWTComponent;
import com.baselet.plugin.swt.SWTDiagramHandler;

public class DiagramViewer extends Viewer {

	private static final Color DEFAULT_BACKGROUND = new Color(255, 255, 255);
	private final Canvas canvas;
	private SWTDiagramHandler diagram;
	private final Selector selector;

	private class DiagramElementSelector extends Selector {

		private final HasGridElements gridElementProvider;

		public DiagramElementSelector(HasGridElements gridElementProvider) {
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
			// swtOwnPropertyPane.switchToElement(e);
		}

		@Override
		public void doAfterSelectionChanged() {
			ArrayList<GridElement> elements = new ArrayList<GridElement>(getSelectedElements());
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
		canvas.addMouseListener(new MouseListener() {
			@Override
			public void mouseUp(MouseEvent e) {
				// TODO Auto-generated method stub
			}

			@Override
			public void mouseDown(MouseEvent e) {
				if ((e.stateMask & SWT.BUTTON_MASK) != SWT.BUTTON1) {
					return;
				}
				final Point position = new Point(e.x, e.y);
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
				if (selectedElement != null) {
					if (selector.isSelected(selectedElement)) {
						selector.moveToLastPosInList(selectedElement);
						// propertiesPanel.setGridElement(element, DrawPanel.this);
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

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				// TODO Auto-generated method stub
			}
		});
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void refresh() {
		canvas.redraw();
	}

	@Override
	public void setInput(Object input) {
		if (input instanceof SWTDiagramHandler) {
			diagram = (SWTDiagramHandler) input;
		}
	}

	@Override
	public void setSelection(ISelection selection, boolean reveal) {
		// TODO Auto-generated method stub

	}
}
