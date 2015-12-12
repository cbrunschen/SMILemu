/*
 * TapeReaderGraphic
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

import java.util.List;

import com.brunschen.christian.graphic.AbstractGraphic;
import com.brunschen.christian.graphic.Color;
import com.brunschen.christian.graphic.Ellipse;
import com.brunschen.christian.graphic.Font;
import com.brunschen.christian.graphic.Path;
import com.brunschen.christian.graphic.Rectangle;
import com.brunschen.christian.graphic.Size;
import com.brunschen.christian.graphic.StrokeStyle;
import com.brunschen.christian.graphic.Surface;
import com.brunschen.christian.smil.SMIL;
import com.brunschen.christian.smil.Tape;
import com.brunschen.christian.smil.TapeReader;

public class TapeReaderGraphic extends AbstractGraphic {

  private TapeReader tapeReader;
  private Tape tapeInvestigated = null;
  private int totalWidth = 0;
  private Font font;
  
  private static int TextLines = 5;
  private static double[] holeOffsets = new double[] { 4.7, 3.7, 2.7, 1.3, 0.3, };
  private static double holeDiameter = 0.8;
  private static double holeRadius = holeDiameter / 2.0;
  private static double holeInset = 0.5 - holeRadius;
  private static double tractorHoleDiameter = 0.2;
  private static double tractorHoleRadius = tractorHoleDiameter / 2.0;
  private static double tractorHolePosition = 2.5;
  private static double tractorHoleOffset = tractorHolePosition - tractorHoleRadius;
  private static double tractorHoleInset = 1.0 - tractorHoleRadius;
  private static double startEndExtraSpace = 0.5f;
  private static double tapeHeight = 6.0;

  public TapeReaderGraphic(Size size, Font font, TapeReader tapeReader) {
    super(size);
    this.tapeReader = tapeReader;
    this.font = font;
    adjustBounds(tapeReader.tape());
  }

  public void drawTraction(Surface g, int n) {
    g.setColor(Color.BLACK);
    for (int i = 0; i < n; i++) {
      g.fill(new Ellipse(tractorHoleInset + i, tractorHoleOffset, tractorHoleDiameter, tractorHoleDiameter));
    }
  }

  public void drawValue(Surface g, int value) {
    g.setColor(Color.BLACK);
    g.fill(new Ellipse(tractorHoleInset, tractorHoleOffset, tractorHoleDiameter, tractorHoleDiameter));
    if (value != (byte) 0xff) {
      if (SMIL.bitIsSet(value, 3)) {
        g.fill(new Ellipse(holeInset, holeInset + holeOffsets[4], holeDiameter, holeDiameter));
      }
      if (SMIL.bitIsSet(value, 2)) {
        g.fill(new Ellipse(holeInset, holeInset + holeOffsets[3], holeDiameter, holeDiameter));
      }
      if (SMIL.bitIsSet(value, 1)) {
        g.fill(new Ellipse(holeInset, holeInset + holeOffsets[2], holeDiameter, holeDiameter));
      }
      if (SMIL.bitIsSet(value, 0)) {
        g.fill(new Ellipse(holeInset, holeInset + holeOffsets[1], holeDiameter, holeDiameter));
      }
      g.fill(new Ellipse(holeInset, holeInset + holeOffsets[0], holeDiameter, holeDiameter));
    }
  }

  public void drawText(Surface g, List<String> text) {
    g.setFont(font);
    for (int i = 0; i < text.size() && i < TextLines; i++) {
      g.save();
      String s = text.get(i);
      Rectangle r = g.measureString(s);
      double scale = 1.0 / r.getHeight();
      double ascent = scale * -r.getMinY();
      double offset = holeOffsets[holeOffsets.length - i - 1] + ascent;
      g.translate(0.0, offset);
      g.scale(scale, scale);
      g.drawString(s, 0.0f, 0.0f);
      g.restore();
    }
  }

  public double widthOfText(List<String> text) {
    double width = 0;
    for (int i = 0; i < text.size() && i < TextLines; i++) {
      Rectangle r = font.measureString(text.get(i));
      double scale = 1.0 / r.getHeight();
      width = Math.max(width, scale * r.getWidth());
    }
    return width;
  }

  public void investigateTape(Tape tape) {
    if (tape != tapeInvestigated) {
      if (tape != null) {
        totalWidth = 0;
        tapeInvestigated = tape;
        for (int i = 0; i < tape.length(); i++) {
          Tape.Entry entry = tape.get(i);
          if (entry.hasText()) {
            entry.setWidth((int) Math.ceil(widthOfText(entry.text())));
          }
          totalWidth += entry.width();
        }
      } else {
        tapeInvestigated = null;
        totalWidth = 0;
      }
    }
  }

  @Override
  public void draw(Surface g) {
    g.setFont(font);
    Tape tape = tapeReader.tape();
    g.setColor(com.brunschen.christian.graphic.Color.DARK_GRAY);
    g.fill(bounds());

    g.save();

    double h = bounds().getHeight();
    double holeSize = h / 8.0; // 6 holes across, one extra hole space
    // above and below as margins
    g.scale(holeSize, holeSize);
    g.translate(startEndExtraSpace + tapeHeight / 2 + 1, 1);
    g.setStrokeStyle(new StrokeStyle(1.0 / holeSize));
    if (tape != null) {
      investigateTape(tape);

      Path p = new Path();
      p.moveTo(0, 0);

      p.lineTo((float) (totalWidth + startEndExtraSpace + tapeHeight / 2), 0);
      p.lineTo((float) (totalWidth + startEndExtraSpace), (float) (tapeHeight / 2));
      p.lineTo((float) (totalWidth + startEndExtraSpace + tapeHeight / 2), (float) tapeHeight);

      p.lineTo((float) (0.0 - startEndExtraSpace), (float) tapeHeight);
      p.lineTo((float) (0.0 - startEndExtraSpace - tapeHeight / 2), (float) (tapeHeight / 2));
      p.lineTo((float) (0.0 - startEndExtraSpace), 0);

      p.closePath();

      g.setColor(Color.BLACK);
      g.stroke(p);
      g.setColor(Color.YELLOW);
      g.fill(p);

      g.translate(-3, 0);
      drawTraction(g, 3);
      g.translate(3, 0);
      for (int i = 0; i < tape.length(); i++) {
        Tape.Entry entry = tape.get(i);
        int width = entry.width();
        if (entry.hasValue()) {
          drawValue(g, entry.value());
        } else if (entry.hasText()) {
          drawTraction(g, width);
          drawText(g, entry.text());
        } else {
          drawTraction(g, 1);
        }
        g.translate(width, 0.0);
      }
      drawTraction(g, 1);

      g.translate(-totalWidth + tapeReader.headPosition(), 0);
    }

    g.setColor(Color.BLUE);
    g.stroke(new Path().moveTo(0.0, -1.0).lineTo(0.0, 7.0));
    g.restore();
  }

  public void adjustBounds(Tape tape) {
    double holeSize = bounds().getHeight() / 8.0;
    investigateTape(tape);
    setBounds(new Rectangle(0.0, 0.0, (totalWidth + 2 * startEndExtraSpace + tapeHeight + 2) * holeSize,
        8 * holeSize));
    revalidate();
  }

  public Rectangle headRect() {
    double fraction = 0.5;
    Rectangle visibleRect = visibleRect();
    if (tapeInvestigated != null) {
      fraction = (double) tapeReader.headPosition() / (double) totalWidth;
    }
    double midX = fraction * bounds.getWidth();
    double minX = Math.max(bounds.getX(), midX - visibleRect.getWidth() / 2);
    double maxX = Math.min(midX + visibleRect.getWidth() / 2, bounds.getMaxX());
    return new Rectangle(minX, bounds.getY(), maxX - minX, bounds.getHeight());
  }

}
