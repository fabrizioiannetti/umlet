package com.baselet.plugin.swt;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baselet.control.basics.geom.Rectangle;
import com.baselet.control.config.Config;
import com.baselet.control.config.SharedConfig;
import com.baselet.control.constants.Constants;
import com.baselet.control.util.Utils;
import com.baselet.diagram.DiagramHandler;
import com.baselet.diagram.DiagramNotification;
import com.baselet.diagram.DrawPanel;
import com.baselet.diagram.PaletteHandler;
import com.baselet.element.interfaces.GridElement;
import com.baselet.element.old.element.Relation;

public class SWTDrawPanel {

	private static final Logger log = LoggerFactory.getLogger(DrawPanel.class);

	private final Point origin;
	// private final SelectorOld selector;
	private final DiagramHandler handler;
	private final Canvas paintCanvas;
	private final Color enabledColor = new Color(255, 255, 255);
	private final Color disabledColor = new Color(235, 235, 235);

	private final List<GridElement> gridElements = new ArrayList<GridElement>();
	private final List<SWTComponent> components = new ArrayList<SWTComponent>();

	/**
	 * @param initStartupTextAndFiledrop
	 */
	public SWTDrawPanel(Composite parent, int style, DiagramHandler handler, boolean initStartupTextAndFiledrop) {
		paintCanvas = new Canvas(parent, style);
		this.handler = handler;
		// AB: Origin is used to track diagram movement in Cut Command
		origin = new Point(0, 0);
		paintCanvas.setBackground(new Color(0xff, 0xff, 0xff));
		// selector = new SelectorOld(this);

		// if (initStartupTextAndFiledrop) {
		// StartUpHelpText startupHelpText = new StartUpHelpText(this);
		// add(startupHelpText);
		// @SuppressWarnings("unused")
		// FileDrop fd = new FileDrop(startupHelpText, new FileDropListener()); // only init if this is not a BATCH call. Also fixes Issue 81
		// }
	}

	public void dispose() {
		enabledColor.dispose();
		disabledColor.dispose();
	}

	public void setEnabled(boolean en) {
		handler.setEnabled(en);
		paintCanvas.setEnabled(en);
		paintCanvas.setBackground(en ? enabledColor : disabledColor);
	}

	public DiagramHandler getHandler() {
		return handler;
	}

	/**
	 * Returns the smalles possible rectangle which contains all entities and a border space around it
	 *
	 * @param borderSpace
	 *            the borderspace around the rectangle
	 * @param entities
	 *            the entities which should be included
	 * @return Rectangle which contains all entities with border space
	 */
	public static Rectangle getContentBounds(int borderSpace, Collection<GridElement> entities) {
		if (entities.size() == 0) {
			return new Rectangle(0, 0, 0, 0);
		}

		int minx = Integer.MAX_VALUE;
		int miny = Integer.MAX_VALUE;
		int maxx = 0;
		int maxy = 0;

		for (GridElement e : entities) {
			minx = Math.min(minx, e.getRectangle().x - borderSpace);
			miny = Math.min(miny, e.getRectangle().y - borderSpace);
			maxx = Math.max(maxx, e.getRectangle().x + e.getRectangle().width + borderSpace);
			maxy = Math.max(maxy, e.getRectangle().y + e.getRectangle().height + borderSpace);
		}
		return new Rectangle(minx, miny, maxx - minx, maxy - miny);
	}

	// @Override
	// public int print(Graphics g, PageFormat pageFormat, int pageIndex) {
	// if (pageIndex > 0) {
	// return NO_SUCH_PAGE;
	// }
	// else {
	// Graphics2D g2d = (Graphics2D) g;
	// RepaintManager currentManager = RepaintManager.currentManager(this);
	// currentManager.setDoubleBufferingEnabled(false);
	// Rectangle bounds = getContentBounds(Config.getInstance().getPrintPadding(), getGridElements());
	// g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
	// AffineTransform t = g2d.getTransform();
	// double scale = Math.min(pageFormat.getImageableWidth() / bounds.width,
	// pageFormat.getImageableHeight() / bounds.height);
	// if (scale < 1) {
	// t.scale(scale, scale);
	// g2d.setTransform(t);
	// }
	// g2d.translate(-bounds.x, -bounds.y);
	// paint(g2d);
	// currentManager = RepaintManager.currentManager(this);
	// currentManager.setDoubleBufferingEnabled(true);
	// return PAGE_EXISTS;
	// }
	// }

	public List<GridElement> getGridElements() {
		return gridElements;
	}

	public List<Relation> getOldRelations() {
		return getHelper(Relation.class);
	}

	public List<com.baselet.element.relation.Relation> getStickables(Collection<GridElement> excludeList) {
		if (!SharedConfig.getInstance().isStickingEnabled() || handler instanceof PaletteHandler) {
			return Collections.<com.baselet.element.relation.Relation> emptyList();
		}
		List<com.baselet.element.relation.Relation> returnList = getHelper(com.baselet.element.relation.Relation.class);
		returnList.removeAll(excludeList);
		return returnList;
	}

