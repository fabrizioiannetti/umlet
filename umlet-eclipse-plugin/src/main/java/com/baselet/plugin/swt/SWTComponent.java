package com.baselet.plugin.swt;

import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import com.baselet.control.StringStyle;
import com.baselet.control.basics.geom.DimensionDouble;
import com.baselet.control.basics.geom.PointDouble;
import com.baselet.control.enums.AlignHorizontal;
import com.baselet.control.enums.FormatLabels;
import com.baselet.diagram.draw.DrawFunction;
import com.baselet.diagram.draw.DrawHandler;
import com.baselet.diagram.draw.helper.ColorOwn;
import com.baselet.diagram.draw.helper.Style;
import com.baselet.diagram.draw.helper.theme.Theme;
import com.baselet.diagram.draw.helper.theme.ThemeFactory;
import com.baselet.element.interfaces.Component;
import com.baselet.element.interfaces.GridElement;
import com.baselet.plugin.gui.EclipseGUI;

/**
 * SWT representation of a GridElement.
 *
 * Uses an offline image to render the content and can paint
 * itself on a GC (e.g. for a widget paint)
 *
 * @author fab
 *
 */
public class SWTComponent implements Component {

	private static final Color BACKGROUND_DEFAULT = new Color(Display.getDefault(), 255, 255, 255, 0);

	private static Color toSWTColor(ColorOwn ownColor) {
		return new Color(Display.getDefault(), ownColor.getRed(), ownColor.getGreen(), ownColor.getBlue(), ownColor.getAlpha());
	}

	private static void updateGCWithStyle(GC gc, Style style) {
		gc.setForeground(toSWTColor(style.getForegroundColor()));
		gc.setBackground(toSWTColor(style.getBackgroundColor()));
		gc.setLineWidth((int) style.getLineWidth());
		switch (style.getLineType()) {
			case DASHED:
			case DOUBLE_DASHED:
				gc.setLineStyle(SWT.LINE_DASH);
				break;
			case DOTTED:
			case DOUBLE_DOTTED:
				gc.setLineStyle(SWT.LINE_DOT);
				break;
			case SOLID:
			default:
				gc.setLineStyle(SWT.LINE_SOLID);
				break;
		}
	}

	private interface StyledDrawfunction {
		public abstract void run(Style style);
	}

	private final class SWTDrawHandler extends DrawHandler {

		@Override
		protected void addDrawable(final DrawFunction drawable) {
			final Style styleClone = getStyleClone();
			super.addDrawable(new DrawFunction() {
				@Override
				public void run() {
					updateGCWithStyle(supportGC, styleClone);
					drawable.run();
				}
			});
		}

		private void addDrawable(final StyledDrawfunction drawable) {
			final Style styleClone = getStyleClone();
			super.addDrawable(new DrawFunction() {
				@Override
				public void run() {
					updateGCWithStyle(supportGC, styleClone);
					drawable.run(styleClone);
				}
			});
		}

		@Override
		protected DimensionDouble textDimensionHelper(StringStyle singleLine) {
			double fontSizeInPixels = getFontSize();
			FontMetrics fontMetrics = supportGC.getFontMetrics();
			final double ppi = 72;
			final double dpi = supportGC.getDevice().getDPI().y;
			final int fontSize = (int) (fontSizeInPixels / dpi * ppi);
			Font oldFont = null;
			if (fontMetrics.getHeight() != fontSize) {
				oldFont = supportGC.getFont();
				supportGC.setFont(EclipseGUI.getFontForSize(fontSize, oldFont));
			}
			Point extent = supportGC.stringExtent(singleLine.getStringWithoutMarkup());
			if (oldFont != null) {
				supportGC.setFont(oldFont);
			}
			return new DimensionDouble(extent.x, fontSizeInPixels);
		}

		@Override
		protected double getDefaultFontSize() {
			// TODO@fab
			return 14;
		}

		@Override
		public void drawArc(final double x, final double y, final double width, final double height, final double start, final double extent, final boolean open) {
			final Rectangle bounds = asRect(x, y, width, height, style.getLineWidth());
			// TODO@fab open ?
			addDrawable(new StyledDrawfunction() {
				@Override
				public void run(Style style) {
					supportGC.setAlpha(style.getForegroundColor().getAlpha());
					supportGC.drawArc(bounds.x, bounds.y, bounds.width, bounds.height, (int) start, (int) extent);
				}
			});
		}

		@Override
		public void drawCircle(double x, double y, double radius) {
			drawEllipse(x - radius, y - radius, radius * 2, radius * 2);
		}

