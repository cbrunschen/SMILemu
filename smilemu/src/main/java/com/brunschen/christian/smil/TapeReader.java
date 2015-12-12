/*
 * TapeReader
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

import java.io.IOException;

import com.brunschen.christian.graphic.Font;
import com.brunschen.christian.graphic.Size;
import com.brunschen.christian.smil.Clock.UnitTick;
import com.brunschen.christian.smil.graphic.TapeReaderGraphic;

public class TapeReader {

  public static class NoTapeInReaderException extends IOException {
    public static final long serialVersionUID = 0L;
    public NoTapeInReaderException() {
      super("No Tape in Reader");
    }
  }

  public static class ReadPastEndOfTapeException extends IOException {
    public static final long serialVersionUID = 0L;
    public ReadPastEndOfTapeException() {
      super("Read Past End of Tape");
    }
  }

  private Clock<UnitTick> tickClock;
  private long ticksPerRow;
  private Tape tape;
  private int location;
  private int headPosition;
  private TapeReaderGraphic graphic;
  private Font commentFont;

  public TapeReader(Clock<UnitTick> tickClock, long ticksPerSecond, Font commentFont) {
    super();
    this.tickClock = tickClock;
    this.ticksPerRow = ticksPerSecond / 200; // tqpe reader can read 200 rows per second.
    this.commentFont = commentFont;
  }

  public int read() throws IOException {
    if (tape == null) {
      throw new NoTapeInReaderException();
    }
    Tape.Entry entry;
    do {
      if (location >= tape.length()) {
        throw new ReadPastEndOfTapeException();
      }
      entry = tape.get(location++);
      int width = entry == null ? 1 : entry.width();
      for (int i = 0; i < width; i++) {
        tickClock.sleep(ticksPerRow);
        ++headPosition;
        // repaint();
      }
    } while (entry == null || !entry.hasValue());
    repaint();
    return entry.value();
  }

  public long readWord() throws IOException {
    long value = 0L;
    for (int i = 0; i < 10; i++) {
      value = value << 4 | read() & 0xf;
    }
    return value;
  }

  public void repaint() {
    if (graphic != null) {
      graphic.scrollRectToVisible(graphic.headRect());
      graphic.repaint();
    }
  }

  public void setTape(Tape tape) {
    this.tape = tape;
    location = 0;
    headPosition = 0;
    if (graphic != null) {
      graphic.adjustBounds(tape);
      graphic.scrollRectToVisible(graphic.headRect());
      graphic.repaint();
    }
  }

  public Tape tape() {
    return tape;
  }

  public int location() {
    return location;
  }

  public int headPosition() {
    return headPosition;
  }

  public TapeReaderGraphic graphic() {
    if (graphic == null) {
      graphic = new TapeReaderGraphic(new Size(350.0f, 70.0f), commentFont, this);
    }
    return graphic;
  }

  public void scrollToHead() {
    if (graphic != null) {
      graphic.scrollRectToVisible(graphic.headRect());
    }
  }
}
