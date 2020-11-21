package com.baselet.plugin.swt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.ImageTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

import com.baselet.control.basics.geom.Rectangle;
import com.baselet.control.config.Config;
import com.baselet.element.interfaces.GridElement;
import com.baselet.plugin.core.DiagramModel;
import com.baselet.plugin.gui.EclipseGUI;

public class SWTClipBoard {
	private static final SWTElementFactory ELEMENT_FACTORY = new SWTElementFactory();
	private final static List<GridElement> content = new ArrayList<GridElement>();
	private static Clipboard clipboard = null;

	public static void copyElements(List<GridElement> elements) {
		clipboard = new Clipboard(Display.getDefault());
		DiagramModel targetDiagram = new DiagramModel();
		content.clear();
		for (GridElement gridElement : elements) {
			content.add(ELEMENT_FACTORY.create(gridElement, targetDiagram));
		}
		targetDiagram.getGridElements().addAll(content);
		Rectangle boundingBox = targetDiagram.getBoundingBox(Config.getInstance().getPrintPadding());
		if (boundingBox.width > 0 && boundingBox.height > 0) {
			Image image = SWTOutputHandler.createImageForGridElements(content);
			ImageData imageData = image.getImageData();
			image.dispose();
			Object[] data = new Object[] { imageData, "ImageData(" + imageData.width + "," + imageData.height + ")" };
			Transfer[] dataTypes = new Transfer[] { ImageTransfer.getInstance(), TextTransfer.getInstance() };
			clipboard.setContents(data, dataTypes);
			EclipseGUI.setPasteAvailable(true);
		}
		else {
			clipboard.clearContents();
			EclipseGUI.setPasteAvailable(false);
		}
	}

	public static void pasteElements(List<GridElement> targetList, DiagramModel targetDiagram) {
		List<GridElement> elementsToPaste = new ArrayList<GridElement>();
		for (GridElement gridElement : content) {
			elementsToPaste.add(ELEMENT_FACTORY.create(gridElement, targetDiagram));
		}
		targetList.addAll(elementsToPaste);
	}
}