		@Override
		public void drawEllipse(final double x, final double y, final double width, final double height) {
			final Rectangle bounds = asRect(x, y, width, height, style.getLineWidth());
			addDrawable(new StyledDrawfunction() {
				@Override
				public void run(Style style) {
					supportGC.setAlpha(style.getBackgroundColor().getAlpha());
					supportGC.fillOval(bounds.x, bounds.y, bounds.width, bounds.height);
					supportGC.setAlpha(style.getForegroundColor().getAlpha());
					supportGC.drawOval(bounds.x, bounds.y, bounds.width, bounds.height);
				}
			});
		}

		@Override
		public void drawLines(final PointDouble... points) {
			if (points.length > 1) {
				final boolean drawOuterLine = style.getLineWidth() > 0;
				addDrawable(new StyledDrawfunction() {
					@Override
					public void run(Style style) {
						drawLineHelper(style, drawOuterLine, points);
					}
				});
			}
		}

		private void drawLineHelper(Style style, boolean drawOuterLine, PointDouble... points) {
			Path path = new Path(getDevice());
			boolean first = true;
			for (PointDouble point : points) {
				int x = point.x.intValue();
				int y = point.y.intValue();
				if (first) {
					path.moveTo(x, y);
					first = false;
				}
				else {
					path.lineTo(x, y);
				}
			}
			if (points[0].equals(points[points.length - 1])) {
				path.close();
				supportGC.setAlpha(style.getBackgroundColor().getAlpha());
				supportGC.fillPath(path);
			}
			if (drawOuterLine) {
				supportGC.setAlpha(style.getForegroundColor().getAlpha());
				supportGC.drawPath(path);
			}
		}

		@Override
		public void drawRectangle(final double x, final double y, final double width, final double height) {
			final Rectangle bounds = asRect(x, y, width, height, style.getLineWidth());
			addDrawable(new StyledDrawfunction() {
				@Override
				public void run(Style style) {
					supportGC.setAlpha(style.getBackgroundColor().getAlpha());
					supportGC.fillRectangle(bounds.x, bounds.y, bounds.width, bounds.height);
					supportGC.setAlpha(style.getForegroundColor().getAlpha());
					supportGC.drawRectangle(bounds.x, bounds.y, bounds.width, bounds.height);
				}
			});
		}

		@Override
		public void drawRectangleRound(final double x, final double y, final double width, final double height, final double radius) {
			final Rectangle bounds = asRect(x, y, width, height, style.getLineWidth());
			final int r = (int) radius * 2;
			addDrawable(new StyledDrawfunction() {
				@Override
				public void run(Style style) {
					supportGC.setAlpha(style.getBackgroundColor().getAlpha());
					supportGC.fillRoundRectangle(bounds.x, bounds.y, bounds.width, bounds.height, r, r);
					supportGC.setAlpha(style.getForegroundColor().getAlpha());
					supportGC.drawRoundRectangle(bounds.x, bounds.y, bounds.width, bounds.height, r, r);
				}
			});
		}

		@Override
		public void printHelper(final StringStyle[] lines, final PointDouble point, final AlignHorizontal align) {
			if (lines.length == 0) {
				return;
			}
			final double fontSizeInPixels = getFontSize();
			addDrawable(new StyledDrawfunction() {
				@Override
				public void run(Style style) {
					// note@fab: move the y position one line back as SWT uses the top-left corner
					// of the surrounding box as reference, while swing uses the font baseline/bottom
					double x = point.x;
					double y = point.y - drawer.textHeightMax();
					final double ppi = 72;
					final double dpi = supportGC.getDevice().getDPI().y;
					final int fontSize = (int) (fontSizeInPixels / dpi * ppi);
					supportGC.setAlpha(style.getForegroundColor().getAlpha());
					for (StringStyle line : lines) {
						Set<FormatLabels> format = line.getFormat();
						final Font base = supportGC.getFont();
						supportGC.setFont(EclipseGUI.getFontForSize(
								fontSize,
								base,
								format.contains(FormatLabels.BOLD),
								format.contains(FormatLabels.ITALIC)));
						double dx = 0;
						switch (align) {
							case CENTER:
								dx = -supportGC.stringExtent(line.getStringWithoutMarkup()).x / 2.0;
								break;
							case RIGHT:
								dx = -supportGC.stringExtent(line.getStringWithoutMarkup()).x;
								break;
							case LEFT:
							default:
								break;
						}
						// set transparency = true for text, as the background alpha is not directly transferred
						supportGC.drawText(line.getStringWithoutMarkup(), (int) (x + dx), (int) y, true);
						y += textHeightMax();
					}

				}
			});
		}

		private Rectangle asRect(double x, double y, double w, double h, double lineWidth) {
			int delta = Math.min((int) lineWidth / 2, (int) Math.min(w / 2, h / 2));
			supportGC.setAlpha(style.getBackgroundColor().getAlpha());
			return new Rectangle((int) x + delta, (int) y + delta, (int) w - delta * 2, (int) h - delta * 2);
		}
	}

