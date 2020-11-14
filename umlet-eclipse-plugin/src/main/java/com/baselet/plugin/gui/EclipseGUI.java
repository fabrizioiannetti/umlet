package com.baselet.plugin.gui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;

public class EclipseGUI {

	private static Contributor contributor;

	public static void setContributor(Contributor contributor) {
		EclipseGUI.contributor = contributor;
	}

	public static void setPasteAvailable(boolean available) {
		if (contributor != null) {
			contributor.setPaste(available);
		}
	}

	public static void setUndoRedoAvailable(boolean undoAvailable, boolean redoAvailable) {
		if (contributor != null) {
			contributor.setUndoRedoAvailable(undoAvailable, redoAvailable);
		}
	}

	static private class FontEntry {
		public Font regular;
		public Font bold;
		public Font italic;
		public Font boldItalic;

		public void dispose() {
			disposefont(regular);
			disposefont(bold);
			disposefont(italic);
			disposefont(boldItalic);
		}

		private void disposefont(Font font) {
			if (font != null) {
				font.dispose();
			}
		}
	}

	private static Map<Integer, FontEntry> fontRegistry = new HashMap<Integer, FontEntry>();

	public static Font getFontForSize(int fontSizeInPoints, Font base, boolean bold, boolean italic) {
		FontEntry fontEntry = fontRegistry.get(fontSizeInPoints);
		Font font;
		if (fontEntry == null) {
			fontEntry = new FontEntry();
			FontDescriptor fontDescriptor = FontDescriptor
					.createFrom(base)
					.setHeight(fontSizeInPoints);
			fontEntry.regular = fontDescriptor.createFont(base.getDevice());
			fontEntry.bold = fontDescriptor.setStyle(SWT.BOLD).createFont(base.getDevice());
			fontEntry.italic = fontDescriptor.setStyle(SWT.ITALIC).createFont(base.getDevice());
			fontEntry.boldItalic = fontDescriptor.setStyle(SWT.BOLD | SWT.ITALIC).createFont(base.getDevice());
			fontRegistry.put(fontSizeInPoints, fontEntry);
		}
		if (!bold && !italic) {
			font = fontEntry.regular;
		}
		else if (bold && italic) {
			font = fontEntry.boldItalic;
		}
		else if (bold) {
			font = fontEntry.bold;
		}
		else {
			font = fontEntry.italic;
		}
		return font;
	}

	public static Font getFontForSize(int fontSizeInPoints, Font oldbase) {
		return getFontForSize(fontSizeInPoints, oldbase, false, false);
	}

	public static void disposeFontforSize(int fontSizeInPoints) {
		FontEntry fontEntry = fontRegistry.remove(fontSizeInPoints);
		if (fontEntry != null) {
			fontEntry.dispose();
		}
	}
}
