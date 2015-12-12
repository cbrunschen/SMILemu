/*
 * Register
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.brunschen.christian.graphic.ValueUpdatedListener;

public class Register {

  protected long bits;
  protected int nLowBits, nValueBits, nHighBits;

  protected int[] shifts;
  protected long[] masks;
  protected int lowBitsShift;
  public long lowBitsMask;
  public int valueBitsShift;
  public long valueBitsMask;
  public int highBitsShift;
  public long highBitsMask;
  public long mask;

  protected String name;
  protected boolean notifyListeners = true;
  protected Map<Integer, List<ValueUpdatedListener<Integer>>> listenersByBit = new HashMap<Integer, List<ValueUpdatedListener<Integer>>>(
      100);
  protected List<ActionListener> listeners = new ArrayList<ActionListener>(10);
  protected Map<String, Integer> bitsByName = new HashMap<String, Integer>();
  protected List<String> bitNames = null;
  protected Map<Integer, String> namesByBit = null;

  public static long mask(int nBits, int shift) {
    return nBits == 0 ? 0l : (~0L >>> (Long.SIZE - nBits)) << shift;
  }

  public Register(String name, BitOrder bitOrder, int nLowBits, int nValueBits, int nHighBits) {
    setName(name);
    this.nLowBits = nLowBits;
    this.nValueBits = nValueBits;
    this.nHighBits = nHighBits;

    if (bitOrder == BitOrder.LITTLE_ENDIAN) {
      lowBitsShift = 0;
      lowBitsMask = mask(nLowBits, lowBitsShift);

      valueBitsShift = nLowBits;
      valueBitsMask = mask(nValueBits, valueBitsShift);

      highBitsShift = nLowBits + nValueBits;
      highBitsMask = mask(nHighBits, highBitsShift);

      int shift = 0;
      long mask = 1L;
      shifts = new int[nLowBits + nValueBits + nHighBits];
      masks = new long[nLowBits + nValueBits + nHighBits];
      for (int i = -nLowBits; i < nValueBits + nHighBits; i++) {
        shifts[i + nLowBits] = shift;
        shift++;
        masks[i + nLowBits] = mask;
        mask <<= 1;
      }
    } else {
      lowBitsShift = nHighBits + nValueBits;
      lowBitsMask = mask(nLowBits, lowBitsShift);

      valueBitsShift = nHighBits;
      valueBitsMask = mask(nValueBits, valueBitsShift);

      highBitsShift = 0;
      highBitsMask = mask(nHighBits, highBitsShift);

      int shift = nLowBits + nValueBits + nHighBits - 1;
      long mask = 1L << shift;
      shifts = new int[nLowBits + nValueBits + nHighBits];
      masks = new long[nLowBits + nValueBits + nHighBits];
      for (int i = -nLowBits; i < nValueBits + nHighBits; i++) {
        shifts[i + nLowBits] = shift;
        shift--;
        masks[i + nLowBits] = mask;
        mask >>>= 1;
      }
    }

    mask = lowBitsMask | valueBitsMask | highBitsMask;

    setBitNames();
  }

  public int startBit() {
    return -nLowBits;
  }

  public int endBit() {
    return nValueBits + nHighBits;
  }

  public long maskForBit(int i) {
    return masks[i + nLowBits];
  }

  public int shiftForBit(int i) {
    return shifts[i + nLowBits];
  }

  public void setBit(int bit) {
    setBits(bits() | maskForBit(bit));
  }

  public void clearBit(int bit) {
    setBits(bits() & ~maskForBit(bit));
  }

  public void setBit(int bit, int bitValue) {
    if (bitValue == 1) {
      setBit(bit);
    } else {
      clearBit(bit);
    }
  }

  public void setBit(int bit, boolean bitIsSet) {
    if (bitIsSet) {
      setBit(bit);
    } else {
      clearBit(bit);
    }
  }

  public boolean bitIsSet(int bit) {
    return (bits & maskForBit(bit)) != 0L;
  }

  public int bit(int bit) {
    return bitIsSet(bit) ? 1 : 0;
  }

  public long valueBits() {
    return bits & valueBitsMask;
  }

  public long value() {
    return valueBits() >>> valueBitsShift;
  }

  public void setValueBits(long newMainBits) {
    setBits((bits() & ~valueBitsMask) | (newMainBits & valueBitsMask));
  }

  public void setValue(long newValue) {
    setValueBits(newValue << valueBitsShift);
  }

  public long bits() {
    return bits & mask;
  }

  public long lowBits() {
    return bits & lowBitsMask;
  }

  public long low() {
    return lowBits() >>> lowBitsShift;
  }

  public void setLowBits(long newLowBits) {
    setBits((bits() & ~lowBitsMask) | (newLowBits & lowBitsMask));
  }

  public void setLow(long low) {
    setLowBits(low << lowBitsShift);
  }

  public long highBits() {
    return bits & highBitsMask;
  }

  public long high() {
    return highBits() >>> highBitsShift;
  }

  public void setHighBits(long newHighBits) {
    setBits((bits() & ~highBitsMask) | (newHighBits & highBitsMask));
  }

  public void setHigh(long high) {
    setHighBits(high << highBitsShift);
  }

  public long bits(int startBit, int nBits) {
    long m = ~0L >>> (Long.SIZE - nBits);
    int shift = Math.min(shiftForBit(startBit), shiftForBit(startBit + nBits - 1));
    return (bits() & (m << shift)) >>> shift;
  }

  public void setBits(int startBit, int nBits, long newBits) {
    int shift = Math.min(shiftForBit(startBit), shiftForBit(startBit + nBits - 1));
    long m = (~0L >>> (Long.SIZE - nBits)) << shift;
    setBits((bits() & ~m) | ((newBits << shift) & m));
  }

  public void clear() {
    setBits(0L);
  }

  public void shiftLeft() {
    setBits(bits() << 1);
  }

  public void copy(Register r) {
    setLow(r.low());
    setValue(r.value());
    setHigh(r.high());
  }

  public void toggleBit(int i) {
    if (isBitSet(i)) {
      clearBit(i);
    } else {
      setBit(i);
    }
  }

  public boolean isBitSet(int i) {
    return (bits() & maskForBit(i)) != 0;
  }

  public long bitsComplemented() {
    return (~bits() + 1) & mask;
  }

  public long valueComplemented() {
    return ((~valueBits() + 1) & valueBitsMask) >>> valueBitsShift;
  }

  public void complementBits() {
    setBits(bitsComplemented());
  }

  public void increment() {
    setBits(bits() + (1L << valueBitsShift));
  }

  public void decrement() {
    setBits(bits() - (1L << valueBitsShift));
  }

  protected void setBitNames() {
    for (int i = 0; i < nValueBits; i++) {
      this.addNameForBit(String.format("%d", i), i);
    }
  }

  public String name() {
    return name;
  }

  public void setName(String newName) {
    name = newName;
  }

  public synchronized void setBits(long newBits) {
    long oldBits = bits;

    bits = newBits & mask;

    if (!notifyListeners || bits == oldBits) {
      return;
    }
    for (ActionListener al : listeners) {
      al.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "changed"));
    }
    long changed = newBits ^ oldBits;
    for (int i = startBit(); i < endBit(); i++) {
      int shift = shiftForBit(i);
      long mask = maskForBit(i);
      if ((changed & mask) != 0) {
        // System.err.format("%s: bit %d changed, ", name(), i);
        int oldValue = (int) ((oldBits & mask) >>> shift);
        int newValue = (int) ((newBits & mask) >>> shift);
        List<ValueUpdatedListener<Integer>> listeners = listenersByBit.get(i);
        if (listeners != null) {
          for (ValueUpdatedListener<Integer> listener : listeners) {
            listener.valueUpdated(oldValue, newValue);
          }
        }
      }
    }
  }

  /**
   * @return the bitsByName
   */
  public Map<String, Integer> bitsByName() {
    return bitsByName;
  }

  /**
   * @return the bitNames
   */
  public List<String> bitNames() {
    if (bitNames == null) {
      // generate a list of all the named bits, sorted in ascending bit number
      ArrayList<Map.Entry<String, Integer>> namedBits = new ArrayList<Map.Entry<String, Integer>>(bitsByName.entrySet());
      Collections.sort(namedBits, new Comparator<Map.Entry<String, Integer>>() {
        public int compare(Entry<String, Integer> a, Entry<String, Integer> b) {
          return a.getValue() - b.getValue();
        }
      });

      // create us a list of just the names, in that same order
      bitNames = new ArrayList<String>(namedBits.size());
      for (Map.Entry<String, Integer> namedBit : namedBits) {
        bitNames.add(namedBit.getKey());
      }
    }

    return bitNames;
  }

  public void addNameForBit(String name, int bit) {
    bitNames = null;
    namesByBit = null;
    bitsByName.put(name, bit);
  }

  public Integer bitWithName(String name) {
    return bitsByName.get(name);
  }

  /**
   * @return the namesByBit
   */
  private Map<Integer, String> namesByBit() {
    if (namesByBit == null) {
      namesByBit = new HashMap<Integer, String>();
      for (Map.Entry<String, Integer> entry : bitsByName().entrySet()) {
        namesByBit.put(entry.getValue(), entry.getKey());
      }
    }
    return namesByBit;
  }

  public String nameForBit(int i) {
    return namesByBit().get(i);
  }

  public synchronized void addValueUpdatedListenerForBit(ValueUpdatedListener<Integer> listener, int bit) {
    List<ValueUpdatedListener<Integer>> list = listenersByBit.get(bit);
    if (list == null) {
      list = new LinkedList<ValueUpdatedListener<Integer>>();
      listenersByBit.put(bit, list);
    }
    list.add(listener);
  }

  public synchronized void removeValueUpdatedListenerForBit(ValueUpdatedListener<Integer> listener, int bit) {
    List<ValueUpdatedListener<Integer>> list = listenersByBit.get(bit);
    if (list != null) {
      list.remove(listener);
    }
  }

  public synchronized void addListener(ActionListener listener) {
    listeners.add(listener);
  }

  public synchronized void removeListener(ActionListener listener) {
    listeners.remove(listener);
  }

  public static enum BitOrder {
    BIG_ENDIAN {
      public int shiftForBit(int bit, int lowBits, int valueBits, int highBits) {
        if (-lowBits <= bit && bit < (valueBits + highBits)) {
          return valueBits + highBits - bit;
        }
        throw new IllegalArgumentException();
      }
    },
    LITTLE_ENDIAN {
      public int shiftForBit(int bit, int lowBits, int valueBits, int highBits) {
        if (-lowBits <= bit && bit < (valueBits + highBits)) {
          return lowBits + bit;
        }
        throw new IllegalArgumentException();
      }
    };
    public abstract int shiftForBit(int bit, int lowBits, int valueBits, int highBits);

    public long maskForBit(int bit, int lowBits, int valueBits, int highBits) {
      return 1L << shiftForBit(bit, lowBits, valueBits, highBits);
    }
  }
  
}
