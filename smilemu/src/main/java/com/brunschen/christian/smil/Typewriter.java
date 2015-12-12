/*
 * Typewriter
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

public interface Typewriter {

  public String[] specialChars = new String[] { " ", "\n", ".", "\t", "-", "+", "_", "", "i", };

  public abstract int length();

  public abstract String text();

  public abstract void append(String s);

  public void printHex(int d);

  public void printSpecial(int c);

  public abstract void clear();
  
  public abstract static class Default implements Typewriter {
    public void printHex(int d) {
      append(String.format("%01X", d & 0xF));
    }

    public void printSpecial(int c) {
      append(specialChars[(c & 0xf) % specialChars.length]);
    }
  }
}
