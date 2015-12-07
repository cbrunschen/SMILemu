/*
 * Pulse
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
import com.brunschen.christian.graphic.Path;
import com.brunschen.christian.graphic.Size;
import com.brunschen.christian.graphic.StrokeStyle;
import com.brunschen.christian.graphic.Surface;

public class Pulse extends AbstractGraphic {
  
  public static Path SHAPE = new Path();
  static {
    SHAPE.moveTo(0, 0.9f);
    SHAPE.lineTo(1.0f / 3.0f, 0.9f);
    SHAPE.lineTo(3.0f / 5.0f, 0.1f);
    SHAPE.lineTo(2.0f / 3.0f, 0.9f);
    SHAPE.lineTo(1.0f, 0.9f);
  }
  
  private Path path;
  private StrokeStyle strokeStyle;

  public Pulse(Size size) {
    super(size);
    AffineTransform at = AffineTransform.makeTranslate(bounds.getCenterX(), bounds.getCenterY());
    at.scale(bounds.getWidth(), bounds.getHeight());
    at.translate(-0.5f, -0.5f);

    path = SHAPE.transformed(at);
    strokeStyle = new StrokeStyle(1.0f);
  }

  @Override
  public void draw(Surface g) {
    g.save();
    g.clip(bounds());
    g.setColor(Color.BLACK);
    g.setStrokeStyle(strokeStyle);
    g.stroke(path);
    g.restore();
  }

}
