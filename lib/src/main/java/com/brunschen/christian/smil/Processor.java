/**
 * 
 */
package com.brunschen.christian.smil;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author Christian Brunschen
 *
 */
public class Processor {
  
  public static final int decodeInstructionClockCycles = 6;
  public static final int shortOperationClockCycles = 9;
  public static final int longOperationClockCycles = shortOperationClockCycles + 43;
  public static final int clockCyclesPerSecond = 100000; // normal SMIL speed = 100 kHz
  public static final int nanosPerClockCycle = 1000000000 / clockCyclesPerSecond;
  
  public Accumulator ar = new Accumulator("AR"); // accumulator
  public ValueRegister mr = new ValueRegister("MR", 0, 1); // multiplicator
  public ValueRegister md = new ValueRegister("MD"); // multiplicand
  public ValueRegister ir = new ValueRegister("IR"); // instruction register
  public ProgramCounter kr = new ProgramCounter("KR"); // control register == program counter
  public BitCounter br = new BitCounter("BR"); // bit counter (used in multiply & divide instructions)
  public Register rr = new Register("RR", Register.BitOrder.LITTLE_ENDIAN, 0, 1, 0);
  public Register dummy = new Register("dummy", Register.BitOrder.BIG_ENDIAN, 0, 13, 0);

  public Register[] registers = new Register[] { ar, mr, md, ir, kr, br };
  public Operation[] operations = new Operation[16];
  
  boolean needToLoadIr = false;
  boolean jumped = false;
  private SMIL smil;
  protected Clock<Clock.UnitTick> clock;

  public void addOperation(int i, Operation operation) {
    operations[i] = operation;
  }

  public Operation operation(int i) {
    return operations[i];
  }

