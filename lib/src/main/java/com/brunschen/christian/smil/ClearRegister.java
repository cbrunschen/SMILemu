/*
 * ClearRegister
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

package com.brunschen.christian.smil;

import com.brunschen.christian.graphic.Graphic;
import com.brunschen.christian.graphic.PushButton;

public class ClearRegister implements PushButton.Listener {
  protected Graphic graphic;
  protected ValueRegister register;
  protected Boolean right;

  public ClearRegister(Graphic graphic, ValueRegister register, Boolean right) {
    super();
    this.graphic = graphic;
    this.register = register;
    this.right = right;
  }

  public void buttonPushed(PushButton button) {
    if (right == null || right) {
      register.clearRight();
    } else if (right == null || !right) {
      register.clearLeft();
    }
    graphic.repaint();
  }

  public void buttonReleased(PushButton button) {
    // no-op
  }
}
