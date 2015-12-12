/*
 * Switch
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import com.brunschen.christian.graphic.AbstractGraphic;
import com.brunschen.christian.graphic.AffineTransform;
import com.brunschen.christian.graphic.Color;
import com.brunschen.christian.graphic.Path;
import com.brunschen.christian.graphic.Point;
import com.brunschen.christian.graphic.Size;
import com.brunschen.christian.graphic.Surface;

public class Switch extends AbstractGraphic {

  private class RepeatThread extends Thread {
    @Override
    public void run() {
      while (true) {
        try {
          Thread.sleep(1000);
          for (Listener listener : listeners) {
            if (listener != null) {
              listener.onSwitched(Switch.this);
            }
          }
        } catch (InterruptedException e) {
          break;
        }
      }
    }
  }

  public enum State {
    UP, NEUTRAL, DOWN
  }

  protected State state;
  protected boolean permanent = false;
  protected boolean repeat = false;
  protected Collection<Listener> listeners;
  protected RepeatThread repeatThread = null;

  protected static Path HEAD_FLIPPED;
  static {
    HEAD_FLIPPED = new Path();
    HEAD_FLIPPED.addArc(-4, 0, 8, 7, 10, 160);
    HEAD_FLIPPED.addArc(-5, 5, 10, 9, 170, 200);
    HEAD_FLIPPED.closePath();
  }

  protected static Path STEM;
  static {
    STEM = new Path();
    STEM.addArc(-1, -1.8, 2, 1.6, 0, 180);
    STEM.lineTo(1, 5);
    STEM.lineTo(-1, 5);
    STEM.closePath();
  }

  protected static Path HEAD_CENTER;
  static {
    HEAD_CENTER = new Path();
    HEAD_CENTER.addArc(-5, -5, 10, 10, 0, 180);
    HEAD_CENTER.addArc(-5, -5, 10, 10, 180, 180);
    HEAD_CENTER.closePath();
  }
  
  protected static Path SLOT;
  static {
    SLOT = new Path();
    SLOT.moveTo(-1, -10);
    SLOT.lineTo(1, -10);
    SLOT.lineTo(1, 10);
    SLOT.lineTo(-1, 10);
    SLOT.closePath();
  }


  protected Path headFlipped;
  protected Path headCenter;
  protected Path stem;
  protected Path slot;

  public Switch(Size size, State state, boolean permanent, Listener... listeners) {
    super(size);
    this.state = state;
    this.permanent = permanent;
    this.listeners = new ArrayList<Listener>(Arrays.asList(listeners));

    double scale = Math.min(size.getWidth() / 10.0, size.getHeight() / 30.0);
    AffineTransform at = AffineTransform.makeScale(scale, scale);
    headFlipped = HEAD_FLIPPED.transformed(at);
    headCenter = HEAD_CENTER.transformed(at);
    stem = STEM.transformed(at);
    slot = SLOT.transformed(at);
  }

  public Switch(Size size, boolean permanent, Listener... listeners) {
    this(size, State.NEUTRAL, permanent, listeners);
  }

  public Switch(Size size, Listener... listeners) {
    this(size, State.NEUTRAL, true, listeners);
  }

  public void setRepeat(boolean repeat) {
    this.repeat = repeat;
  }
  
  public State getState() {
    return state;
  }

  @Override
  public void draw(Surface g) {
    g.save();

    g.translate(bounds.getCenterX(), bounds.getCenterY());

    g.setColor(Color.LIGHT_GRAY);
    g.fill(slot);
    g.setColor(Color.BLACK);
    switch (state) {
      case NEUTRAL:
        g.fill(headCenter);
        break;
      case UP:
        g.scale(1.0, -1.0);
        // fall-through
      case DOWN:
        g.fill(stem);
        g.fill(headFlipped);
        break;
    }
    g.restore();
  }

  public void addListeners(Listener... listenersToAdd) {
    for (Listener listener : listenersToAdd) {
      listeners.add(listener);
    }
  }

  public void addListener(Listener listener) {
    addListeners(listener);
  }

  public void removeListeners(Listener... listenersToRemove) {
    for (Listener listener : listenersToRemove) {
      listeners.remove(listener);
    }
  }

  public void removeListener(Listener listener) {
    removeListeners(listener);
  }

  public State stateForPoint(Point p) {
    double dy = (p.getY() - bounds.getY()) / bounds.getHeight();
    if (dy < 0.35) {
      return State.UP;
    } else if (dy <= 0.65) {
      return State.NEUTRAL;
    } else {
      return State.DOWN;
    }
  }

  @Override
  public boolean mouseDown(Point p) {
    State oldState = state;
    state = stateForPoint(p);
    repaint();
    if (repeat && !permanent && repeatThread == null) {
      repeatThread = new RepeatThread();
      repeatThread.start();
    }
    if (oldState != state) {
      for (Listener listener : listeners) {
        if (listener != null) {
          listener.onSwitched(this);
        }
      }
    }
    return true;
  }

  @Override
  public boolean mouseDragged(Point p) {
    return mouseDown(p);
  }

  @Override
  public boolean mouseUp(Point p) {
    State oldState = state;
    if (!permanent) {
      state = State.NEUTRAL;
    }
    if (repeatThread != null) {
      repeatThread.interrupt();
      while (repeatThread != null) {
        try {
          repeatThread.join();
          repeatThread = null;
        } catch (InterruptedException e) {
        }
      }
    }
    if (state != oldState) {
      repaint();
      for (Listener listener : listeners) {
        if (listener != null) {
          listener.onSwitched(this);
        }
      }
    }
    return true;
  }

  @Override
  public boolean mouseExited(Point p) {
    return mouseUp(p);
  }

  public interface Listener {
    public void onSwitched(Switch switch_);
  }
}