  public void prepareOperations() {
    addOperation(0x0, new Operation("Read from Tape") {
      public void describe(PrintWriter pw, int address, int extras) {
        if (SMIL.bitIsSet(extras, 3)) {
          pw.printf("tape row -> AR{36..39}");
        } else {
          pw.printf("tape word -> AR");
          if (!SMIL.bitIsSet(extras, 2)) {
            pw.printf(", [%03X]", address);
          }
        }
      }

      public void perform(int address, int extras) {
        try {
          if (SMIL.bitIsSet(extras, 3)) {
            // read one single row
            int value = smil.tapeReader_read();
            long newValue = ar.value() & 0xfffffffff0L | value & 0xf;
            ar.setValue(newValue);
          } else {
            // read a whole word
            long word = smil.tapeReader_readWord();
            ar.setValue(word);
            if (!SMIL.bitIsSet(extras, 2)) {
              smil.memory_write(address, word);
            }
          }
        } catch (IOException e) {
          smil.stop();
        }

        clock.sleep(shortOperationClockCycles);
      }

      public Traceable[] trace(int address, int extras) {
        if (!SMIL.bitIsSet(extras, 3) && !SMIL.bitIsSet(extras, 2)) {
          return new Traceable[] { T(ar), T(address) };
        } else {
          return new Traceable[] { T(ar) };
        }
      }

    });
    addOperation(0x1, new Operation("Logical Product") {
      public void describe(PrintWriter pw, int address, int extras) {
        String minus = shouldNegate(extras) ? "-" : "";
        String abs = shouldAbsolute(extras) ? "|" : "";
        String arSource = shouldClearAr(extras) ? "" : "AR & ";
        pw.printf("%s%s%s[%03X]%s -> AR", arSource, minus, abs, address, abs);
      }

      public void perform(int address, int extras) {
        loadMd(address, extras);
        ar.add(md, shouldNegate(extras), shouldAbsolute(extras));
        clock.sleep(shortOperationClockCycles);
        and();
      }

      public Traceable[] traceBefore(int address, int extras) {
        return new Traceable[] { T(address, false), T(md, false), T(mr, false) };
      }

      public Traceable[] traceAfter(int address, int extras) {
        return new Traceable[] { T(ar, false), T(mr, false) };
      }
    });
    addOperation(0x2, new Operation("Transfer") {
      public void describe(PrintWriter pw, int address, int extras) {
        if (SMIL.bitIsSet(extras, 3)) {
          pw.printf("MR{0..39} -> AR{39..0}");
        } else {
          pw.printf("MR -> AR");
        }
        pw.printf(", 0 -> MR");
      }

      public void perform(int address, int extras) {
        if (SMIL.bitIsSet(extras, 3)) {
          reverseMrIntoAr();
        } else {
          copyMrToAr();
        }
      }

      public Traceable[] traceBefore(int address, int extras) {
        return new Traceable[] { T(ar, false), T(mr, false) };
      }

      public Traceable[] traceAfter(int address, int extras) {
        return new Traceable[] { T(ar), T(mr, false) };
      }
    });
    addOperation(0x3, new Operation("Store") {
      public void describe(PrintWriter pw, int address, int extras) {
        String bits;
        switch (extras >>> 2) {
          case 1:
            bits = "{0..11}";
            break;
          case 2:
            bits = "{20..31}";
            break;
          case 3:
            bits = "{0..11,20..31}";
            break;
          default:
            bits = "";
            break;
        }
        String arSource = shouldClearAr(extras) ? "0" : ar.name() + bits;
        pw.printf("%s -> [%03X]%s", arSource, address, bits);
      }

      public void perform(int address, int extras) {
        long mask = SMIL.WORD_MASK;
        switch (extras >>> 2) {
          case 1:
            mask = 0xfff0000000L;
            break;
          case 2:
            mask = 0x00000fff00L;
            break;
          case 3:
            mask = 0xfff00fff00L;
            break;
        }
        smil.memory_write(address, ar.value(), mask);

        clock.sleep(30); // special case, 30 clock pulses
      }

      public Traceable[] traceBefore(int address, int extras) {
        if (shouldClearAr(extras)) {
          return new Traceable[] { T(address, false) };
        } else {
          return new Traceable[] { T(address, false), T(ar, false) };
        }
      }

      public Traceable[] traceAfter(int address, int extras) {
        return new Traceable[] { T(address, false) };
      }
    });
    addOperation(0x4, new Operation("Add to MR") {
      public void describe(PrintWriter pw, int address, int extras) {
        String abs = shouldAbsolute(extras) ? "|" : "";
        String source = shouldClearAr(extras) ? (shouldNegate(extras) ? "-" : "") : "AR "
            + (shouldNegate(extras) ? "-" : "+") + " ";
        pw.printf("%s%s[%03X]%s -> AR, MR", source, abs, address, abs);
      }

      public void perform(int address, int extras) {
        loadMd(address, extras);
        ar.add(md, shouldNegate(extras), shouldAbsolute(extras));
        mr.copy(ar);
        clock.sleep(shortOperationClockCycles);
      }

      public Traceable[] traceBefore(int address, int extras) {
        return new Traceable[] { T(address), T(ar), T(mr) };
      }

      public Traceable[] traceAfter(int address, int extras) {
        return new Traceable[] { T(ar), T(mr) };
      }
    });
    addOperation(0x5, new Operation("Add to AR") {
      public void describe(PrintWriter pw, int address, int extras) {
        String abs = shouldAbsolute(extras) ? "|" : "";
        String source = shouldClearAr(extras) ? (shouldNegate(extras) ? "-" : "") : "AR "
            + (shouldNegate(extras) ? "-" : "+") + " ";
        pw.printf("%s%s[%03X]%s -> AR", source, abs, address, abs);
      }

      public void perform(int address, int extras) {
        loadMd(address, extras);
        ar.add(md, shouldNegate(extras), shouldAbsolute(extras));
        clock.sleep(shortOperationClockCycles);
      }

      public Traceable[] traceBefore(int address, int extras) {
        return new Traceable[] { T(address), T(ar) };
      }

      public Traceable[] trace(int address, int extras) {
        return new Traceable[] { T(ar) };
      }
    });
    addOperation(0x6, new Operation("Multiply") {
      public void describe(PrintWriter pw, int address, int extras) {
        String arSource = shouldClearAr(extras) ? "" : " + AR * 2^-39";
        pw.printf("[%03X] * MR%s -> AR, MR", address, arSource);
      }

      public void perform(int address, int extras) {
        loadMd(address, extras);
        multiply();
      }

      public Traceable[] traceBefore(int address, int extras) {
        return new Traceable[] { T(address), T(mr), T(ar) };
      }

      public Traceable[] traceAfter(int address, int extras) {
        return new Traceable[] { T(ar), T(mr) };
      }
    });
    addOperation(0x7, new Operation("Short Multiply") {
      public void describe(PrintWriter pw, int address, int extras) {
        pw.printf("[%03X] * MR + 2^-40 -> AR, 0 -> MR", address);
      }

      public void perform(int address, int extras) {
        loadMd(address, extras);
        multiply();
        if (mr.isBitSet(1)) { // bit 1 contains the most significant
          // digit of the result
          ar.increment();
        }
        mr.clear();
      }

      public Traceable[] traceBefore(int address, int extras) {
        return new Traceable[] { T(address), T(mr), T(ar) };
      }

      public Traceable[] traceAfter(int address, int extras) {
        return new Traceable[] { T(ar), T(mr) };
      }
    });
    addOperation(0x8, new Operation("Divide") {
      public void describe(PrintWriter pw, int address, int extras) {
        pw.printf("AR / [%03X] -> MR{39...0}", address);
      }

      public void perform(int address, int extras) {
        loadMd(address, extras);
        divide();
      }

      public Traceable[] traceBefore(int address, int extras) {
        return new Traceable[] { T(ar), T(address) };
      }

      public Traceable[] traceAfter(int address, int extras) {
        return new Traceable[] { T(ar), T(mr) };
      }
    });
    addOperation(0x9, new Operation("Jump with Overflow") {
      public void describe(PrintWriter pw, int address, int extras) {
        String half = SMIL.bitIsSet(extras, 2) ? "right" : "left ";
        String condition = SMIL.bitIsSet(extras, 3) ? " if AR{00} != AR{0}" : "";
        pw.printf("JUMP %03X %s%s", address, half, condition);
      }

      public void perform(int address, int extras) {
        boolean jumpOnlyIfOverflow = SMIL.bitIsSet(extras, 3);
        boolean destinationIsRight = SMIL.bitIsSet(extras, 2);
        if (!jumpOnlyIfOverflow || ar.overflow()) {
          jump(address, destinationIsRight);
        }
        clock.sleep(shortOperationClockCycles);
      }

      public Traceable[] traceBefore(int address, int extras) {
        if (SMIL.bitIsSet(extras, 3)) {
          return new Traceable[] { T(ar), T(kr) };
        } else {
          return new Traceable[] { T(kr) };
        }
      }

      public Traceable[] traceAfter(int address, int extras) {
        return new Traceable[] { T(kr) };
      }
    });
    addOperation(0xa, new Operation("Jump with Sign") {
      public void describe(PrintWriter pw, int address, int extras) {
        String half = SMIL.bitIsSet(extras, 2) ? "right" : "left ";
        String condition = SMIL.bitIsSet(extras, 3) ? "< 0" : ">= 0 ";
        pw.printf("JUMP %03X %s if AR %s", address, half, condition);
      }

      public void perform(int address, int extras) {
        boolean destinationIsRight = SMIL.bitIsSet(extras, 2);
        boolean wantNegative = SMIL.bitIsSet(extras, 3);
        if (wantNegative && ar.isSignBitSet() || !wantNegative && !ar.isSignBitSet()) {
          jump(address, destinationIsRight);
        }
        clock.sleep(shortOperationClockCycles);
      }

      public Traceable[] traceBefore(int address, int extras) {
        return new Traceable[] { T(ar), T(kr) };
      }

      public Traceable[] traceAfter(int address, int extras) {
        return new Traceable[] { T(kr) };
      }
    });
    addOperation(0xb, new Operation("Control") {
      public void describe(PrintWriter pw, int address, int extras) {
        pw.printf(SMIL.bitIsSet(extras, 3) ? "NOOP" : "HALT");
      }

      public void perform(int address, int extras) {
        if (!SMIL.bitIsSet(extras, 3)) {
          smil.stop();
        } else {
          clock.sleep(shortOperationClockCycles);
        }
      }

      public Traceable[] traceBefore(int address, int extras) {
        if (SMIL.bitIsSet(extras, 3)) {
          return new Traceable[] { T("continuing") };
        } else {
          return new Traceable[] { T("halting") };
        }
      }

      public Traceable[] traceAfter(int address, int extras) {
        return new Traceable[] {};
      }
    });
    addOperation(0xc, new Operation("Normalize") {
      public void describe(PrintWriter pw, int address, int extras) {
        pw.printf("normalize AR, shift count * 2^-31 -> MR");
      }

      public void perform(int address, int extras) {
        mr.clear();
        clock.sleep(shortOperationClockCycles);
        normalize();
      }

      public Traceable[] traceBefore(int address, int extras) {
        return new Traceable[] { T(ar) };
      }

      public Traceable[] traceAfter(int address, int extras) {
        return new Traceable[] {T(ar), T(mr) };
      }
    });
    addOperation(0xd, new Operation("Shift") {
      public void describe(PrintWriter pw, int address, int extras) {
        switch (extras >>> 2) {
          case 0:
            pw.printf("AR << %d -> AR", address);
            break;
          case 2:
            pw.printf("AR >> %d (arithmetic) -> AR", address);
            break;
          case 3:
            pw.printf("AR >> %d -> AR", address);
            break;
        }
      }

      public void perform(int address, int extras) {
        clock.sleep(shortOperationClockCycles);
        switch (extras >>> 2) {
          case 0:
            shiftLeft(address);
            break;
          case 2:
            shiftRight(address, true);
            break;
          case 3:
            shiftRight(address, false);
            break;
        }
      }

      public Traceable[] traceBefore(int address, int extras) {
        return new Traceable[] { T(ar) };
      }

      public Traceable[] traceAfter(int address, int extras) {
        return new Traceable[] { T(ar) };
      }
    });
    addOperation(0xe, new Operation("<invalid>") {
      public void describe(PrintWriter pw, int address, int extras) {
        pw.printf("<%03Xe%x>??", address, extras);
      }

      public void perform(int address, int extras) {
        clock.sleep(9);
      }
      
      public Traceable[] traceBefore(int address, int extras) {
        return new Traceable[] { T("invalid instruction, ignoring") };
      }
    });
    addOperation(0xf, new Operation("Typewriter") {
      private String specials[] = new String[] { "<space>", "<return>", "'.'", "<tab>", "'-'", "'+'", "'_'", null,
          "'i'", };

      public void describe(PrintWriter pw, int address, int extras) {
        if (SMIL.bitIsSet(extras, 3)) {
          String special = address <= specials.length ? specials[address] : null;
          pw.printf("PRINT %s", special != null ? special : "<undef>");
        } else {
          pw.printf("PRINT hex(AR{36..39})");
        }
      }

      public void perform(int address, int extras) {
        if (SMIL.bitIsSet(extras, 3)) {
          smil.typewriter_printSpecial((int) (address & 0xf));
        } else {
          smil.typewriter_printHex((int) (ar.value() & 0xf));
        }
        clock.sleep(shortOperationClockCycles);
      }

      public Traceable[] traceBefore(int address, int extras) {
        if (SMIL.bitIsSet(extras, 3)) {
          return new Traceable[] { };
        } else {
          return new Traceable[] { T(ar) };
        }
      }

      public Traceable[] traceAfter(int address, int extras) {
        return new Traceable[] { };
      }
    });
  }

