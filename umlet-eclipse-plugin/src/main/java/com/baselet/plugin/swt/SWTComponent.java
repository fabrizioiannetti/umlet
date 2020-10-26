package com.baselet.plugin.swt;

import org.eclipse.jface.resource.FontDescriptor;
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
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baselet.control.StringStyle;
import com.baselet.control.basics.geom.DimensionDouble;
import com.baselet.control.basics.geom.PointDouble;
import com.baselet.control.enums.AlignHorizontal;
import com.baselet.diagram.draw.DrawFunction;
import com.baselet.diagram.draw.DrawHandler;
import com.baselet.diagram.draw.helper.ColorOwn;
import com.baselet.diagram.draw.helper.Style;
import com.baselet.element.interfaces.Component;
import com.baselet.element.interfaces.GridElement;

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

	private static final Logger log = LoggerFactory.getLogger(SWTComponent.class);
	private static final Color BACKGROUND_DEFAULT = new Color(255, 255, 255, 0);

	private static Color toSWTColor(ColorOwn ownColor) {
		return new Color(ownColor.getRed(), ownColor.getGreen(), ownColor.getBlue(), ownColor.getAlpha());
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

	private final class DrawHandlerExtension extends DrawHandler {

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
				supportGC.setFont(
						FontDescriptor
								.createFrom(supportGC.getFont())
								.setHeight(fontSize)
								.createFont(getDevice()));
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
			log.info("drawArc x=" + x + " y=" + y + " w=" + width + " h=" + height);
			// TODO@fab open ?
			addDrawable(new DrawFunction() {
				@Override
				public void run() {
					supportGC.drawArc((int) x, (int) y, (int) width, (int) height, (int) start, (int) extent);
				}
			});
		}

		@Override
		public void drawCircle(double x, double y, double radius) {
			log.info("drawCircle x=" + x + " y=" + y + " radius=" + radius);
			drawEllipse(x - radius, y - radius, radius * 2, radius * 2);
		}

		@Override
		public void drawEllipse(final double x, final double y, final double width, final double height) {
			log.info("drawEllipse x=" + x + " y=" + y + " w=" + width + " h=" + height);
			addDrawable(new DrawFunction() {
				@Override
				public void run() {
					supportGC.fillOval((int) x, (int) y, (int) width, (int) height);
					supportGC.drawOval((int) x, (int) y, (int) width, (int) height);
				}
			});
		}

		@Override
		public void drawLines(final PointDouble... points) {
			String linesLog = "";
			for (PointDouble point : points) {
				linesLog += point + ":";
			}
			log.info("drawLines " + linesLog);
			if (points.length > 1) {
				final boolean drawOuterLine = style.getLineWidth() > 0;
				addDrawable(new DrawFunction() {
					@Override
					public void run() {
						drawLineHelper(drawOuterLine, points);
					}
				});
			}
		}

		private void drawLineHelper(boolean drawOuterLine, PointDouble... points) {
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
				supportGC.fillPath(path);
			}
			if (drawOuterLine) {
				supportGC.drawPath(path);
			}
		}

		@Override
		public void drawRectangle(final double x, final double y, final double width, final double height) {
			log.info("drawRectangle x=" + x + " y=" + y + " w=" + width + " h=" + height);
			addDrawable(new DrawFunction() {
				@Override
				public void run() {
					supportGC.fillRectangle((int) x, (int) y, (int) width, (int) height);
					supportGC.drawRectangle((int) x, (int) y, (int) width, (int) height);
				}
			});
		}

		@Override
		public void drawRectangleRound(final double x, final double y, final double width, final double height, final double radius) {
			log.info("drawRectangleRound x=" + x + " y=" + y + " w=" + width + " h=" + height);
			addDrawable(new DrawFunction() {
				@Override
				public void run() {
					supportGC.drawRoundRectangle((int) x, (int) y, (int) width, (int) height, (int) radius, (int) radius);
				}
			});
		}

		@Override
		public void printHelper(final StringStyle[] lines, final PointDouble point, final AlignHorizontal align) {
			log.info("printText x=" + point.x + " y=" + point.y + " line[0]=" + (lines.length > 0 ? lines[0] : "<>"));
			final double fontSizeInPixels = getFontSize();
			addDrawable(new DrawFunction() {
				@Override
				public void run() {
					// note@fab: move the y position one line back as SWT uses the top-left corner
					// of the surrounding box as reference, while swing uses the font baseline/bottom
					double x = point.x;
					double y = point.y - drawer.textHeightMax();
					// TODO@fabColorOwn fgColor = getOverlay().getForegroundColor() != null ? getOverlay().getForegroundColor() : styleAtDrawingCall.getForegroundColor();
					// TODO@fab ctx.setFillStyle(Converter.convert(fgColor));
					FontMetrics fontMetrics = supportGC.getFontMetrics();
					final double ppi = 72;
					final double dpi = supportGC.getDevice().getDPI().y;
					final int fontSize = (int) (fontSizeInPixels / dpi * ppi);
					if (fontMetrics.getHeight() != fontSize) {
						supportGC.setFont(
								FontDescriptor
										.createFrom(supportGC.getFont())
										.setHeight(fontSize)
										.createFont(getDevice()));
					}
					for (StringStyle line : lines) {
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
						// TODO@fab use transparency = true for text, as the background alpha is not directly transferred
						supportGC.drawText(line.getStringWithoutMarkup(), (int) (x + dx), (int) y, true);
						y += textHeightMax();
					}

				}
			});
		}
		// private void drawTextHelper(final StringStyle line, PointDouble p, AlignHorizontal align, double fontSize) {
		//
		// ctxSetFont(fontSize, line);
		//
		// String textToDraw = line.getStringWithoutMarkup();
		// if (textToDraw == null || textToDraw.isEmpty()) {
		// return; // if nothing should be drawn return (some browsers like Opera have problems with ctx.fillText calls on empty strings)
		// }
		//
		// ctxSetTextAlign(align);
		// ctx.fillText(textToDraw, p.x, p.y);
		//
		// if (line.getFormat().contains(FormatLabels.UNDERLINE)) {
		// ctx.setLineWidth(1.0f);
		// setLineDash(ctx, LineType.SOLID, 1.0f);
		// double textWidth = textWidth(line);
		// int vDist = 1;
		// switch (align) {
		// case LEFT:
		// drawLineHelper(true, new PointDouble(p.x, p.y + vDist), new PointDouble(p.x + textWidth, p.y + vDist));
		// break;
		// case CENTER:
		// drawLineHelper(true, new PointDouble(p.x - textWidth / 2, p.y + vDist), new PointDouble(p.x + textWidth / 2, p.y + vDist));
		// break;
		// case RIGHT:
		// drawLineHelper(true, new PointDouble(p.x - textWidth, p.y + vDist), new PointDouble(p.x, p.y + vDist));
		// break;
		// }
		// }
		// }
	}

	private Image support;
	private GC supportGC;
	private final DrawHandler drawer;
	private final DrawHandler metaDrawer;
	private final GridElement element;
	private boolean lastSelected;
	private boolean redrawNecessary;
	private com.baselet.control.basics.geom.Rectangle rect = new com.baselet.control.basics.geom.Rectangle(0, 0, 32, 32);

	public SWTComponent(GridElement gridElement) {
		support = new Image(getDevice(), 100, 100);
		supportGC = new GC(support);
		supportGC.setBackground(BACKGROUND_DEFAULT);
		drawer = new DrawHandlerExtension();
		metaDrawer = new DrawHandlerExtension();
		element = gridElement;
	}

	private Display getDevice() {
		return Display.getDefault();
	}

	public void drawOn(GC context, boolean isSelected, double scaling) {
		// override for now
		// scaling = 1d;
		// TODO@fab needed?
		// drawer.setNewScaling(scaling);
		// metaDrawer.setNewScaling(scaling);
		if (redrawNecessary || lastSelected != isSelected) {
			redrawNecessary = false;
			ColorOwn bgOwnColor = drawer.getBackgroundColor();
			RGB bgColor = new RGB(bgOwnColor.getRed(), bgOwnColor.getGreen(), bgOwnColor.getBlue());
			RGBA bgColorWithAlpha = new RGBA(bgOwnColor.getRed(), bgOwnColor.getGreen(), bgOwnColor.getBlue(), bgOwnColor.getAlpha());
			// TODO@fab scaling ?
			// bounds.width *= scaling;
			// bounds.height *= scaling;
			Rectangle swtBounds = new Rectangle(0, 0, rect.width + 1, rect.height + 1);
			// (int) (bounds.width * scaling) + (int) Math.ceil(1d * scaling), // canvas size is +1px to make sure a rectangle with width pixels is still visible (in Swing the bound-checking happens in BaseDrawHandlerSwing because you cannot extend the clipping area)
			// (int) (bounds.height * scaling) + (int) Math.ceil(1d * scaling));
			if (!swtBounds.equals(support.getBounds())) {
				supportGC.dispose();
				support.dispose();
				ImageData imageData = new ImageData(swtBounds.width, swtBounds.height, 32, new PaletteData(0x0000FF, 0x00FF, 0xFF));
				int pixelValue = imageData.palette.getPixel(bgColor);
				int alpha = bgColorWithAlpha.alpha;
				for (int ix = 0; ix < swtBounds.width; ix++) {
					for (int iy = 0; iy < swtBounds.height; iy++) {
						imageData.setPixel(ix, iy, pixelValue);
						imageData.setAlpha(ix, iy, alpha);
					}
				}
				support = new Image(getDevice(), imageData);
				supportGC = new GC(support);
				supportGC.setBackground(new Color(bgColorWithAlpha));
			}
			// supportGC.fillRectangle(support.getBounds());
			drawer.drawAll(isSelected);
			if (isSelected) {
				metaDrawer.drawAll();
			}
		}
		lastSelected = isSelected;
		context.drawImage(support, (int) (element.getRectangle().getX() * scaling), (int) (element.getRectangle().getY() * scaling));
	}

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
		return rect;
	}

	@Override
	public void repaintComponent() {
		// TODO@fab not necessary?
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