	@SuppressWarnings("unchecked")
	private <T extends GridElement> List<T> getHelper(Class<T> filtered) {
		List<T> gridElementsToReturn = new ArrayList<T>();
		for (GridElement e : getGridElements()) {
			if (e.getClass().equals(filtered)) {
				gridElementsToReturn.add((T) e);
			}
		}
		return gridElementsToReturn;
	}

	// public SelectorOld getSelector() {
	// return selector;
	// }

	/**
	 * This method must be called after every "significant change" on the drawpanel.
	 * This doesn't include every increment of dragging an grid element but it should be called after
	 * the grid elements new location is set (= after the mousebutton is released)
	 * It should be called only once after many grid elements have changed and not for each element!
	 * This makes it very hard to call this method by using listeners, therefore it's called explicitly in specific cases.
	 */
	public void updatePanelAndScrollbars() {
		insertUpperLeftWhitespaceIfNeeded();
		// removeUnnecessaryWhitespaceAroundDiagram();
	}

	/**
	 * If entities are out of the visible drawpanel border on the upper left
	 * corner this method enlarges the drawpanel and displays scrollbars
	 */
	private void insertUpperLeftWhitespaceIfNeeded() {

		Rectangle diaWithoutWhite = getContentBounds(0, getGridElements());
		// We must adjust the components and the view by a certain factor
		int adjustWidth = 0;
		if (diaWithoutWhite.getX() < 0) {
			adjustWidth = diaWithoutWhite.getX();
		}

		int adjustHeight = 0;
		if (diaWithoutWhite.getY() < 0) {
			adjustHeight = diaWithoutWhite.getY();
		}

		moveOrigin(adjustWidth, adjustHeight);

		// If any adjustment is needed we move the components and increase the view position
		if (adjustWidth != 0 || adjustHeight != 0) {
			for (SWTComponent c : components) {
				c.setLocation(handler.realignToGrid(false, c.getX() - adjustWidth), handler.realignToGrid(false, c.getY() - adjustHeight));
			}
		}
	}

	/**
	 * Changes the viewposition of the drawpanel and recalculates the optimal drawpanelsize
	 */
	public void changeViewPosition(int incx, int incy) {
		// Point viewp = _scr.getViewport().getViewPosition();
		// _scr.getViewport().setViewSize(getPreferredSize());
		// _scr.getViewport().setViewPosition(new Point(viewp.x + incx, viewp.y + incy));
	}

	/**
	 * If there is a scrollbar visible and a unnecessary whitespace on any border of the diagram
	 * which is not visible (but possibly scrollable by scrollbars) we remove this whitespace
	 */
	// private void removeUnnecessaryWhitespaceAroundDiagram() {
	//
	// Rectangle diaWithoutWhite = getContentBounds(0, getGridElements());
	// Dimension viewSize = getViewableDiagrampanelSize();
	// int horSbPos = _scr.getHorizontalScrollBar().getValue();
	// int verSbPos = _scr.getVerticalScrollBar().getValue();
	//
	// horSbPos = handler.realignToGrid(false, horSbPos);
	// verSbPos = handler.realignToGrid(false, verSbPos);
	//
	// int newX = 0;
	// if (_scr.getHorizontalScrollBar().isShowing()) {
	// if (horSbPos > diaWithoutWhite.getX()) {
	// newX = diaWithoutWhite.getX();
	// }
	// else {
	// newX = horSbPos;
	// }
	// }
	//
	// int newY = 0;
	// if (_scr.getVerticalScrollBar().isShowing()) {
	// if (verSbPos > diaWithoutWhite.getY()) {
	// newY = diaWithoutWhite.getY();
	// }
	// else {
	// newY = verSbPos;
	// }
	// }
	//
	// int newWidth = (int) (horSbPos + viewSize.getWidth());
	// // If the diagram exceeds the right viewable border the width must be adjusted
	// if (diaWithoutWhite.getX() + diaWithoutWhite.getWidth() > horSbPos + viewSize.getWidth()) {
	// newWidth = diaWithoutWhite.getX() + diaWithoutWhite.getWidth();
	// }
	//
	// int newHeight = (int) (verSbPos + viewSize.getHeight());
	// // If the diagram exceeds the lower viewable border the width must be adjusted
	// if (diaWithoutWhite.getY() + diaWithoutWhite.getHeight() > verSbPos + viewSize.getHeight()) {
	// newHeight = diaWithoutWhite.getY() + diaWithoutWhite.getHeight();
	// }
	//
	// moveOrigin(newX, newY);
	//
	// for (GridElement ge : getGridElements()) {
	// ge.setLocation(handler.realignToGrid(false, ge.getRectangle().x - newX), handler.realignToGrid(false, ge.getRectangle().y - newY));
	// }
	//
	// changeViewPosition(-newX, -newY);
	// setPreferredSize(new Dimension(newWidth - newX, newHeight - newY));
	//
	// checkIfScrollbarsAreNecessary();
	// }

