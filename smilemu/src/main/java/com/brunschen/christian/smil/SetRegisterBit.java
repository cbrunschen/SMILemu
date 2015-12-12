/*
 * SetRegisterBit
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

import com.brunschen.christian.graphic.PushButton;

public class SetRegisterBit implements PushButton.Listener {
  protected Register register;
  protected int bit;
  protected boolean set = true;

  public SetRegisterBit(Register register, int bit, boolean set) {
    super();
    this.register = register;
    this.bit = bit;
    this.set = set;
  }

  public SetRegisterBit(Register register, int bit) {
    this(register, bit, true);
  }

  public void buttonPushed(PushButton button) {
    register.setBit(bit, set);
  }

  public void buttonReleased(PushButton button) {
    // no-op
  }
}