	private Image support;
	private GC supportGC;
	private final DrawHandler drawer;
	private final DrawHandler metaDrawer;
	private final GridElement element;
	private boolean lastSelected;
	private boolean redrawNecessary;
	private com.baselet.control.basics.geom.Rectangle rect = new com.baselet.control.basics.geom.Rectangle(0, 0, 0, 0);

	public SWTComponent(GridElement gridElement) {
		support = new Image(getDevice(), 100, 100);
		supportGC = new GC(support);
		supportGC.setBackground(BACKGROUND_DEFAULT);
		drawer = new SWTDrawHandler();
		metaDrawer = new SWTDrawHandler();
		element = gridElement;
	}

	private Display getDevice() {
		return Display.getDefault();
	}

	public void drawOn(GC context, boolean isSelected) {
		if (redrawNecessary || lastSelected != isSelected) {
			redrawNecessary = false;
			Rectangle swtBounds = new Rectangle(0, 0, rect.width + 1, rect.height + 1);
			if (!swtBounds.equals(support.getBounds())) {
				replaceSupport(swtBounds);
			}
			else {
				// fill with alpha 0
				resetSupport();
			}
			// now fill with background, including alpha
			ColorOwn bgOwnColor = ThemeFactory.getCurrentTheme().getColor(Theme.ColorStyle.DEFAULT_BACKGROUND);
			RGBA bgColorWithAlpha = new RGBA(bgOwnColor.getRed(), bgOwnColor.getGreen(), bgOwnColor.getBlue(), bgOwnColor.getAlpha());
			// RGB bgColor = new RGB(bgOwnColor.getRed(), bgOwnColor.getGreen(), bgOwnColor.getBlue());
			supportGC.setBackground(new Color(support.getDevice(), bgColorWithAlpha));
			supportGC.setAlpha(bgColorWithAlpha.alpha);
			supportGC.fillRectangle(support.getBounds());
			drawer.drawAll(isSelected);
			if (isSelected) {
				metaDrawer.drawAll();
			}
		}
		lastSelected = isSelected;
		context.drawImage(support, element.getRectangle().getX(), element.getRectangle().getY());
	}

	private void replaceSupport(Rectangle newBounds) {
		// create a support image with alpha channel
		supportGC.dispose();
		support.dispose();
		ImageData imageData = new ImageData(newBounds.width, newBounds.height, 32, new PaletteData(0x0000FF, 0x00FF, 0xFF));
		imageData.alphaData = new byte[newBounds.width * newBounds.height];
		support = new Image(getDevice(), imageData);
		supportGC = new GC(support);
	}

	private void resetSupport() {
		// SWT does not seem to support a way to reset the image (SOURCE operator)
		// re-create the image for now
		// TODO: ask SWT mailing list
		replaceSupport(support.getBounds());
	}
	// private static Image resetPattern;
	//
	// private void resetSupport() {
	// if (resetPattern == null) {
	// ImageData imageData = new ImageData(256, 256, 32, new PaletteData(0x0000FF, 0x00FF, 0xFF));
	// int pixelValue = imageData.palette.getPixel(new RGB(255, 0, 255));
	// int alpha = 0;
	// for (int ix = 0; ix < 255; ix++) {
	// for (int iy = 0; iy < 255; iy++) {
	// imageData.setPixel(ix, iy, pixelValue);
	// imageData.setAlpha(ix, iy, alpha);
	// }
	// }
	// resetPattern = new Image(getDevice(), imageData);
	// }
	// GC resetGC = new GC(resetPattern);
	// int m = (support.getBounds().width + 255) / 256;
	// int n = (support.getBounds().height + 255) / 256;
	// for (int j = 0; j < n; j++) {
	// int y = j * 256;
	// for (int i = 0; i < m; i++) {
	// int x = i * 256;
	// resetGC.copyArea(support, x, y);
	// }
	// }
	// resetGC.dispose();
	// ImageData supportData = support.getImageData();
	// System.out.println("alpha=" + supportData.getAlpha(0, 0));
	// }

	@Override
	public void translateForExport() {
		// translation breaks export of some elements, therefore its disabled - see issue 353
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

	@Override
	public com.baselet.control.basics.geom.Rectangle getBoundsRect() {
		return rect.copy();
	}

	@Override
	public void repaintComponent() {
		// components repaints itself if needed in drawOn()
		// i.e. before painting itself on the canvas
	}

	@Override
	public void setBoundsRect(com.baselet.control.basics.geom.Rectangle rect) {
		this.rect = rect.copy();
	}

	@Override
	public void afterModelUpdate() {
		redrawNecessary = true;
	}
}