  public static boolean shouldStopConditionally(int extras) {
    return SMIL.bitIsSet(extras, 0);
  }

  public static boolean shouldClearAr(int extras) {
    return SMIL.bitIsSet(extras, 1);
  }

  public static boolean shouldAbsolute(int extras) {
    return SMIL.bitIsSet(extras, 2);
  }

  public static boolean shouldNegate(int extras) {
    return SMIL.bitIsSet(extras, 3);
  }

  public static long negate(long value) {
    return (value ^ SMIL.WORD_MASK) + 1 & SMIL.WORD_MASK;
  }

  public static long absValue(long value) {
    if (SMIL.bitIsSet(value, SMIL.WORD_BITS - 1)) {
      return negate(value);
    } else {
      return value;
    }
  }

  public void multiply() {
    clock.sleep(2);
    boolean mrWasNegative = mr.isSignBitSet();

    for (br.setValue(1); br.value() < SMIL.WORD_BITS; br.increment()) {
      if (mr.isLeastBitSet()) { // 'mr'[39] == 1?
        // 'ar' += 'md';
        ar.add(md);
      }

      // shift right one step
      ar.shiftRightArithmetic();

      // and the multiplicator, too
      mr.shiftRight();
      // shift in the previous low bit from 'ar' at the top of 'mr'
      if (ar.isLowBitSet()) {
        mr.setBit(0, true);
      }
      
      clock.sleep(1);
    }

    // shift 'mr' right, past the sign bit
    mr.shiftRight();

    // adjust for negative multiplicators
    if (mrWasNegative) {
      ar.subtract(md);
    }
    
    clock.sleep(2);
  }

