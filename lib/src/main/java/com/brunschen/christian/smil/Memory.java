/**
 * 
 */
package com.brunschen.christian.smil;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Christian Brunschen
 *
 */
public class Memory {
  private long[] contents;
  private long wordMask;
  private Set<MemoryChangeListener> changeListeners = new HashSet<MemoryChangeListener>();
  
  public Memory(int length, int significantBits) {
    contents = new long[length];
    wordMask = ~0L >>> (Long.SIZE - significantBits);
  }

  public int length() {
    return contents.length;
  }
  
  public long get(int address) {
    return contents[address % contents.length];
  }
  
  public long read(int address) {
    return get(address);
  }
  
  private void notifyChange(int address) {
    for (MemoryChangeListener changeListener : changeListeners) {
      changeListener.memoryChanged(this, address);
    }
  }
  
  private void notifyChange(int address, int length) {
    if (length <= 0) {
      return;
    } else if (length == 1) {
      notifyChange(address);
    } else {
      for (MemoryChangeListener changeListener : changeListeners) {
        changeListener.memoryChanged(this, address, length);
      }
    }
  }
  
  public void set(int address, long[] values, int offset, int length) {
    if (length > contents.length) {
      /*
       * If we try to write more than there is space for in total, some of the initial data will be
       * overwritten by later data, so doesn't need to be written in the first place.
       * Calculate the amount of this initial redundant data, and adjust the address, offset
       * and length accordingly.
       */
      int overwritten = length - contents.length;
      offset += overwritten;
      length -= overwritten;
      address = (address + overwritten) % contents.length;
    }
    
    if (address + length <= contents.length) {
      // everything fits between here and the end of memory, no problem.
      System.arraycopy(values, offset, contents, address, length);
      notifyChange(address, length);
    } else {
      // break the data into two blocks, at the end and the beginning of memory. Write each
      // block and notify separately.
      int n = contents.length - (address + length);
      System.arraycopy(values, offset, contents, address, n);
      System.arraycopy(values, offset + n, contents, 0, length - n);
      notifyChange(address, n);
      notifyChange(0, length - n);
    }
  }
  
  public void set(int address, long value) {
    address %= contents.length;
    contents[address] = value & wordMask;
    notifyChange(address);
  }
  
  public void set(int address, long value, long mask) {
    set(address, get(address) & ~mask | value & mask);
  }
  
  public void write(int address, long value) {
    set(address, value);
  }
  
  public void write(int address, long value, long mask) {
    set(address, value, mask);
  }
  
  public void write(int address, long[] values, int offset, int len) {
    set(address, values, offset, len);
  }
  
  public void clear() {
    for (int i = 0; i < contents.length; i++) {
      contents[i] = 0L;
    }
    notifyChange(0, contents.length);
  }
  
  public void addChangeListener(MemoryChangeListener changeListener) {
    changeListeners.add(changeListener);
  }
  
  public void removeChangeListener(MemoryChangeListener changeListener) {
    changeListeners.remove(changeListener);
  }
}
