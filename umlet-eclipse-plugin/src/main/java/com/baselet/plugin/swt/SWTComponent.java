package com.baselet.plugin.swt;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import com.baselet.control.StringStyle;
import com.baselet.control.basics.geom.DimensionDouble;
import com.baselet.control.basics.geom.PointDouble;
import com.baselet.control.basics.geom.Rectangle;
import com.baselet.control.enums.AlignHorizontal;
import com.baselet.diagram.draw.DrawFunction;
import com.baselet.diagram.draw.DrawHandler;
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

	private final class DrawHandlerExtension extends DrawHandler {
		@Override
		protected DimensionDouble textDimensionHelper(StringStyle singleLine) {
			// TODO@fab font size?
			Point extent = supportGC.stringExtent(singleLine.getStringWithoutMarkup());
			return new DimensionDouble(extent.x, extent.y);
		}

		@Override
		protected double getDefaultFontSize() {
			// TODO@fab
			return 12;
		}

		@Override
		public void drawArc(final double x, final double y, final double width, final double height, final double start, final double extent, final boolean open) {
			// TODO@fab open ?
			addDrawable(new DrawFunction() {
				@Override
				public void run() {
					supportGC.drawArc((int) x, (int) y, (int) width, (int) height, (int) start, (int) extent);
				}
			});
		}

		@Override
		public void drawCircle(double cx, double cy, double radius) {
			drawEllipse(cx - radius, cy - radius, radius * 2, radius * 2);
		}

		@Override
		public void drawEllipse(final double x, final double y, final double width, final double height) {
			addDrawable(new DrawFunction() {
				@Override
				public void run() {
					supportGC.drawOval((int) x, (int) y, (int) width, (int) height);
				}
			});
		}

		@Override
		public void drawLines(final PointDouble... points) {
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
				if (first) {
					path.moveTo(point.x.floatValue(), point.y.floatValue());
					first = false;
				}
				else {
					path.lineTo(point.x.floatValue(), point.y.floatValue());
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
			addDrawable(new DrawFunction() {
				@Override
				public void run() {
					supportGC.drawRectangle((int) x, (int) y, (int) width, (int) height);
				}
			});
		}

		@Override
		public void drawRectangleRound(final double x, final double y, final double width, final double height, final double radius) {
			addDrawable(new DrawFunction() {
				@Override
				public void run() {
					supportGC.drawRoundRectangle((int) x, (int) y, (int) width, (int) height, (int) radius, (int) radius);
				}
			});
		}

		@Override
		public void printHelper(final StringStyle[] lines, final PointDouble point, final AlignHorizontal align) {
			final double fontSizeInPixels = getStyleClone().getFontSize();
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
						supportGC.drawText(line.getStringWithoutMarkup(), (int) (x + dx), (int) y);
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
	private Rectangle rect = new Rectangle(0, 0, 32, 32);

	public SWTComponent(GridElement gridElement) {
		support = new Image(getDevice(), 100, 100);
		supportGC = new GC(support);
		drawer = new DrawHandlerExtension();
		metaDrawer = new DrawHandlerExtension();
		element = gridElement;
	}

	private Display getDevice() {
		return Display.getDefault();
	}

	public void drawOn(GC context, boolean isSelected, double scaling) {
		// override for now
		scaling = 1d;
		// TODO@fab needed?
		// drawer.setNewScaling(scaling);
		// metaDrawer.setNewScaling(scaling);
		if (redrawNecessary || lastSelected != isSelected) {
			redrawNecessary = false;
			// TODO@fab scaling ?
			// bounds.width *= scaling;
			// bounds.height *= scaling;
			org.eclipse.swt.graphics.Rectangle swtBounds = new org.eclipse.swt.graphics.Rectangle(
					0,
					0,
					rect.width + 1,
					rect.height + 1);
			// (int) (bounds.width * scaling) + (int) Math.ceil(1d * scaling), // canvas size is +1px to make sure a rectangle with width pixels is still visible (in Swing the bound-checking happens in BaseDrawHandlerSwing because you cannot extend the clipping area)
			// (int) (bounds.height * scaling) + (int) Math.ceil(1d * scaling));
			if (!swtBounds.equals(support.getBounds())) {
				supportGC.dispose();
				support.dispose();
				support = new Image(getDevice(), swtBounds.width, swtBounds.height);
				supportGC = new GC(support);
			}
			supportGC.fillRectangle(swtBounds);
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
	public Rectangle getBoundsRect() {
		return rect;
	}

	@Override
	public void repaintComponent() {
		// TODO@fab not necessary?
	}

	@Override
	public void setBoundsRect(Rectangle rect) {
		this.rect = rect.copy();
	}

	@Override
	public void afterModelUpdate() {
		redrawNecessary = true;
	}
}
