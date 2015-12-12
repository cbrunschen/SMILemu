/*
 * TestRegister
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

import java.util.List;

import com.brunschen.christian.smil.Accumulator;
import com.brunschen.christian.smil.ValueRegister;

import junit.framework.TestCase;

public class TestRegister extends TestCase {

  private Accumulator acc = new Accumulator("ACC");
  private ValueRegister s = new ValueRegister("S");

  public TestRegister(String name) {
    super(name);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testInitialStatus() {
    assertEquals(acc.value(), 0L);
  }

  public void testShiftsAndMasks() {
    assertEquals(0, s.shiftForBit(s.LEAST_BIT));
    assertEquals(39, s.shiftForBit(s.SIGN_BIT));

    assertEquals(0, acc.shiftForBit(acc.LOW_BIT));
    assertEquals(1, acc.shiftForBit(acc.LEAST_BIT));
    assertEquals(40, acc.shiftForBit(acc.SIGN_BIT));
    assertEquals(41, acc.shiftForBit(acc.EXTRA_BIT));

    assertEquals(0x08000000000L, s.maskForBit(s.SIGN_BIT));
    assertEquals(0x00000000001L, s.maskForBit(s.LEAST_BIT));
    
    assertEquals(0x20000000000L, acc.maskForBit(acc.EXTRA_BIT));
    assertEquals(0x20000000000L, acc.EXTRA_BIT_MASK);
    assertEquals(0x10000000000L, acc.maskForBit(acc.SIGN_BIT));
    assertEquals(0x00000000002L, acc.maskForBit(acc.LEAST_BIT));
    assertEquals(0x00000000001L, acc.maskForBit(acc.LOW_BIT));

    for (int i = 0; i < 40; i++) {
      assertEquals(39 - i, s.shiftForBit(i));
      assertEquals(40 - i, acc.shiftForBit(i));
      assertEquals(0x10000000000L >>> i, acc.maskForBit(i));
      assertEquals(0x08000000000L >>> i, s.maskForBit(i));
    }
  }

  public void testShiftLeft() {
    acc.setValue(0x10);
    acc.shiftLeft();
    assertEquals(0x20, acc.value());
    acc.shiftLeft();
    acc.shiftLeft();
    acc.shiftLeft();
    acc.shiftLeft();
    assertEquals(0x200, acc.value());
  }
  
  public void testValueComplemented() {
    long expectedValue = 0x123456789aL;
    acc.setValue(expectedValue);
    assertEquals(expectedValue, acc.value());
    assertEquals(0xedcba98766L, acc.valueComplemented());
    s.setValue(expectedValue);
    assertEquals(0xedcba98766L, s.valueComplemented());
  }

  public void testOverflow() {
    // System.err.format("testOverflow\n");
    acc.setValue(0x8000000000L);
    s.setValue(0x8000000000L);
    acc.add(s);
    assertTrue(acc.overflow());

    acc.setDoubleValue(-1.0);
    s.setDoubleValue(-1.0);
    acc.subtract(s);
    assertEquals(acc.doubleValue(), 0.0);
    assertFalse(acc.overflow());

    acc.setDoubleValue(0.75);
    s.setDoubleValue(0.75);
    acc.add(s);
    assertTrue(acc.overflow());

    acc.setDoubleValue(-0.75);
    s.setDoubleValue(-0.75);
    acc.add(s);
    assertTrue(acc.overflow());

    acc.setDoubleValue(0.75);
    s.setDoubleValue(-0.75);
    acc.subtract(s);
    assertTrue(acc.overflow());

    acc.setDoubleValue(-0.75);
    s.setDoubleValue(0.75);
    acc.subtract(s);
    assertTrue(acc.overflow());

    acc.setValue(0L);
    s.setValue(0x8000000000L);
    acc.subtract(s);
    assertEquals(acc.value(), 0x8000000000L);
    assertTrue(acc.overflow());

    acc.setDoubleValue(0.5);
    s.setDoubleValue(0.5);
    acc.add(s);
    assertEquals(-1.0, acc.doubleValue());
    assertTrue(acc.overflow());

    acc.setDoubleValue(-0.5);
    s.setDoubleValue(-0.5);
    acc.add(s);
    assertEquals(-1.0, acc.doubleValue());
    assertFalse(acc.overflow());

    acc.setDoubleValue(0.5);
    s.setDoubleValue(-0.5);
    acc.subtract(s);
    assertEquals(-1.0, acc.doubleValue());
    assertTrue(acc.overflow());

    acc.setDoubleValue(-0.5);
    s.setDoubleValue(0.5);
    acc.subtract(s);
    assertEquals(-1.0, acc.doubleValue());
    assertFalse(acc.overflow());
    
    acc.setDoubleValue(0.0);
    s.setDoubleValue(0.5);
    acc.add(s);
    assertEquals(0.5, acc.doubleValue());
    assertFalse(acc.overflow());
    
    acc.setDoubleValue(0.0);
    s.setDoubleValue(0.5);
    acc.subtract(s);
    assertEquals(-0.5, acc.doubleValue());
    assertFalse(acc.overflow());
    
    acc.setDoubleValue(0.0);
    s.setDoubleValue(-0.5);
    acc.add(s);
    assertEquals(-0.5, acc.doubleValue());
    assertFalse(acc.overflow());

    acc.setDoubleValue(0.0);
    s.setDoubleValue(-0.5);
    acc.subtract(s);
    assertEquals(0.5, acc.doubleValue());
    assertFalse(acc.overflow());
 }

  public void testShiftRight() {
    acc.setValue(0x1000);
    acc.shiftRight();
    assertEquals(0x0800, acc.value());
    acc.shiftRight();
    acc.shiftRight();
    acc.shiftRight();
    acc.shiftRight();
    assertEquals(0x0080, acc.value());
    acc.setValue(0x8001);
    assertEquals(0x8001, acc.value());
    acc.shiftRight();
    assertEquals(0x4000, acc.value());
    assertTrue(acc.isLowBitSet());
    
    acc.setValue(0x4000000000L);
    acc.shiftRightArithmetic();
    assertEquals(0x2000000000L, acc.value());
    
    acc.setValue(0xc000000000L);
    acc.shiftRightArithmetic();
    assertEquals(0xe000000000L, acc.value());
    
    acc.setValue(0xc500000000L);
    acc.setExtraBit(true);
    acc.shiftRightArithmetic();
    acc.shiftRightArithmetic();
    acc.shiftRightArithmetic();
    acc.shiftRightArithmetic();
    assertEquals(String.format("expected %x, actual %x", 0xfc50000000L, acc.value()),
        0xfc50000000L, acc.value());
    
    acc.setValue(0x4500000000L);
    acc.setExtraBit(true);
    acc.shiftRightArithmetic();
    acc.shiftRightArithmetic();
    acc.shiftRightArithmetic();
    acc.shiftRightArithmetic();
    assertEquals(String.format("expected %x, actual %x", 0x0450000000L, acc.value()),
        0x0450000000L, acc.value());
 }

  public void testShiftRightArithmetic() {
    // System.err.format("testShiftRightArithmetic:\n");
    acc.setValue(0x8000000000L);
    // System.err.format("before: %011x\n", r.primValue());
    acc.shiftRightArithmetic();
    // System.err.format("after one shift of 1 bit: %011x\n", r.primValue());
    assertEquals(acc.value(), 0xc000000000L);
    acc.shiftRightArithmetic();
    acc.shiftRightArithmetic();
    // System.err.format("after further shift of 2 bit: %011x\n", r.primValue());
    assertEquals(acc.value(), 0xf000000000L);
  }

  public void testUnderflow() {
    // System.err.format("testUnderflow\n");
    acc.setValue(1L);
    // System.err.format("primValue = %011x\n", r.primValue());
    acc.shiftRight();
    // System.err.format("primValue = %011x\n", r.primValue());
    // System.err.format("isLowBitSet = %b\n", r.isLowBitSet());
    assertTrue(acc.isLowBitSet());
  }

  public void testDoubleValue() {
    // System.err.format("testDoubleValue\n");

    acc.setValue(0x0000000000L);
    // System.err.format("value %011x == %23.20f\n", r.value(), r.doubleValue());
    assertEquals(acc.doubleValue(), 0.0);

    acc.setValue(0x8000000000L);
    // System.err.format("value %011x == %23.20f\n", r.value(), r.doubleValue());
    assertEquals(acc.doubleValue(), -1.0);

    acc.setValue(0x4000000000L);
    // System.err.format("value %011x == %23.20f\n", r.value(), r.doubleValue());
    assertEquals(acc.doubleValue(), 0.5);

    acc.setValue(0xffffffffffL);
    // System.err.format("value %011x == %23.20f\n", r.value(), r.doubleValue());
    assertEquals(acc.doubleValue(), -1.0 / acc.MAX_ABS);

    acc.setValue((0x8000000000L - 1L));
    // System.err.format("value %011x == %23.20f\n", r.value(), r.doubleValue());
    assertEquals(acc.doubleValue(), 1.0 - 1.0 / acc.MAX_ABS);
  }

  public void testSetDoubleValue() {
    // System.err.format("testSetDoubleValue\n");

    acc.setDoubleValue(0.0);
    // System.err.format("doubleValue %23.20f == %011x\n", r.doubleValue(), r.value());
    assertEquals(acc.value(), 0x0L);

    acc.setDoubleValue(0.5);
    // System.err.format("doubleValue %23.20f == %011x\n", r.doubleValue(), r.value());
    assertEquals(acc.value(), 0x4000000000L);

    acc.setDoubleValue(0.25);
    // System.err.format("doubleValue %23.20f == %011x\n", r.doubleValue(), r.value());
    assertEquals(acc.value(), 0x2000000000L);

    acc.setDoubleValue(-0.5);
    // System.err.format("doubleValue %23.20f == %011x\n", r.doubleValue(), r.value());
    assertEquals(acc.value(), 0xc000000000L);

    acc.setDoubleValue(-0.25);
    // System.err.format("doubleValue %23.20f == %011x\n", r.doubleValue(), r.value());
    assertEquals(acc.value(), 0xe000000000L);

    try {
      acc.setDoubleValue(1.0);
      fail();
    } catch (RuntimeException e) {
    }
    try {
      acc.setDoubleValue(1.0 - 1.0 / acc.MAX_ABS);
    } catch (RuntimeException e) {
      fail();
    }
    try {
      acc.setDoubleValue(2.0);
      fail();
    } catch (RuntimeException e) {
    }
    try {
      acc.setDoubleValue(-1.0);
    } catch (RuntimeException e) {
      fail();
    }
    try {
      acc.setDoubleValue(-2.0);
      fail();
    } catch (RuntimeException e) {
    }
  }

  public void testSetPrimValue() {
    acc.setBits(0xffffffffffffL);
    assertEquals(acc.bits(), acc.mask);
  }

  public void testSetValue() {
    acc.setValue(0xffffffffffL);
    assertEquals(acc.bits(), acc.valueBitsMask | acc.EXTRA_BIT_MASK);
  }

  public void testAdd() {
    ValueRegister s = new ValueRegister("S");

    acc.setValue(1);
    s.setValue(2);
    acc.add(s);
    assertEquals(acc.value(), 3);

    acc.setDoubleValue(0.5);
    s.setDoubleValue(0.25);
    acc.add(s);
    assertEquals(0.75, acc.doubleValue());
  }

  public void testCopy() {
    ValueRegister s = new ValueRegister("S");

    s.clear();
    acc.clear();

    acc.setValue(0x0123456789L);
    s.copy(acc);
    assertEquals(0x0123456789L, s.value());
  }
  
  public void testNames() {
    // create a new register with only the names we want to test
    ValueRegister t = new ValueRegister("T") {
     @Override
     protected void setBitNames() {
       this.addNameForBit("a", 2);
       this.addNameForBit("c", 7);
       this.addNameForBit("b", 4);
      }
    };
        
    List<String> bitNames = t.bitNames();
    
    assertEquals(3, bitNames.size());
    
    assertEquals("a", bitNames.get(0));
    assertEquals("b", bitNames.get(1));
    assertEquals("c", bitNames.get(2));
    
    assertEquals((Integer)2, t.bitWithName("a"));
    assertEquals((Integer)4, t.bitWithName("b"));
    assertEquals((Integer)7, t.bitWithName("c"));
    
    assertEquals(null, t.bitWithName("x"));
  }

  public void testAccumulatorNames() {
    // create a new register with only the names we want to test
    Accumulator t = new Accumulator("ar");
        
    List<String> bitNames = t.bitNames();
    
    assertEquals(42, bitNames.size());
    assertEquals(-1, (int) t.bitWithName("00"));
    for (int i = 0; i <= 40; i++) {
      String name = Integer.toString(i);
      assertEquals(i, (int) t.bitWithName(name));
      assertEquals(name, t.nameForBit(i));
    }
  }

  public void testClearLeft() throws Exception{
    acc.setBits(0x123457689abL);
    acc.clearLeft();
    assertEquals(0x000001689abL, acc.bits());
    
    s.setBits(0x123457689abL);
    s.clearLeft();
    assertEquals(0x000000689abL, s.bits());
  }
  
  public void testClearRight() throws Exception {
    acc.setBits(0x123457689abL);
    acc.clearRight();
    assertEquals(0x12345600000L, acc.bits());
    
    s.setBits(0x123456789abL);
    s.clearRight();
    assertEquals(0x02345600000L, s.bits());
  }
}
