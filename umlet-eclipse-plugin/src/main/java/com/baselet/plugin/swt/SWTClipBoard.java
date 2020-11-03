package com.baselet.plugin.swt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.ImageTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.widgets.Display;

import com.baselet.control.basics.geom.Rectangle;
import com.baselet.control.config.Config;
import com.baselet.element.interfaces.GridElement;
import com.baselet.plugin.gui.EclipseGUI;

public class SWTClipBoard {
	private static final SWTElementFactory ELEMENT_FACTORY = new SWTElementFactory();
	private final static List<GridElement> content = new ArrayList<GridElement>();
	private final static Clipboard clipboard = new Clipboard(Display.getDefault());

	public static void copyElements(List<GridElement> elements) {
		SWTDiagramHandler targetDiagram = new SWTDiagramHandler();
		content.clear();
		for (GridElement gridElement : elements) {
			content.add(ELEMENT_FACTORY.create(gridElement, targetDiagram));
		}
		targetDiagram.getGridElements().addAll(content);
		Rectangle boundingBox = targetDiagram.getBoundingBox(Config.getInstance().getPrintPadding());
		if (boundingBox.width > 0 && boundingBox.height > 0) {
			// TODO@fab: render image
			Object[] data = new Object[] { new ImageData(boundingBox.width, boundingBox.height, 32, new PaletteData(0xff0000, 0x00ff00, 0x0000ff)) };
			Transfer[] dataTypes = new Transfer[] { ImageTransfer.getInstance() };
			clipboard.setContents(data, dataTypes);
			EclipseGUI.setPasteAvailable(true);
		}
		else {
			clipboard.clearContents();
			EclipseGUI.setPasteAvailable(false);
		}
	}

	public static void pasteElements(List<GridElement> targetList, SWTDiagramHandler targetDiagram) {
		List<GridElement> elementsToPaste = new ArrayList<GridElement>();
		for (GridElement gridElement : content) {
			elementsToPaste.add(ELEMENT_FACTORY.create(gridElement, targetDiagram));
		}
		targetList.addAll(elementsToPaste);
	}
}