  public void divide() {
    clock.sleep(2);
    
    for (br.setValue(1); br.value() < SMIL.WORD_BITS; br.increment()) {
      boolean sameSign = ar.signBit() == md.signBit();
      ar.shiftLeft();
      mr.shiftRight();
      if (sameSign) {
        ar.subtract(md);
        mr.setBit(0);
      } else {
        ar.add(md);
        mr.clearBit(0);
      }
      
      clock.sleep(1);
    }
    mr.shiftRight();
    mr.setBit(0);
    mr.toggleBit(mr.LEAST_BIT);
    clock.sleep(2);
  }

  public void reverseMrIntoAr() {
    clock.sleep(2);
    for (br.setValue(0); br.value() < SMIL.WORD_BITS; br.increment()) {
      boolean leastBitSet = mr.isLeastBitSet();
      mr.shiftRight();
      ar.shiftLeft();
      if (leastBitSet) {
        ar.increment();
      }
      clock.sleep(1);
    }
    clock.sleep(2);
  }
  
  public void copyMrToAr() {
    clock.sleep(2);
    for (br.setValue(0); br.value() < SMIL.WORD_BITS; br.increment()) {
      boolean leastBitSet = mr.isLeastBitSet();
      mr.shiftRight();
      ar.shiftRight();
      if (leastBitSet) {
        ar.setBit(0);
      }
      clock.sleep(1);
    }
    clock.sleep(2);
  }
  
