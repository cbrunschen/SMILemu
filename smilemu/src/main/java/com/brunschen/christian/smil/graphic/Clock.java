/*
 * Clock
 *
 * Copyright (C) 2005  Christian Brunschen
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

package com.brunschen.christian.smil.graphic;

import com.brunschen.christian.graphic.AbstractGraphic;
import com.brunschen.christian.graphic.AffineTransform;
import com.brunschen.christian.graphic.Color;
import com.brunschen.christian.graphic.Ellipse;
import com.brunschen.christian.graphic.Font;
import com.brunschen.christian.graphic.Path;
import com.brunschen.christian.graphic.Rectangle;
import com.brunschen.christian.graphic.Size;
import com.brunschen.christian.graphic.StrokeStyle;
import com.brunschen.christian.graphic.Surface;

public class Clock extends AbstractGraphic {

  private static Path HAND_PATH = new Path();
  static {
    HAND_PATH.moveTo(-0.07, 0.07);
    HAND_PATH.lineTo(0, -0.95);
    HAND_PATH.lineTo(0.07, 0.07);
    HAND_PATH.closePath();
  }

  private static long millisPerSecond = 1000;
  private static long millisPerMinute = 60 * millisPerSecond;
  private static long millisPerHour = 60 * millisPerMinute;
  private long startTime;
  private Font font;
  protected AffineTransform hoursTransform;
  private Rectangle hoursRect;
  private StrokeStyle strokeStyle;
  private Path handPath;

  public Clock(Size size, Font font, long startTime) {
    super(size);
    this.startTime = startTime;
    this.font = font;
    this.strokeStyle = new StrokeStyle(1.0);

    double rMax = Math.min(size.getWidth(), size.getHeight()) / 2;
    handPath = HAND_PATH.transformed(AffineTransform.makeScale(rMax, rMax));
  }

  public Clock(Size size, Font font) {
    this(size, font, System.currentTimeMillis());
  }

  private void drawString(Surface g, String s, double x, double y, double r, double t) {
    g.save();
    g.setColor(Color.WHITE);
    g.setFont(font.atSize(r / 3.0));
    Rectangle rect = g.measureString(s);
    // System.err.format("String rect for '%s' = %s\n", s, Util.printRect(rect));
    double dx = Math.cos(t);
    double dy = Math.sin(t);
    // System.err.format("drawing string '%s' with dx = %f, dy = %f\n", s, dx, dy);
    double x0 = x + r * dx;
    double y0 = y + r * dy;
    g.translate(x0 - rect.getWidth() / 2, y0 - rect.getHeight() / 2);
    g.translate(-rect.getX(), -rect.getY());
    g.drawString(s, 0, 0);
    g.restore();
  }

  private static Rectangle hoursRect(double x, double y, double r) {
    return new Rectangle(x - r * 0.6, y + r * 0.2, r * 1.2, r * 0.25);
  }

  private void calculateHoursTransform(Surface g, String s) {
    Rectangle stringRect = g.measureString(s);

    double scale = Math.min(hoursRect.getWidth() / stringRect.getWidth(), hoursRect.getHeight() / hoursRect.getHeight());
    hoursTransform = AffineTransform.makeTranslate(hoursRect.getCenterX(), hoursRect.getCenterY());
    hoursTransform.scale(scale, scale);
    hoursTransform.translate(-stringRect.getCenterX(), -stringRect.getCenterY());
  }
  
  private void drawBackground(Surface g, double x, double y, double r) {
    double rInner = r * 0.85;
    double rLabels = r * 0.65;
    g.setColor(Color.BLACK);
    g.fill(new Ellipse(x - r, y - r, 2 * r, 2 * r));
    g.setColor(Color.WHITE);
    g.stroke(new Ellipse(x - rInner, y - rInner, 2 * rInner, 2 * rInner));
    drawString(g, "30", x, y, rLabels, Math.PI / 2);
    drawString(g, "15", x, y, rLabels, 0);
    drawString(g, "60", x, y, rLabels, 3 * Math.PI / 2);
    drawString(g, "45", x, y, rLabels, Math.PI);
    g.fill(hoursRect);
    g.stroke(hoursRect);
  }

  @Override
  public void draw(Surface g) {
    g.setStrokeStyle(strokeStyle);

    Rectangle bounds = bounds();
    double w = bounds.getWidth();
    double h = bounds.getHeight();
    double x = bounds.getCenterX();
    double y = bounds.getCenterY();
    double rMax = Math.min(w, h) / 2;

    if (hoursRect == null) {
      hoursRect = hoursRect(x, y, rMax);
    }

    drawBackground(g, x, y, rMax);

    g.setFont(font);

    if (hoursTransform == null) {
      calculateHoursTransform(g, "00000000");
    }

    long now = System.currentTimeMillis();
    long upTotalMillis = now - startTime;
    long upTotalHours = upTotalMillis / millisPerHour;
    long upTotalMinutes = upTotalMillis / millisPerMinute;
    long upMinutes = upTotalMinutes % 60;
    long upTotalSeconds = upTotalMillis / millisPerSecond;
    long upSeconds = upTotalSeconds % 60;
    // long upMillis = upTotalMillis % 1000;


    String s = String.format("%08d", upTotalHours);
    g.save();
    g.setColor(Color.DARK_GRAY);
    g.transform(hoursTransform);
    g.drawString(s, 0, 0);
    g.restore();

    double hourProportion = (upMinutes + upSeconds / 60.0) / 60.0;
    // double hourProportion = (upSeconds + upMillis/1000.0) / 60.0;
    double handAngle = 2 * Math.PI * hourProportion;
    // System.err.format("hourProportion = %f, handAngle = PI * %f\n", hourProportion, handAngle /
    // Math.PI);

    g.save();
    g.translate(x, y);
    g.rotate(handAngle);
    g.setColor(Color.LIGHT_GRAY);
    g.fill(handPath);
    g.restore();
  }

}
