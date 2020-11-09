package com.baselet.plugin.export;

import java.awt.Graphics;

import com.baselet.control.basics.geom.Rectangle;
import com.baselet.diagram.DiagramHandler;
import com.baselet.diagram.draw.DrawHandler;
import com.baselet.diagram.draw.swing.DrawHandlerSwing;
import com.baselet.element.interfaces.Component;
import com.baselet.element.interfaces.GridElement;

public class G2DComponent implements Component {
	private final DrawHandlerSwing drawer;
	private Rectangle rect;

	public G2DComponent(GridElement gridElement) {
		drawer = new DrawHandlerSwing(gridElement);
	}

	public void paint(Graphics g) {
		drawer.setGraphics(g);
		drawer.drawAll(false);
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
		return drawer;
	}

	@Override
	public void setBoundsRect(Rectangle rect) {
		this.rect = rect;
	}

	@Override
	public void afterModelUpdate() {
	}

	public void setHandler(DiagramHandler diagramHandler) {
		drawer.setHandler(diagramHandler);
	}

	@Override
	public Rectangle getBoundsRect() {
		return rect;
	}

	@Override
	public void repaintComponent() {
		// TODO Auto-generated method stub
	}

}
