package com.baselet.plugin.swt;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;

import com.baselet.element.interfaces.CursorOwn;

public class SWTConverter {

	private static Map<CursorOwn, Cursor> cursorMap = new HashMap<CursorOwn, Cursor>(12);

	public static void init(Display display) {
		cursorMap.put(CursorOwn.N, display.getSystemCursor(SWT.CURSOR_SIZEN));
		cursorMap.put(CursorOwn.NE, display.getSystemCursor(SWT.CURSOR_SIZENE));
		cursorMap.put(CursorOwn.E, display.getSystemCursor(SWT.CURSOR_SIZEE));
		cursorMap.put(CursorOwn.SE, display.getSystemCursor(SWT.CURSOR_SIZESE));
		cursorMap.put(CursorOwn.S, display.getSystemCursor(SWT.CURSOR_SIZES));
		cursorMap.put(CursorOwn.SW, display.getSystemCursor(SWT.CURSOR_SIZESW));
		cursorMap.put(CursorOwn.W, display.getSystemCursor(SWT.CURSOR_SIZEW));
		cursorMap.put(CursorOwn.NW, display.getSystemCursor(SWT.CURSOR_SIZENW));
		cursorMap.put(CursorOwn.HAND, display.getSystemCursor(SWT.CURSOR_HAND));
		cursorMap.put(CursorOwn.MOVE, display.getSystemCursor(SWT.CURSOR_SIZEALL));
		cursorMap.put(CursorOwn.DEFAULT, display.getSystemCursor(SWT.CURSOR_ARROW));
		cursorMap.put(CursorOwn.CROSS, display.getSystemCursor(SWT.CURSOR_CROSS));
		cursorMap.put(CursorOwn.TEXT, display.getSystemCursor(SWT.CURSOR_IBEAM));
	}

	public static Cursor cursor(CursorOwn in) {
		if (cursorMap.isEmpty()) {
			init(Display.getDefault());
		}
		Cursor cursor = cursorMap.get(in);
		if (cursor == null) {
			cursor = Display.getDefault().getSystemCursor(SWT.CURSOR_ARROW);
		}
		return cursor;
	}

}
