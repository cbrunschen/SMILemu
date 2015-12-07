/*
 * TestSMIL
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

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import com.brunschen.christian.smil.sound.SoundGenerator;

import junit.framework.TestCase;

public class TestSMIL extends TestCase {

  private SMIL smil;

  public TestSMIL(String name) {
    super(name);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    smil = new SMIL();
    
    smil.setTapeReader(new TapeReader(null, 0, null));
    smil.setSoundGenerator(new FakeSoundGenerator());
    smil.setControlPanel(new ControlPanel(null, null));
    smil.setTypewriter(new FakeTypewriter());

    smil.init();
    smil.tickClock().setWillWait(false);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    smil = null;
  }

  public void printRegs(PrintStream ps) {
    ps.format("  %s\n", smil.processor().mr);
    ps.format("  %s\n", smil.processor().ar);
    ps.format("  %s\n", smil.processor().md);
    // ps.format(" ir = %s\n", smil.processor().ir);
    // ps.format(" kr = %s\n", smil.processor().kr);
  }

  public void testMultiply() {
    smil.processor().ar.clear();
    smil.processor().mr.setValue(2);
    smil.processor().md.setValue(3);
    smil.processor().multiply();
    assertEquals(0x0, smil.processor().ar.value());
    assertEquals(0x6, smil.processor().mr.value());
    assertFalse(smil.processor().ar.overflow());

    smil.processor().ar.clear();
    smil.processor().mr.setValue(2 << SMIL.HALFWORD_BITS);
    smil.processor().md.setValue(3 << SMIL.HALFWORD_BITS - 1);
    smil.processor().multiply();
    assertEquals(smil.processor().ar.value(), 0x6);
    assertEquals(smil.processor().mr.value(), 0x0);
    assertFalse(smil.processor().ar.overflow());

    smil.processor().ar.clear();
    smil.processor().mr.setDoubleValue(1.0 / 16);
    smil.processor().md.setValue(0x0123456789L);
    smil.processor().multiply();
    assertEquals(smil.processor().ar.value(), 0x0012345678L);
    assertEquals(smil.processor().mr.value(), 0x9L << 39 - 4);
    assertFalse(smil.processor().ar.overflow());

    smil.processor().ar.clear();
    smil.processor().mr.setDoubleValue(-1.0);
    smil.processor().md.setValue(0x0100000000L);
    smil.processor().multiply();
    assertEquals(smil.processor().ar.value(), 0xff00000000L);
    assertEquals(smil.processor().mr.value(), 0x0L);
    assertFalse(smil.processor().ar.overflow());

    smil.processor().ar.clear();
    smil.processor().mr.setDoubleValue(-1.0);
    smil.processor().md.setValue(0x0000000001L);
    smil.processor().multiply();
    assertEquals(smil.processor().ar.value(), 0xffffffffffL);
    assertEquals(smil.processor().mr.value(), 0x0L);
    assertFalse(smil.processor().ar.overflow());

    smil.processor().ar.clear();
    smil.processor().mr.setDoubleValue(-1.0);
    smil.processor().md.setDoubleValue(-0.5);
    smil.processor().multiply();
    assertEquals(smil.processor().ar.doubleValue(), 0.5);
    assertEquals(smil.processor().mr.value(), 0L);
    assertFalse(smil.processor().ar.overflow());

    smil.processor().ar.clear();
    smil.processor().mr.setDoubleValue(-1.0);
    smil.processor().md.setDoubleValue(0.5);
    smil.processor().multiply();
    assertEquals(smil.processor().ar.doubleValue(), -0.5);
    assertEquals(smil.processor().mr.value(), 0L);
    assertFalse(smil.processor().ar.overflow());

    smil.processor().ar.clear();
    smil.processor().mr.setDoubleValue(-0.5);
    smil.processor().md.setDoubleValue(0.5);
    smil.processor().multiply();
    assertEquals(smil.processor().ar.doubleValue(), -0.25);
    assertEquals(smil.processor().mr.value(), 0L);
    assertFalse(smil.processor().ar.overflow());

    smil.processor().ar.clear();
    smil.processor().mr.setDoubleValue(0.5);
    smil.processor().md.setDoubleValue(-0.5);
    smil.processor().multiply();
    assertEquals(smil.processor().ar.doubleValue(), -0.25);
    assertEquals(smil.processor().mr.value(), 0L);
    assertFalse(smil.processor().ar.overflow());

    smil.processor().ar.clear();
    smil.processor().mr.setDoubleValue(-0.5);
    smil.processor().md.setDoubleValue(-0.5);
    smil.processor().multiply();
    assertEquals(smil.processor().ar.doubleValue(), 0.25);
    assertEquals(smil.processor().mr.value(), 0L);
    assertFalse(smil.processor().ar.overflow());

    smil.processor().ar.clear();
    smil.processor().mr.setDoubleValue(-1.0);
    smil.processor().md.setDoubleValue(-1.0);
    smil.processor().multiply();
    assertEquals(smil.processor().ar.doubleValue(), -1.0);
    assertEquals(smil.processor().mr.value(), 0L);
    assertTrue(smil.processor().ar.overflow());
  }

  public void assertCloseTo(double a, double b, double tolerance) {
    assertTrue(String.format("expected %f to be within %f of %f, but was %f away", b, tolerance, a, b-a),
        Math.abs(a - b) <= Math.abs(tolerance));
  }

  public void assertCloseTo(long a, long b, long tolerance) {
    assertTrue(String.format("expected %x to be within %x of %x, but was %x away", b, tolerance, a, b-a),
        Math.abs(a - b) <= Math.abs(tolerance));
  }

  public void assertCloseTo(double a, double b) {
    assertCloseTo(a, b, (1.0 / 0x800000000L));
  }

  public void assertCloseTo(long a, long b) {
    assertCloseTo(a, b, 0x1);
  }

  double restMultiplier = (double) 1L / (double) (1L << 39);

  public void testDivide(double x, double y, double expected) {
    // System.err.format("%18.15f / %18.15f = ", x, y);
    smil.processor().ar.setDoubleValue(x);
    smil.processor().md.setDoubleValue(y);
    smil.processor().mr.clear();
    smil.processor().divide();
    double rest = smil.processor().ar.doubleValue();
    smil.processor().reverseMrIntoAr();
    double result = smil.processor().ar.doubleValue();
    // System.err.format("%18.15f (rest %18.15f)\n", result, rest);
    assertCloseTo(smil.processor().ar.doubleValue(), expected);
    assertEquals(x, y * result + rest * restMultiplier);
    // System.err.format("y * result = %18.15f\n", y * result);
    // System.err.format("y * result + rest>>39 = %18.15f\n", y * result + rest * restMultiplier );
  }

  public void testDivide(long x, long y, long expected) {
    double floatX = SMIL.doubleValue(x);
    double floatY = SMIL.doubleValue(y);
    // System.err.format("%010x (%18.15f) / %010x (%18.15f) = ", x, floatX, y, floatY);
    smil.processor().ar.setValue(x);
    smil.processor().md.setValue(y);
    smil.processor().mr.clear();
    smil.processor().divide();
    // long rest = smil.processor().ar.value();
    double floatRest = smil.processor().ar.doubleValue();
    smil.processor().reverseMrIntoAr();
    long result = smil.processor().ar.value();
    double floatResult = smil.processor().ar.doubleValue();
    assertCloseTo(result, expected);
    assertEquals(floatX, floatY * floatResult + floatRest * restMultiplier);
    // System.err.format("%010x (%18.15f) (rest %010x (%18.15f))\n", result, floatResult, rest,
    // floatRest);
    // double floatReconstituted = floatY * floatResult + floatRest * restMultiplier;
    // long reconstituted = SMIL.word(floatReconstituted);
    // System.err.format("y * result = %010x (%18.15f)\n", SMIL.word(floatY * floatResult), floatY *
    // floatResult);
    // System.err.format("y * result + rest = %010x (%18.15f)\n", reconstituted,
    // floatReconstituted);
  }

  public void testDivide() {
    testDivide(0.0123456789, -0.0123456789, -1.0);
    testDivide(-0.0123456789, 0.0123456789, -1.0);
    testDivide(-0.0123456789, -0.0123456789, 1.0);
    testDivide(0.0123456789, 0.0123456789, 1.0);
    testDivide(1.0 / 8.0, 1.0 / 4.0, 1.0 / 2.0);
    testDivide(0x3000000000L, 0x4000000000L, 0x6000000000L);
  }

  public void jump(int address, boolean right) throws Exception {
    smil.processor().jump(address, right);
    smil.processor().loadIrIfNecessary();
  }

  public void testInstruction0() throws Exception {
    List<String> lines = Arrays.asList(new String[] { "0123456789ab" });
    smil.setTapeReader(new TapeReader(new FakeClock(), 0, null));
    smil.tapeReader().setTape(new Tape(lines));

    smil.memory().set(0x000, 0x0000000008L);
    smil.memory().set(0x001, 0x0000a00000L);
    jump(0x000, false);

    smil.processor().oneStep();
    assertEquals(smil.memory().get(0x0000), 0x0123456789L);
    assertEquals(smil.processor().ar.value(), 0x0123456789L);

    smil.processor().oneStep();
    assertEquals(smil.processor().ar.value(), 0x012345678aL);

    smil.processor().oneStep();
    assertEquals(smil.processor().ar.value(), 0x000000000bL);
  }
  
  private void testAnd(int instruction, long ar, long mem, long mr, long result) throws Exception {
    smil.reset();
    smil.memory().set(0x000, mem);
    smil.processor().ar.setValue(ar);
    smil.processor().mr.setValue(mr);
    smil.memory().set(0x001, instruction << 20);
    jump(0x001, false);
    smil.processor().oneStep();
    assertEquals(result, smil.processor().ar.value());
  }

  public void testInstruction1() throws Exception {
    testAnd(0x10, 0L, 0x123456789aL, 0x00000fffffL, 0x000006789aL);
    testAnd(0x12, 0xa987654321L, 0x123456789aL, 0x00000fffffL, 0x000006789aL);
    testAnd(0x14, 0L, 0x123456789aL, 0x000fffff00L, 0x0004567800L);
    testAnd(0x18, 0L, 0x123456789aL, 0x00000fffffL, 0x0000098766L);
    testAnd(0x1c, 0L, 0x123456789aL, 0x00000fffffL, 0x0000098766L);

    testAnd(0x10, 0x1030507090L, 0x020406080aL, 0x00000fffffL, 0x000006789aL);
    testAnd(0x12, 0x1030507090L, 0x020406080aL, 0x000fffff00L, 0x0004060800L);
    testAnd(0x14, 0x1030507090L, 0x020406080aL, 0x00000fffffL, 0x000006789aL);
    testAnd(0x18, 0x1030507090L, 0x020406080aL, 0x00000fffffL, 0x00000a6886L);
    testAnd(0x1c, 0x1030507090L, 0x020406080aL, 0x00000fffffL, 0x00000a6886L);
  }

  public void testInstruction2() throws Exception {
    smil.reset();
    smil.processor().mr.setValue(0x123456789aL);
    smil.memory().set(0x000, 0x0002000000L);
    jump(0x000, false);
    smil.processor().oneStep();
    assertEquals(smil.processor().ar.value(), 0x123456789aL);

    smil.reset();
    smil.processor().mr.setValue(0x123456789aL);
    smil.memory().set(0x000, 0x0002800000L);
    jump(0x000, false);
    smil.processor().oneStep();
    assertEquals(smil.processor().ar.value(), 0x591e6a2c48L);
  }

  public void testInstruction3() throws Exception {
    smil.reset();
    smil.processor().ar.setValue(0x123456789aL);
    smil.memory().set(0x000, 0x0013000000);
    jump(0x000, false);
    smil.processor().oneStep();
    assertEquals(smil.memory().get(0x001), 0x123456789aL);

    smil.reset();
    smil.processor().ar.setValue(0x123456789aL);
    smil.memory().set(0x000, 0x0013400000);
    jump(0x000, false);
    smil.processor().oneStep();
    assertEquals(smil.memory().get(0x001), 0x1230000000L);

    smil.reset();
    smil.processor().ar.setValue(0x123456789aL);
    smil.memory().set(0x000, 0x0013800000);
    jump(0x000, false);
    smil.processor().oneStep();
    assertEquals(smil.memory().get(0x001), 0x0000067800L);

    smil.reset();
    smil.processor().ar.setValue(0x123456789aL);
    smil.memory().set(0x000, 0x0013c00000);
    jump(0x000, false);
    smil.processor().oneStep();
    assertEquals(smil.memory().get(0x001), 0x1230067800L);

  }

  public void testAdd(long a, long b, int instruction) throws Exception {
    smil.reset();
    smil.processor().ar.setValue(a);
    smil.memory().set(0x000, b);
    smil.memory().set(0x001, instruction);
    jump(0x001, true);
    smil.processor().oneStep();
  }

  public void testAddToMr(long a, long b, int extras, long expected) throws Exception {
    testAdd(a, b, 0x40 | extras);
    assertEquals(smil.processor().ar.value(), expected);
    assertEquals(smil.processor().mr.value(), expected);
  }

  public void testAddToAr(long a, long b, int extras, long expected) throws Exception {
    testAdd(a, b, 0x50 | extras);
    assertEquals(smil.processor().ar.value(), expected);
  }

  public void testInstruction4() throws Exception {
    testAddToMr(0x0000000001L, 0x0000000001L, 0x0, 0x0000000002L);
    testAddToMr(0x0000000001L, 0x0000000001L, 0x4, 0x0000000002L);
    testAddToMr(0x0000000001L, 0x0000000001L, 0x8, 0x0000000000L);
    testAddToMr(0x0000000001L, 0x0000000001L, 0xC, 0x0000000000L);
    testAddToMr(0x0000000001L, 0xffffffffffL, 0x0, 0x0000000000L);
    testAddToMr(0x0000000001L, 0xffffffffffL, 0x4, 0x0000000002L);
    testAddToMr(0x0000000001L, 0xffffffffffL, 0x8, 0x0000000002L);
    testAddToMr(0x0000000001L, 0xffffffffffL, 0xC, 0x0000000000L);
    testAddToMr(0x4000000000L, 0x4000000000L, 0x0, 0x8000000000L);
    assertTrue(smil.processor().ar.overflow());
  }

  public void testInstruction5() throws Exception {
    testAddToAr(0x0000000001L, 0x0000000001L, 0x0, 0x0000000002L);
    testAddToAr(0x0000000001L, 0x0000000001L, 0x4, 0x0000000002L);
    testAddToAr(0x0000000001L, 0x0000000001L, 0x8, 0x0000000000L);
    testAddToAr(0x0000000001L, 0x0000000001L, 0xC, 0x0000000000L);
    testAddToAr(0x0000000001L, 0xffffffffffL, 0x0, 0x0000000000L);
    testAddToAr(0x0000000001L, 0xffffffffffL, 0x4, 0x0000000002L);
    testAddToAr(0x0000000001L, 0xffffffffffL, 0x8, 0x0000000002L);
    testAddToAr(0x0000000001L, 0xffffffffffL, 0xC, 0x0000000000L);
    testAddToAr(0x4000000000L, 0x4000000000L, 0x0, 0x8000000000L);
    assertTrue(smil.processor().ar.overflow());
  }

  public void testInstruction6() throws Exception {
    smil.reset();
    smil.memory().set(0x000, SMIL.word(0.125));
    smil.processor().mr.setDoubleValue(0.5);
    smil.memory().set(0x001, 0x0006000000L);
    jump(0x001, false);
    smil.processor().oneStep();
    assertCloseTo(0.0625, smil.processor().ar.doubleValue());
    assertEquals(smil.processor().mr.value(), 0x0000000000L);
  }

  public void testInstruction7() throws Exception {
    smil.reset();
    smil.memory().set(0x000, SMIL.word(0.125));
    smil.processor().mr.setDoubleValue(0.5);
    smil.memory().set(0x001, 0x0007200000L);
    jump(0x001, false);
    smil.processor().oneStep();
    assertCloseTo(0.0625, smil.processor().ar.doubleValue());
  }

  public void testInstruction8() throws Exception {
    smil.reset();
    smil.memory().set(0x000, SMIL.word(0.5));
    smil.processor().ar.setDoubleValue(0.125);
    smil.memory().set(0x001, 0x0008000028L);
    jump(0x001, false);
    smil.processor().oneStep();
    smil.processor().oneStep();
    assertCloseTo(0.25, smil.processor().ar.doubleValue());
  }

  public void testJump(long arPrimValue, int instruction, boolean jumpExpected, boolean rightExpected) throws Exception {
    smil.reset();
    smil.processor().ar.setBits(arPrimValue);
    smil.memory().set(0x000, (0x01000L | instruction) << 20);
    jump(0x000, false);
    smil.processor().oneStep();
    if (jumpExpected) {
      assertEquals(0x010, smil.processor().kr.value());
      assertEquals(rightExpected, smil.processor().kr.isBitSet(-1));
    } else {
      assertEquals(0x000, smil.processor().kr.value());
    }
  }

  public void testInstruction9() throws Exception {
    testJump(0x0L, 0x90, true, false);
    testJump(0x0L, 0x94, true, true);
    testJump(0x00000000000L, 0x98, false, false);
    testJump(0x30000000000L, 0x98, false, false);
    testJump(0x20000000000L, 0x98, true, false);
    testJump(0x10000000000L, 0x98, true, false);
    testJump(0x00000000000L, 0x9C, false, true);
    testJump(0x30000000000L, 0x9C, false, true);
    testJump(0x20000000000L, 0x9C, true, true);
    testJump(0x10000000000L, 0x9C, true, true);
  }

  public void testInstructionA() throws Exception {
    testJump(0x10200000000L, 0xa0, false, false);
    testJump(0x0000000000L, 0xa0, true, false);
    testJump(0x0200000000L, 0xa0, true, false);
    testJump(0x10200000000L, 0xa4, false, true);
    testJump(0x0000000000L, 0xa4, true, true);
    testJump(0x0200000000L, 0xa4, true, true);
    testJump(0x10200000000L, 0xa8, true, false);
    testJump(0x0000000000L, 0xa8, false, false);
    testJump(0x0200000000L, 0xa8, false, false);
    testJump(0x10200000000L, 0xac, true, true);
    testJump(0x0200000000L, 0xac, false, true);
    testJump(0x0000000000L, 0xac, false, true);
  }

  public void testInstructionB() throws Exception {
    smil.reset();
    smil.memory().set(0x000, 0x000B000000L);
    jump(0x000, false);
    smil.setRunMode(true, false);
    smil.run();
    assertEquals(smil.processor().kr.value(), 0x000);
    assertEquals(smil.processor().kr.bit(-1), 1);
  }

  public void testNormalize(long value, int expectedShift, long expectedValue) throws Exception {
    smil.reset();
    smil.processor().ar.setValue(value);
    smil.memory().set(0x000, 0x000C000000L);
    jump(0x000, false);
    smil.processor().oneStep();
    if (value != 0L) {
      assertEquals(smil.processor().ar.isBitSet(0), !smil.processor().ar.isBitSet(1));
    }
    long shifts = smil.processor().mr.bits(8, 24);
    assertEquals(shifts, expectedShift);
    assertEquals(smil.processor().ar.value(), value << shifts & SMIL.WORD_MASK);
    assertEquals(smil.processor().ar.value(), expectedValue);
  }

  public void testInstructionC() throws Exception {
    testNormalize(0x0L, 0x3f, 0x0L);
    testNormalize(0x8000000000L, 0, 0x8000000000L);
    testNormalize(0x4000000000L, 0, 0x4000000000L);
    testNormalize(0x0090000000L, 7, 0x4800000000L);
    testNormalize(0xff90000000L, 8, 0x9000000000L);
  }

  public void testShift(long value, int instruction, long expected) throws Exception {
    smil.reset();
    smil.processor().ar.setValue(value);
    smil.memory().set(0x000, (long) instruction << 20);
    jump(0x000, false);
    smil.processor().oneStep();
    assertEquals(String.format("expected %x but was %x", expected, smil.processor().ar.value()),
        expected, smil.processor().ar.value());
  }

  public void testInstructionD() throws Exception {
    testShift(0x0001234000L, 0x004D0, 0x0012340000L);
    testShift(0x0001234000L, 0x004D8, 0x0000123400L);
    testShift(0x0001234000L, 0x004DC, 0x0000123400L);
    testShift(0xfff1234000L, 0x004DC, 0x0fff123400L);
    testShift(0xfff1234000L, 0x004D8, 0xffff123400L);
  }

  public void testTypewriter(Long arValue, int instruction, Character expected) throws Exception {
    smil.reset();
    smil.typewriter().clear();
    if (arValue != null) {
      smil.processor().ar.setValue(arValue);
    }
    smil.memory().set(0x000, (long) instruction << 20);
    jump(0x000, false);
  smil.processor().oneStep();
    String text = smil.typewriter().text();
    if (expected == null) {
      assertEquals(0, text.length());
    } else {
      assertEquals(1, text.length());
      char c = text.charAt(0);
      assertEquals(c, (char) expected);
    }
  }

  public void testTypewriter(int instruction, Character expected) throws Exception {
    testTypewriter(null, instruction, expected);
  }

  public void testInstructionF() throws Exception {
    testTypewriter(0x0L, 0xF0, '0');
    testTypewriter(0x1L, 0xF0, '1');
    testTypewriter(0x2L, 0xF0, '2');
    testTypewriter(0x3L, 0xF0, '3');
    testTypewriter(0x4L, 0xF0, '4');
    testTypewriter(0x5L, 0xF0, '5');
    testTypewriter(0x6L, 0xF0, '6');
    testTypewriter(0x7L, 0xF0, '7');
    testTypewriter(0x8L, 0xF0, '8');
    testTypewriter(0x9L, 0xF0, '9');
    testTypewriter(0xAL, 0xF0, 'A');
    testTypewriter(0xBL, 0xF0, 'B');
    testTypewriter(0xCL, 0xF0, 'C');
    testTypewriter(0xDL, 0xF0, 'D');
    testTypewriter(0xEL, 0xF0, 'E');
    testTypewriter(0xFL, 0xF0, 'F');
    testTypewriter(0x0F8, ' ');
    testTypewriter(0x1F8, '\n');
    testTypewriter(0x2F8, '.');
    testTypewriter(0x3F8, '\t');
    testTypewriter(0x4F8, '-');
    testTypewriter(0x5F8, '+');
    testTypewriter(0x6F8, '_');
    testTypewriter(0x7F8, null);
    testTypewriter(0x8F8, 'i');
  }

  private final class FakeClock extends Clock<Clock.UnitTick> {
    long now = 0;

    @Override
    public boolean wasInterrupted() {
      return false;
    }

    @Override
    public long sleepUntil(long then, double speedup, boolean doWait) {
      return now = then;
    }

    @Override
    public long sleep(long delay, double speedup, boolean doWait) {
      return (now = now + delay);
    }

    @Override
    public void reset() {
      now = 0;
    }

    @Override
    public long now() {
      return now;
    }

    @Override
    public InterruptedException interruptedException() {
      return null;
    }

    @Override
    public void interruptSleepers() {
    }
  }

  private final class FakeTypewriter extends Typewriter.Default {
    StringBuilder builder = new StringBuilder();

    @Override
    public int length() {
      return builder.length();
    }

    @Override
    public String text() {
      return builder.toString();
    }

    @Override
    public void append(String s) {
      builder.append(s);
    }

    @Override
    public void clear() {
      builder = new StringBuilder();
    }
  }

  private final class FakeSoundGenerator extends SoundGenerator {
    @Override
    public void stopDestination(boolean finishPlaying, boolean retainData) {
    }

    @Override public void startDestination() {
    }

    @Override public void pushBufferToDestination() {
    }

    @Override public boolean canGenerateSound() {
      return false;
    }
  }
}
