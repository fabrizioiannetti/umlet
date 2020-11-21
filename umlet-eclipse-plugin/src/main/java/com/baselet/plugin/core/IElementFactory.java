package com.baselet.plugin.core;

import com.baselet.control.basics.geom.Rectangle;
import com.baselet.control.enums.ElementId;
import com.baselet.element.interfaces.Diagram;
import com.baselet.element.interfaces.GridElement;

public interface IElementFactory {

	GridElement create(ElementId id, Rectangle rect, String panelAttributes, String additionalPanelAttributes, Diagram diagram);

}