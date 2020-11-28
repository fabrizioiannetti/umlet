package com.baselet.plugin.core;

import java.util.List;

import com.baselet.element.interfaces.GridElement;

public interface IClipboard {
	public void copyElements(List<GridElement> elements);

	public void pasteElements(List<GridElement> targetList, DiagramModel targetDiagram);
}