  public void normalize() {
    br.setValue(0);
    clock.sleep(2);
    while (ar.bit(0) == ar.bit(1) && br.value() < 63) {
      ar.shiftLeft();
      clock.sleep(1);
      br.setValue(br.value() + 1);
    }
    mr.setValue(br.value() << 8);
    clock.sleep(2);
  }
  
  public void shiftLeft(int n) {
    clock.sleep(2);
    br.setValue(0);
    while (br.value() != n) {
      ar.shiftLeft();
      br.setValue(br.value() + 1);
      clock.sleep(1);
    }
    clock.sleep(2);
  }

  public void shiftRight(int n, boolean arithmetic) {
    clock.sleep(2);
    br.setValue(0);
    while (br.value() != n) {
      ar.shiftRight(arithmetic);
      br.setValue(br.value() + 1);
      clock.sleep(1);
    }
    clock.sleep(2);
  }
  
  public void and() {
    clock.sleep(2);
    br.setValue(0);
    while (br.value() < SMIL.WORD_BITS) {
      int arBit = ar.leastBit();
      int mrBit = mr.leastBit();
      ar.shiftRight();
      mr.shiftRight();
      ar.setBit(0, arBit & mrBit);
      br.setValue(br.value() + 1);
      clock.sleep(1);
    }
    clock.sleep(2);
  }

