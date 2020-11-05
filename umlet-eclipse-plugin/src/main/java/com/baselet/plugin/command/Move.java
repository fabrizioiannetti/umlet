package com.baselet.plugin.command;

import java.util.Collection;

import com.baselet.command.Command;
import com.baselet.control.basics.geom.Point;
import com.baselet.control.enums.Direction;
import com.baselet.element.interfaces.GridElement;
import com.baselet.element.sticking.StickableMap;

public class Move extends Command {

	private final GridElement entity;
	private final int x;
	private final int y;
	private final boolean isShiftKeyDown;
	private final boolean firstDrag;
	private final boolean useSetLocation;
	private final StickableMap stickables;
	private final Collection<Direction> resizeDirection;
	private final double mouseX;
	private final double mouseY;
	private boolean isRedo;

	public Move(Collection<Direction> resizeDirection, boolean absoluteMousePos, GridElement e, int x, int y, Point mousePosBeforeDrag, boolean isShiftKeyDown, boolean firstDrag, boolean useSetLocation, StickableMap stickingStickables) {
		entity = e;
		this.x = x;
		this.y = y;
		mouseX = calcRelativePos(absoluteMousePos, mousePosBeforeDrag.getX(), entity.getRectangle().getX());
		mouseY = calcRelativePos(absoluteMousePos, mousePosBeforeDrag.getY(), entity.getRectangle().getY());
		this.isShiftKeyDown = isShiftKeyDown;
		this.firstDrag = firstDrag;
		this.useSetLocation = useSetLocation;
		stickables = stickingStickables;
		this.resizeDirection = resizeDirection;
	}

	public Move(Collection<Direction> resizeDirection, GridElement e, int x, int y, Point mousePosBeforeDrag, boolean isShiftKeyDown, boolean firstDrag, boolean useSetLocation, StickableMap stickingStickables) {
		this(resizeDirection, true, e, x, y, mousePosBeforeDrag, isShiftKeyDown, firstDrag, useSetLocation, stickingStickables);
	}

	private double calcRelativePos(boolean absoluteMousePos, int mousePos, int entityLocation) {
		double xCalcBase = mousePos * 1.0;
		if (absoluteMousePos) {
			xCalcBase -= entityLocation;
		}
		return xCalcBase;
	}

	private Point getMousePosBeforeDrag() {
		return new Point((int) Math.round(mouseX), (int) Math.round(mouseY));
	}

	@Override
	public void execute() {
		if (isRedo) {
			redo();
			return;
		}
		if (useSetLocation) {
			entity.setRectangleDifference(x, y, 0, 0, firstDrag, stickables, true);
		}
		else {
			entity.drag(resizeDirection, x, y, getMousePosBeforeDrag(), isShiftKeyDown, firstDrag, stickables, true);
		}
		isRedo = true;
	}

	@Override
	public void undo() {
		entity.undoDrag();
		entity.updateModelFromText();
	}

	public void redo() {
		entity.redoDrag();
		entity.updateModelFromText();
	}

	@Override
	public boolean isMergeableTo(Command c) {
		if (!(c instanceof Move)) {
			return false;
		}
		Move m = (Move) c;
		boolean stickablesEquals = stickables.equalsMap(m.stickables);
		boolean shiftEquals = isShiftKeyDown == m.isShiftKeyDown;
		boolean notBothFirstDrag = !(firstDrag && m.firstDrag);
		return entity == m.entity && useSetLocation == m.useSetLocation && stickablesEquals && shiftEquals && notBothFirstDrag;
	}

	@Override
	public Command mergeTo(Command c) {
		Move m = (Move) c;
		Point mousePosBeforeDrag = firstDrag ? getMousePosBeforeDrag() : m.getMousePosBeforeDrag();
		// Important: absoluteMousePos=false, because the mousePos is already relative from the first constructor call!
		Move ret = new Move(m.resizeDirection, false, entity, x + m.y, y + m.y, mousePosBeforeDrag, isShiftKeyDown, firstDrag || m.firstDrag, useSetLocation, stickables);
		ret.isRedo = isRedo;
		entity.mergeUndoDrag();
		return ret;
	}

}