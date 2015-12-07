/**
 * 
 */
package com.brunschen.christian.smil;

class Range {
  public int min;
  public int max;

  public Range(int a, int b) {
    min = a;
    max = b;
  }

  public int constrain(int i) {
    return i < min ? min : i > max ? max : i;
  }
}