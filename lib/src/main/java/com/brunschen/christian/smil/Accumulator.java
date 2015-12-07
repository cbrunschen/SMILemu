/**
 * 
 */
package com.brunschen.christian.smil;


/**
 * @author Christian Brunschen
 *
 */
public class Accumulator extends ValueRegister {
  public int EXTRA_BIT = -1;
  public int EXTRA_BIT_SHIFT;
  public long EXTRA_BIT_MASK;
  public int LOW_BIT = SMIL.WORD_BITS;

  public Accumulator(String name) {
    super(name, 1, 1);
    EXTRA_BIT_SHIFT = shiftForBit(EXTRA_BIT);
    EXTRA_BIT_MASK = maskForBit(EXTRA_BIT);
    this.addNameForBit("00", EXTRA_BIT);
    this.addNameForBit("40", LOW_BIT);
  }

  @Override
  public void setValue(long v) {
    long bits = (v << valueBitsShift) & valueBitsMask;
    if ((bits & SIGN_BIT_MASK) != 0) {
      bits |= EXTRA_BIT_MASK;
    }
    setBits(bits);
  }
  
  public synchronized void setLowBit(int newLowBit) {
    setBit(LOW_BIT, newLowBit);
  }

  public synchronized int lowBit() {
    return bit(LOW_BIT);
  }
  
  public boolean isExtraBitSet() {
    return isBitSet(EXTRA_BIT);
  }

  public int extraBit() {
    return bit(EXTRA_BIT);
  }
  
  public void setExtraBit(boolean newExtraBit) {
    setBit(EXTRA_BIT, newExtraBit);
  }

  public synchronized boolean isLowBitSet() {
    return isBitSet(LOW_BIT);
  }
  
  public boolean overflow() {
    return isExtraBitSet() != isSignBitSet();
  }
  
  public void add(ValueRegister r, boolean complement) {
    long rBits = r.valueBits() << valueBitsShift;
    if ((rBits & SIGN_BIT_MASK) != 0) {
      rBits |= EXTRA_BIT_MASK;
    }
    if (complement) {
      rBits = (~rBits + 1) & (valueBitsMask | EXTRA_BIT_MASK);
    }
    add(rBits);
  }
  
  public void add(ValueRegister r) {
    add(r, false);
  }

  public void subtract(ValueRegister r) {
    add(r, true);
  }

  public void add(long otherBits) { 
    long newBits = bits();
    newBits += otherBits;
    newBits &= (valueBitsMask | EXTRA_BIT_MASK);
    setBits(newBits);
  }

  public void add(ValueRegister r, boolean minus, boolean absolute) {
    add(r, shouldComplement(r.isSignBitSet(), minus, absolute));
  }

  @Override
  public void shiftRight(boolean arithmetic) {
    setLowBit(bit(LEAST_BIT));
    long signExtraBitMask = (bits() & EXTRA_BIT_MASK)
        | ((arithmetic && isSignBitSet()) ? ((mask << shiftForBit(1)) & mask) : 0L); // maintain
    setBits(valueBits() >>> 1 | signExtraBitMask);
  }

  @Override
  public void clear() {
    super.clear();
    setLowBit(0);
  }
  
  @Override
  public void clearRight() {
    super.clearRight();
    setLowBit(0);
  }

  @Override
  public String toString() {
    return String.format("%s: %23.20f = %010x (= %01x.%010x.%01x)",
        name(), doubleValue(), value(), low(), bits(), high());
  }

}
