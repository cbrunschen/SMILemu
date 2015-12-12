package com.brunschen.christian.smil;

/**
 * @author Christian Brunschen
 *
 */
public class ValueRegister extends Register {

  public int SIGN_BIT = 0;
  public int LEAST_BIT = SMIL.WORD_BITS - 1;

  public BitOrder BIT_ORDER = BitOrder.BIG_ENDIAN;

  public int SIGN_BIT_SHIFT = BIT_ORDER.shiftForBit(SIGN_BIT, 0, SMIL.WORD_BITS, 0);
  public long SIGN_BIT_MASK = BIT_ORDER.maskForBit(SIGN_BIT, 0, SMIL.WORD_BITS, 0);

  public int LEAST_BIT_SHIFT = BIT_ORDER.shiftForBit(LEAST_BIT, 0, SMIL.WORD_BITS, 0);
  public long LEAST_BIT_MASK = BIT_ORDER.maskForBit(LEAST_BIT, 0, SMIL.WORD_BITS, 0);

  public long MAX_ABS;

  public ValueRegister(String name, int lowBits, int highBits) {
    super(name, BitOrder.BIG_ENDIAN, lowBits, SMIL.WORD_BITS, highBits);

    SIGN_BIT_SHIFT = shiftForBit(SIGN_BIT);
    SIGN_BIT_MASK = maskForBit(SIGN_BIT);

    LEAST_BIT = SMIL.WORD_BITS - 1;
    LEAST_BIT_SHIFT = shiftForBit(LEAST_BIT);
    LEAST_BIT_MASK = maskForBit(LEAST_BIT);

    MAX_ABS = 1L << (shiftForBit(SIGN_BIT) - shiftForBit(LEAST_BIT));
    setValue(0L);
  }
  
  public ValueRegister(String name) {
    this(name, 0, 0);
  }

  public int left() {
    return (int) ((value() & SMIL.LEFT_MASK) >>> SMIL.HALFWORD_BITS);
  }

  public int right() {
    return (int) (value() & SMIL.RIGHT_MASK);
  }

  public boolean isLeastBitSet() {
    return isBitSet(LEAST_BIT);
  }

  public int leastBit() {
    return bit(LEAST_BIT);
  }

  public boolean isSignBitSet() {
    return isBitSet(SIGN_BIT);
  }

  public int signBit() {
    return bit(SIGN_BIT);
  }

  public void clearLeft() {
    setBits(bits() & (mask >>> nLowBits + nValueBits / 2));
  }

  public void clearRight() {
    setBits(bits() & (mask << nHighBits + nValueBits / 2));
  }

  public double doubleValue() {
    return SMIL.doubleValue(value());
  }

  public void setDoubleValue(double d) {
    setValue(SMIL.word(d));
  }

  @Override
  public String toString() {
    return String.format("%s: %23.20f = %010x", name(), doubleValue(), value(), bits());
  }

  public void shiftRight(boolean arithmetic) {
    long signBitMask = 0L;
    if (arithmetic) {
      signBitMask = (valueBitsMask << (nValueBits - 1)) & valueBitsMask; // maintain sign bit
    }
    setBits((bits() >>> 1) | (isSignBitSet() ? signBitMask : 0L));
  }

  public void shiftRight() {
    shiftRight(false);
  }

  public void shiftRightArithmetic() {
    shiftRight(true);
  }
  
  public static boolean shouldComplement(boolean isNegative, boolean minus, boolean absolute) {
    boolean result = isNegative && absolute != minus || !isNegative && minus;
    return result;
  }
}
