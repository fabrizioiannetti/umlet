package com.baselet.plugin.swt;

import java.io.File;

import com.baselet.diagram.DrawPanel;
import com.baselet.element.interfaces.GridElement;
import com.baselet.gui.listener.GridElementListener;

public class SWTPaletteHandler extends SWTDiagramHandler {

	public SWTPaletteHandler(File palettefile) {
		super();
	}

	public GridElementListener getEntityListener(GridElement e) {
		return null; // PaletteEntityListener.getInstance(this);
	}

	protected void initDiagramPopupMenu(boolean extendedPopupMenu) {
		/* no diagram popup menu */
	}

	protected DrawPanel createDrawPanel() {
		return null; // new DrawPanel(this, false); /* no startup and filedrop */
	}
}