	/**
	 * Returns the visible size of the drawpanel
	 */
	public Point getViewableDiagrampanelSize() {
		org.eclipse.swt.graphics.Rectangle clientArea = paintCanvas.getClientArea();
		return new Point(clientArea.width, clientArea.height);
	}

	private void drawGrid(Graphics2D g2d) {
		g2d.setColor(Constants.GRID_COLOR);

		int gridSize = handler.getGridSize();
		if (gridSize == 1) {
			return; // Gridsize 1 would only make the whole screen grey
		}

		int width = 2000 + getPreferredSize().x;
		int height = 1000 + getPreferredSize().y;
		for (int i = gridSize; i < width; i += gridSize) {
			g2d.drawLine(i, 0, i, height);
		}
		for (int i = gridSize; i < height; i += gridSize) {
			g2d.drawLine(0, i, width, i);
		}
	}

	private Point getPreferredSize() {
		return new Point(0, 0); // TODO@fab
	}

	protected void paintChildren(Graphics g) {
		// check if layers have changed and update them
		for (GridElement ge : gridElements) {
			if (!ge.getLayer().equals(getLayer(ge.getComponent()))) {
				setLayer(ge.getComponent(), ge.getLayer());
			}
		}

		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHints(Utils.getUxRenderingQualityHigh(true));
		if (Config.getInstance().isShow_grid()) {
			drawGrid(g2d);
		}
	}

	private void setLayer(com.baselet.element.interfaces.Component component, Integer layer) {
		// TODO Auto-generated method stub

	}

	private Object getLayer(com.baselet.element.interfaces.Component component) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * AB: Returns a copy of the actual origin zoomed to 100%.
	 * Origin marks a point that tracks changes of the diagram panel size and can
	 * be used to regenerate the original position of entities at the time they have been cut/copied,
	 * etc...
	 *
	 * @return a point that marks the diagram origin at a zoom of 100%.
	 */

	public Point getOriginAtDefaultZoom() {
		Point originCopy = new Point(
				origin.x * Constants.DEFAULTGRIDSIZE / handler.getGridSize(),
				origin.y * Constants.DEFAULTGRIDSIZE / handler.getGridSize());
		return originCopy;
	}

	/**
	 * AB: Returns a copy of the actual origin.
	 * Origin marks a point that tracks changes of the diagram panel size and can
	 * be used to regenerate the original position of entities at the time they have been cut/copied,
	 * etc...
	 *
	 * @return a point that marks the diagram origin.
	 */
	public Point getOrigin() {
		log.trace("Diagram origin: " + origin);
		return new Point(origin.x, origin.y);
	}

	/**
	 * AB: Moves the origin around the given delta x and delta y
	 * This method is mainly used by updatePanelAndScrollBars() to keep track of the panels size changes.
	 */
	public void moveOrigin(int dx, int dy) {
		log.trace("Move origin to: " + origin);
		origin.x += handler.realignToGrid(false, dx);
		origin.y += handler.realignToGrid(false, dy);
	}

	/**
	 * AB: Zoom the origin from the old grid size to the new grid size
	 * this method is mainly used by the DiagramHandler classes setGridAndZoom method.
	 *
	 * @see DiagramHandler#setGridAndZoom(int)
	 * @param oldGridSize
	 *            the old grid size
	 * @param newGridSize
	 *            the new grid size
	 */
	public void zoomOrigin(int oldGridSize, int newGridSize) {
		log.trace("Zoom origin to: " + origin);
		origin.x = origin.x * newGridSize / oldGridSize;
		origin.y = origin.y * newGridSize / oldGridSize;
	}

	public void removeElement(GridElement gridElement) {
		gridElements.remove(gridElement);
		remove(gridElement.getComponent());
	}

	private void remove(com.baselet.element.interfaces.Component component) {
		// TODO@fab Auto-generated method stub
	}

	public void addElement(GridElement gridElement) {
		gridElements.add(gridElement);
		add((Component) gridElement.getComponent(), gridElement.getLayer());
	}

	private void add(Component component, Integer layer) {
		// TODO@fab Auto-generated method stub
	}

	public void updateElements() {
		for (GridElement e : gridElements) {
			e.updateModelFromText();
		}
	}

	public GridElement getElementToComponent(Component component) {
		for (GridElement e : gridElements) {
			if (e.getComponent().equals(component)) {
				return e;
			}
		}
		return null;
	}

	private DiagramNotification notification;

	public void setNotification(DiagramNotification newNotification) {
		// TODO@fab Auto-generated method stub
		// if (notification != null) {
		// remove(notification);
		// }
		//
		// notification = newNotification;
		// add(notification);
		//
		// repaint();
	}

}
