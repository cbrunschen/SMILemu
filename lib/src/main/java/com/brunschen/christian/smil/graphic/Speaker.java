/*
 * Speaker
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
import com.brunschen.christian.graphic.Color;
import com.brunschen.christian.graphic.Ellipse;
import com.brunschen.christian.graphic.Rectangle;
import com.brunschen.christian.graphic.Size;
import com.brunschen.christian.graphic.Surface;

public class Speaker extends AbstractGraphic {

  public static int n = 10;
  
  private Ellipse shape;
  private Rectangle[] rectangles;

  public Speaker(Size size) {
    super(size);
    shape = new Ellipse(bounds);
    rectangles = new Rectangle[n];
    int widths = 2 * n - 1;
    double oneSlot = bounds().getWidth() / widths;
    for (int i = 0; i < n; i++) {
      double x = 2 * i * oneSlot;
      rectangles[i] = new Rectangle(x, 0, oneSlot, bounds().getHeight());
    }
  }

  @Override
  public void draw(Surface g) {
    g.clip(shape);
    g.setColor(Color.BLACK);
    for (int i = 0; i < n; i++) {
      g.fill(rectangles[i]);
    }
  }
}