  public void loadIrIfNecessary() {
    if (needToLoadIr) {
      int address = (int) kr.value();
      long word = smil.memory_read(address);
      ir.setValue(word);
      needToLoadIr = false;
    }
  }

  public void loadMd(int address, int extras) {
    md.setValue(smil.memory_read(address));
  }

  public void jump(int address, boolean right) {
    kr.setValue(address);
    kr.setBit(ProgramCounter.RIGHT_BIT, right);
    needToLoadIr = true;
    jumped = true;
  }

  public void reset() {
    for (Register r : registers) {
      r.clear();
    }
  }
  
  public Processor(SMIL smil, Clock<Clock.UnitTick> clock) {
    this.smil = smil;
    this.clock = clock;
  }
  
  public void oneStep() {
    jumped = false;
    // check which one of the instructions in ir we need to execute
    boolean right = needToLoadIr = kr.isBitSet(ProgramCounter.RIGHT_BIT);
    int instructionAddress = (int) kr.value();
    // execute the instruction
    long instructionHalfword = ir.bits(right ? 20 : 0, 20);

    int address = (int) ((instructionHalfword & 0xfff00L) >>> 8);
    int instructionGroup = (int) ((instructionHalfword & 0xf0L) >>> 4);
    int extras = (int) (instructionHalfword & 0xfL);

    if (shouldClearAr(extras)) {
      ar.clear();
    }
    clock.sleep(decodeInstructionClockCycles);
    Operation op = operation(instructionGroup);
    if (op != null) {
      Traceable[] before = null, after = null;
      
      if (smil.trace()) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        before = op.traceBefore(address, extras);
        after = op.traceAfter(address, extras);
        
        pw.format("[%03X.%d] %-35s: ", instructionAddress, right ? 1 : 0, op.describe(address, extras));
        
        boolean first = true;
        for (Traceable traceable : before) {
          if (first) {
            first = false;
          } else {
            pw.append(", ");
          }
          traceable.appendTo(pw);
        }
        
        if (after.length > 0) {
          pw.append(" => ");
        }
        
        smil.debug(sw.toString());
      }

      op.perform(address, extras);

      if (smil.trace()) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        boolean first = true;
        for (Traceable traceable : after) {
          if (first) {
            first = false;
          } else {
            pw.append(", ");
          }
          traceable.appendTo(pw);
        }
        
        pw.append("\n");
        
        smil.debug(sw.toString());
      }
    }
    
    if (!jumped) {
      // if we didn't jump, advance kr
      kr.setBits(kr.bits() + 1);
    }

    // reload ir if necessary
    loadIrIfNecessary();

    // if this instruction had the 'stop conditionally' bit set, and the
    // control panel switch is set to stop conditionally, ...
    if (shouldStopConditionally(extras) && smil.stopConditionally()) {
      // ... then stop.
      // System.err.format("Stopping Conditionally\n");
      smil.stop();
    }
  }

  
  // shorthand 'macro' calls to generate traceables for different things
  private Traceable T(Register r, boolean asDouble) {
    return smil.traceRegister(r, asDouble);
  }

  private Traceable T(Register r) {
    return smil.traceRegister(r);
  }
  
  private Traceable T(int address, boolean asDouble) {
    return smil.traceMemory(address, asDouble);
  }

  private Traceable T(int address) {
    return smil.traceMemory(address);
  }
  
  private Traceable T(ProgramCounter kr) {
    return smil.traceProgramCounter(kr);
  }
  
  private Traceable T(String message) {
    return smil.traceMessage(message);
  }

}
