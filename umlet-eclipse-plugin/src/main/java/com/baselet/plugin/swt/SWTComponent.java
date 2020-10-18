package com.baselet.plugin.swt;

import java.awt.Graphics;

import com.baselet.control.HandlerElementMap;
import com.baselet.control.basics.geom.Point;
import com.baselet.control.basics.geom.Rectangle;
import com.baselet.diagram.DiagramHandler;
import com.baselet.diagram.draw.DrawHandler;
import com.baselet.diagram.draw.swing.DrawHandlerSwing;
import com.baselet.element.ElementUtils;
import com.baselet.element.NewGridElement;
import com.baselet.element.interfaces.Component;

public class SWTComponent implements Component {

	private final DrawHandlerSwing drawer;
	private final DrawHandlerSwing metaDrawer;
	private final NewGridElement gridElement;
	private int x = 0;
	private int y = 0;

	public SWTComponent(NewGridElement gridElement) {
		this.gridElement = gridElement;
		drawer = new DrawHandlerSwing(gridElement);
		metaDrawer = new DrawHandlerSwing(gridElement);
	}

	public void paint(Graphics g) {
		drawer.setGraphics(g);
		metaDrawer.setGraphics(g);
		boolean selected = HandlerElementMap.getHandlerForElement(gridElement).getDrawPanel().getSelector().isSelected(gridElement);
		drawer.drawAll(selected);
		if (selected) {
			metaDrawer.drawAll();
		}
	}

	@Override
	public void translateForExport() { // translation breaks export of some elements, therefore its disabled - see issue 353
		// drawer.setTranslate(true);
		// metaDrawer.setTranslate(true);
	}

	@Override
	public DrawHandler getDrawHandler() {
		return drawer;
	}

	@Override
	public DrawHandler getMetaDrawHandler() {
		return metaDrawer;
	}

	/**
	 * Must be overwritten because Swing sometimes uses this method instead of contains(Point)
	 */
	public boolean contains(int x, int y) {
		Rectangle r = gridElement.getRectangle();
		// only check if element selectable on the position, because some elements are not everywhere selectable (eg: Relation)
		if (gridElement.isSelectableOn(new Point(r.getX() + x, r.getY() + y))) {
			return ElementUtils.checkForOverlap(gridElement, new Point(x, y));
		}
		else {
			return false;
		}
	}

	@Override
	public Rectangle getBoundsRect() {
		return gridElement.getRectangle();
		// return Converter.convert(getBounds());
	}

	@Override
	public void repaintComponent() {
		// this.repaint();
	}

	@Override
	public void setBoundsRect(Rectangle rect) {
		// this.setBounds(rect.x, rect.y, rect.width, rect.height);
	}

	@Override
	public void afterModelUpdate() {
		// repaint(); // necessary e.g. for NewGridElement Relation to make sure it gets redrawn correctly when a sticking element is moved around
	}

	public void setHandler(DiagramHandler diagramHandler) {
		drawer.setHandler(diagramHandler);
		metaDrawer.setHandler(diagramHandler);
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public void setLocation(int x, int y) {
		this.x = x;
		this.y = y;
	}

}
