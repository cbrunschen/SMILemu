/**
 * 
 */
package com.brunschen.christian.smil;


/**
 * @author Christian Brunschen
 *
 */
public class BitCounter extends Register {

  /**
   * @param name
   */
  public BitCounter(String name) {
    super(name, BitOrder.LITTLE_ENDIAN, 0, 6, 0);
  }

}
