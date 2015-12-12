/**
 * 
 */
package com.brunschen.christian.smil;

/**
 * @author Christian Brunschen
 *
 */
public class ProgramCounter extends Register {
  public static int RIGHT_BIT = -1;
  
  public ProgramCounter(String name) {
    super(name, Register.BitOrder.LITTLE_ENDIAN, 1, 12, 0);
  }

  /**
   * @see com.brunschen.christian.smil.Register#setBitNames()
   */
  @Override
  protected void setBitNames() {
    super.setBitNames();
    this.addNameForBit("00", RIGHT_BIT);
  }
  
  public boolean right() {
    return bitIsSet(RIGHT_BIT);
  }
  
  public boolean left() {
    return !right();
  }
}
