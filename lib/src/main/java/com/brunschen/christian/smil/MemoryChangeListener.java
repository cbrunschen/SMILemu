/**
 * 
 */
package com.brunschen.christian.smil;

/**
 * @author Christian Brunschen
 *
 */
interface MemoryChangeListener {
  void memoryChanged(Memory memory, int address, int length);
  void memoryChanged(Memory memory, int address);
}
