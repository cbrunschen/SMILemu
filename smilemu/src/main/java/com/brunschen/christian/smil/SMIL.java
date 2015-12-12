/*
 * SMIL
 *
 * Copyright (C) 2005 - 2007 Christian Brunschen
 *
 * This program is free software; you can redistribute it and/processor.or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, processor.or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY processor.or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

package com.brunschen.christian.smil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import com.brunschen.christian.graphic.ValueUpdatedListener;
import com.brunschen.christian.smil.Clock.UnitTick;
import com.brunschen.christian.smil.sound.SoundGenerator;

public class SMIL implements Runnable {
  
  public static long mask(int nBits, int shift) {
    if (nBits == 0) return 0L;
    return (~0L >>> (Long.SIZE - nBits)) << shift; 
  }

  public static final int WORD_BITS = 40;
  public static final int HALFWORD_BITS = WORD_BITS / 2;
  public static final long WORD_MASK = mask(WORD_BITS, 0);
  public static final long LEFT_MASK = mask(HALFWORD_BITS, HALFWORD_BITS);
  public static final long RIGHT_MASK = mask(HALFWORD_BITS, 0);
  public static final int ADDRESS_BITS = 3 * HALFWORD_BITS / 5;
  public static final int ADDRESS_SHIFT = HALFWORD_BITS - ADDRESS_BITS;
  public static final long LEFT_ADDRESS_MASK = mask(ADDRESS_BITS, HALFWORD_BITS + ADDRESS_SHIFT);
  public static final long RIGHT_ADDRESS_MASK = mask(ADDRESS_BITS, ADDRESS_SHIFT);

  public static final long SIGN_BIT_MASK = 1L << WORD_BITS - 1;
  public static final long MAX_ABS = 1L << WORD_BITS - 1;
  public static final double MIN_DOUBLE = -1.0;
  public static final double MAX_DOUBLE = (double) (MAX_ABS - 1L) / (double) MAX_ABS;

  public static final long maxNanosAhead = 5000000L; // allow the simulator to get max 5 ms ahead
  // of its intended time
  public static final long ticksPerSecond = 100000L; // frequency of ticks from the rotating drum

  public static final int wordsPerDrumRow = 2;
  public static final int drumRows = 0x400;

  private SystemClock nanosClock = new SystemClock(maxNanosAhead);
  private DrumMemory memory = new DrumMemory(drumRows, wordsPerDrumRow, WORD_BITS);
  private TickClock drumMemoryTickClock = memory.makeTickClock(nanosClock, ticksPerSecond);
  private SlaveClock<Clock.UnitTick> tickClock = new SlaveClock<Clock.UnitTick>(drumMemoryTickClock);
  private SlaveClock<Clock.UnitTick> memoryClock = new SlaveClock<Clock.UnitTick>(tickClock);
  private SlaveClock<Clock.UnitTick> asyncIoClock = new SlaveClock<Clock.UnitTick>(tickClock);
  private Processor processor = new Processor(this, tickClock);

  private boolean runContinuously = true;
  private boolean stopConditionally = false;
  private Object runModeLock = new Object();
  private boolean stop = false;

  private List<Listener> listeners = new LinkedList<Listener>();

  private Thread thread = null;
  private boolean trace = false;
  private DebugDestination debugDestination = null;

  // externally connected units, some with their own graphics for display
  private ControlPanel controlPanel;
  private TapeReader tapeReader;
  private Typewriter typewriter;
  private SoundGenerator soundGenerator;

  private boolean soundEnabled = true;
  private Register soundSourceRegister = processor.ar;
  private int soundSourceBit = Integer.MIN_VALUE;
  private ValueUpdatedListener<Integer> soundSourceValueUpdatedListener = null;

  public static String[] tapes = new String[] { "A1", "B2", "B3", "Decimal Output", "Primes", "Print Integer", "Sine Wave",
      "Square Roots Main Program", "Square Root Subroutine", };

  public static String stripSpaces(String s) {
    StringBuilder sb = new StringBuilder();
    int startIndex = 0;
    int spaceIndex = s.indexOf(' ', startIndex);
    while (spaceIndex >= 0) {
      sb.append(s.substring(startIndex, spaceIndex));
      startIndex = spaceIndex + 1;
      spaceIndex = s.indexOf(' ', startIndex);
    }
    if (startIndex < s.length()) {
      sb.append(s.substring(startIndex));
    }
    return sb.toString();
  }

  public static Tape tape(String name) {
    return new Tape(new InputStreamReader(SMIL.class.getResourceAsStream("Tapes/" + stripSpaces(name))));
  }
  
  public boolean trace() {
    return trace;
  }
  
  public void setTrace(boolean trace) {
    this.trace = trace;
  }
  
  public void setDebugDestination(DebugDestination destination) {
    this.debugDestination = destination;
  }

  public void trace(String format, Object... args) {
    if (trace) {
      debug(String.format(format, args));
    }
  }
  
  public static String printValue(long word, boolean asDouble) {
    if (asDouble) {
      return String.format("%010X (% 13.16f)", word, doubleValue(word));
    } else {
      return String.format("%010X", word);
    }
  }
  
  public SMIL() {
    memory.setClock(memoryClock);
  }
  
  public ControlPanel controlPanel() {
    return controlPanel;
  }

  public void setControlPanel(ControlPanel controlPanel) {
    if (this.controlPanel != null) {
      this.controlPanel.setSmil(null);
    }
    this.controlPanel = controlPanel;
    if (this.controlPanel != null) {
      this.controlPanel.setSmil(this);
    }
  }

  public TapeReader tapeReader() {
    return tapeReader;
  }

  public void setTapeReader(TapeReader tapeReader) {
    this.tapeReader = tapeReader;
  }

  public Typewriter typewriter() {
    return typewriter;
  }

  public void setTypewriter(Typewriter typewriter) {
    this.typewriter = typewriter;
  }

  public SoundGenerator soundGenerator() {
    return soundGenerator;
  }

  public void setSoundGenerator(SoundGenerator soundGenerator) {
    this.soundGenerator = soundGenerator;
  }
  
  public Clock<UnitTick> tickClock() {
    return tickClock;
  }
  
  public Clock<UnitTick> memoryClock() {
    return memoryClock;
  }

  public Clock<UnitTick> asyncIoClock() {
    return asyncIoClock;
  }
  
  public Processor processor() {
    return processor;
  }
  
  public Memory memory() {
    return memory;
  }

  public void init() {
    memory.setClock(memoryClock);

    if (soundGenerator != null) {
      soundGenerator().open();
      // enable sound iff the sound generator can generate sound
      if (soundGenerator().canGenerateSound()) {
        // by default, connect sound to AR bit 38
        connectSound(processor.ar, processor.ar.bitWithName("38"));
        startSound();
      }
    }

    processor.prepareOperations();
  }

  public synchronized void disconnectSound() {
    if (soundSourceValueUpdatedListener != null) {
      soundSourceRegister.removeValueUpdatedListenerForBit(soundSourceValueUpdatedListener, soundSourceBit);
      soundSourceValueUpdatedListener = null;
    }
  }

  public synchronized void connectSound(Register reg, int bit) {
    disconnectSound();
    soundSourceRegister = reg;
    soundSourceBit = bit;
    connectSound();
  }

  private synchronized void connectSound() {
    soundSourceRegister.addValueUpdatedListenerForBit(
        soundSourceValueUpdatedListener = new ValueUpdatedListener<Integer>() {
          public void valueUpdated(Integer oldValue, Integer newValue) {
            soundGenerator.setValue(newValue == 1 ? 1.0 : -1.0);
          }
        }, soundSourceBit);
  }

  public synchronized void startSound() {
    if (soundGenerator.canGenerateSound()) {
      if (thread != null) {
        soundGenerator.start();
      }
      addListener(soundGenerator.smilListener());
      soundEnabled = true;
    } else {
      soundEnabled = false;
    }
  }

  public synchronized void stopSound() {
    if (soundGenerator.canGenerateSound()) {
      if (thread != null) {
        soundGenerator.stop();
      }
      removeListener(soundGenerator.smilListener());
    }
    soundEnabled = false;
  }
  
  public boolean isSoundEnabled() {
    return soundEnabled;
  }
  
  public Register soundSourceRegister() {
    return soundSourceRegister;
  }
  
  public int soundSourceBit() {
    return soundSourceBit;
  }

  public void reset() {
    processor.reset();
    memory.clear();
  }

  public int tapeReader_read() throws IOException {
    try {
      int value = tapeReader().read();
      return value;
    } catch (IOException e) {
      stopWithError(e);
      throw e;
    }
  }

  public long tapeReader_readWord() throws IOException {
    try {
      long value = tapeReader().readWord();
      return value;
    } catch (IOException e) {
      stopWithError(e);
      throw e;
    }
  }

  public void stopWithError(Exception e) {
    stop();
  }

  public void typewriter_printSpecial(int c) {
    asyncIoClock.sleep(ticksPerSecond / 12);
    typewriter().printSpecial(c);
  }

  public void typewriter_printHex(int c) {
    asyncIoClock.sleep(ticksPerSecond / 12);
    typewriter().printHex(c);
  }

  public synchronized void start() {
    dontStop();
    if (thread == null) {
      thread = new Thread(this);
      thread.start();
    }
  }

  public synchronized void started() {
    processor.rr.setValue(1L);
    for (Listener listener : listeners) {
      listener.onStart(this);
    }
  }

  public void run() {
    // reset the clock to start counting from zero
    tickClock.reset();
    // notify listeners that we have started
    started();
    // and let other threads run a bit (sound, in particular)
    Thread.yield();
    do {
      synchronized (this) {
        processor.oneStep();
      }
      Thread.yield();
    } while (runContinuously() && !shouldStop());
    stopped();
    if (trace) {
      trace("Elapsed time: %d clock pulses.\n", tickClock.now());
    }
  }

  public synchronized void stop(boolean shouldStop) {
    stop = shouldStop;
  }

  public void stop() {
    stop(true);
  }

  public void dontStop() {
    stop(false);
  }

  public synchronized boolean shouldStop() {
    return stop;
  }

  public synchronized void stopped() {
    processor.rr.setValue(0L);
    for (Listener listener : listeners) {
      listener.onStop(this);
    }
    thread = null;
    dontStop();
  }

  public void setRunMode(boolean runContinuously, boolean stopConditionally) {
    synchronized (runModeLock) {
      this.runContinuously = runContinuously;
      this.stopConditionally = stopConditionally;
    }
  }

  public boolean runContinuously() {
    synchronized (runModeLock) {
      return runContinuously;
    }
  }

  public boolean stopConditionally() {
    synchronized (runModeLock) {
      return stopConditionally;
    }
  }

  public void tapeStart() {
    processor.ir.setValue(0L);
    processor.kr.setBits(ProgramCounter.RIGHT_BIT, 13, 0x1fff);
  }

  public void addListeners(Listener... listenersToAdd) {
    for (Listener listener : listenersToAdd) {
      listeners.add(listener);
    }
  }

  public void addListener(Listener listener) {
    addListeners(listener);
  }

  public void removeListeners(Listener... listenersToRemove) {
    for (Listener listener : listenersToRemove) {
      listeners.remove(listener);
    }
  }

  public void removeListener(Listener listener) {
    removeListeners(listener);
  }

  public void addSpareTimeListeners(SystemClock.Listener... listenersToAdd) {
    nanosClock.addListeners(listenersToAdd);
  }

  public void addSpareTimeListener(SystemClock.Listener listener) {
    addSpareTimeListeners(listener);
  }

  public void removeSpareTimeListeners(SystemClock.Listener... listenersToRemove) {
    nanosClock.removeListeners(listenersToRemove);
  }

  public void removeSpareTimeListener(SystemClock.Listener listener) {
    removeSpareTimeListeners(listener);
  }

  public void debug(String s) {
    try {
      debugDestination.debug(s);
    } catch (NullPointerException e) {
      e.printStackTrace();
    }
  }

  public static void printHalfword(PrintWriter w, Operation[] operations, long word, boolean right) {
    int shift = right ? 0 : 20;
    int halfword = (int) ((word & 0xfffff << shift) >>> shift);
    int address = (halfword & 0xfff00) >>> 8;
    int instructionGroup = (halfword & 0xf0) >>> 4;
    int extras = halfword & 0xf;
    Operation op = operations[instructionGroup];
    w.write(Processor.shouldClearAr(extras) ? "0 -> AR, " : "         ");
    w.write(Processor.shouldStopConditionally(extras) ? "halt?, " : "       ");
    if (op == null) {
      w.write("<nothing>");
    } else {
      op.describe(w, address, extras);
    }
  }

  public static String printHalfword(Operation[] operations, long word, boolean right) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    printHalfword(pw, operations, word, right);
    return sw.toString();
  }

  public void printWord(PrintWriter w, int start, int end, long word) {
    if (end != start) {
      w.printf("%03X-%03X", start, end);
    } else {
      w.printf("%03X    ", end);
    }
    w.printf(":  %05X %05X       ", (word & 0xfffff00000L) >>> 20, word & 0xfffffL);
    printHalfword(w, processor.operations, word, false);
    w.write("\n         ");
    w.printf("% 16.13f   ", SMIL.doubleValue(word));
    printHalfword(w, processor.operations, word, true);
    w.write('\n');
  }

  public void dumpMemory() {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    int start = 0;
    long previous = memory.get(0);
    for (int i = 1; i < memory.length(); i++) {
      long m = memory.get(i);
      if (m != previous) {
        printWord(pw, start, i - 1, previous);
        pw.print("\n");
        start = i;
        previous = m;
      }
    }
    printWord(pw, start, memory.length() - 1, previous);
    pw.print("\n");
    pw.close();
    debug(sw.toString());
  }

  public void dumpTape() {
    Tape tape = tapeReader().tape();
    if (tape != null) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      List<Long> words = tape.words();
      int start = 0;
      long previous = words.get(0);
      for (int i = 1; i < words.size(); i++) {
        long m = words.get(i);
        if (m != previous) {
          printWord(pw, start, i - 1, previous);
          pw.print('\n');
          start = i;
          previous = m;
        }
      }
      printWord(pw, start, words.size() - 1, previous);
      pw.print('\n');
      pw.close();
      debug(sw.toString());
    }
  }

  public static long value(String s) {
    try {
      return Long.parseLong(s.substring(0, 5), 16) << 20 | Long.parseLong(s.substring(6), 16);
    } catch (NumberFormatException e) {
      e.printStackTrace();
      return 0L;
    }
  }

  static URL[] extractDocUrls = new URL[] { 
    SMIL.class.getResource("Documentation/QuickStart.html"),
    SMIL.class.getResource("Documentation/Manual.html"),
    SMIL.class.getResource("Documentation/style.css"),
  };
  static URL[] extractImageUrls = new URL[] {
    SMIL.class.getResource("Documentation/images/ControlPanel.png"),
    SMIL.class.getResource("Documentation/images/DebugMenu.png"),
    SMIL.class.getResource("Documentation/images/LoadStandardTapeMenu.png"),
    SMIL.class.getResource("Documentation/images/Memory.png"),
    SMIL.class.getResource("Documentation/images/Options.png"),
    SMIL.class.getResource("Documentation/images/PulseLabel.png"),
    SMIL.class.getResource("Documentation/images/Registers.png"),
    SMIL.class.getResource("Documentation/images/SaveMemoryToTape.png"),
    SMIL.class.getResource("Documentation/images/TapeMenu.png"),
    SMIL.class.getResource("Documentation/images/TapeReader.png"),
    SMIL.class.getResource("Documentation/images/Typewriter.png"),
    SMIL.class.getResource("Documentation/images/WindowsMenu.png")
  };

  private static String filenameFromURL(URL url) {
    String path = url.getPath();
    if (path == null) {
      return null;
    }
    int lastSlash = path.lastIndexOf('/');
    if (lastSlash < 0) {
      return null;
    }
    return path.substring(lastSlash + 1);
  }

  private int copy(InputStream is, OutputStream os) throws IOException {
    byte[] buf = new byte[10240];
    int total = 0;
    int nread;
    while ((nread = is.read(buf)) > 0) {
      os.write(buf, 0, nread);
      nread += total;
    }
    is.close();
    os.close();
    return total;
  }

  private int copy(InputStream is, File f) throws IOException {
    f.createNewFile();
    return copy(is, new FileOutputStream(f));
  }

  protected void copy(ProgressCallback cb, URL[] urls, File dir) {
    for (URL u : urls) {
      String filename = filenameFromURL(u);
      cb.starting(filename);
      try {
        InputStream is = u.openStream();
        copy(is, new File(dir, filename));
        cb.finished(filename);
      } catch (IOException e) {
        cb.exception(filename, e);
      }
    }

  }

  public static long reverse(long value, int nBits) {
    long newValue = 0L;
    for (int i = 0; i < nBits; i++) {
      newValue <<= 1;
      newValue |= value & 1;
      value >>= 1;
    }
    return newValue;
  }

  public static boolean bitIsSet(long value, int i) {
    return (value & 1 << i) != 0;
  }

  public static double doubleValue(long word) {
    if ((word & SIGN_BIT_MASK) != 0) {
      long complement = ~word + 1 & WORD_MASK;
      return -(complement / (double) MAX_ABS);
    } else {
      return word / (double) MAX_ABS;
    }
  }

  public double volume() {
    return soundGenerator.volume();
  }

  public static RuntimeException doubleOutOfRangeException(double d) {
    return new RuntimeException(String.format("cannot set double value %23.20f, must be in range [-1.0 .. 1.0)\n", d));
  }

  public static long word(double d) {
    if (d < 0) {
      if (d >= -1.0) {
        long complement = (long) Math.rint(MAX_ABS * -d);
        return ~complement + 1L;
      } else {
        throw doubleOutOfRangeException(d);
      }
    } else {
      if (d < 1.0) {
        return (long) Math.rint(MAX_ABS * d);
      } else {
        throw doubleOutOfRangeException(d);
      }
    }
  }

  public ValueUpdatedListener<Double> volumeUpdatedListener() {
    return new ValueUpdatedListener<Double>() {
      public void valueUpdated(Double oldValue, Double newValue) {
        soundGenerator.setVolume(newValue);
      }
    };
  }

  /**
   * @param address
   * @return
   */
  public long memory_read(int address) {
    return memory.read(address);
  }

  /**
   * @param address
   * @param word
   */
  public void memory_write(int address, long word) {
    memory.write(address, word);
  }

  /**
   * @param address
   * @param word
   * @param mask
   */
  public void memory_write(int address, long word, long mask) {
    memory.write(address, word, mask);
  }

  // tracing support
  private abstract class ValueTracer implements Traceable {
    public void appendTo(PrintWriter pw) {
      appendPrefix(pw);
      pw.format(" = %010X", value());
      if (asDouble()) {
        pw.format(" (% 13.16f)", doubleValue(value()));
      }
    }
    public abstract void appendPrefix(PrintWriter pw);
    public abstract long value();
    public abstract boolean asDouble();
  }
  
  private class MemoryTracer extends ValueTracer {
    boolean asDouble;
    int address;
    public MemoryTracer(int address, boolean asDouble) {
      this.address = address;
      this.asDouble = asDouble;
    }
    public void appendPrefix(PrintWriter pw) {
      pw.format("[%03X]", address);
    }
    public long value() {
      return SMIL.this.memory.get(address);
    }
    public boolean asDouble() {
      return asDouble;
    }
  }
  
  public Traceable traceMemory(int address, boolean asDouble) {
    return new MemoryTracer(address, asDouble);
  }

  public Traceable traceMemory(int address) {
    return this.traceMemory(address, true);
  }
  
  private class RegisterTracer extends ValueTracer {
    Register r;
    boolean asDouble;
    public RegisterTracer(Register r, boolean asDouble) {
      this.r = r;
      this.asDouble = asDouble;
    }
    public void appendPrefix(PrintWriter pw) {
      pw.append(r.name().toLowerCase());
    }
    public long value() {
      return r.value();
    }
    public boolean asDouble() {
      return asDouble;
    }
  }
  
  public Traceable traceRegister(Register r, boolean asDouble) {
    return new RegisterTracer(r, asDouble);
  }
  
  public Traceable traceRegister(Register r) {
    return traceRegister(r, true);
  }

  private class ProgramCounterTracer implements Traceable {
    ProgramCounter kr;
    public ProgramCounterTracer(ProgramCounter kr) {
      this.kr = kr;
    }
    public void appendTo(PrintWriter pw) {
      pw.format("%s = %03X.%d", kr.name().toLowerCase(), kr.value(), kr.low());
    }
  }
  
  public Traceable traceProgramCounter(ProgramCounter r) {
    return new ProgramCounterTracer(r);
  }
  
  private class MessageTracer implements Traceable {
    String message;
    public MessageTracer(String message) {
      this.message = message;
    }
    public void appendTo(PrintWriter pw) {
      pw.append(message);
    }
  }
  
  public Traceable traceMessage(String message) {
    return new MessageTracer(message);
  }

  public interface Listener {
    void onStart(SMIL smil);
    void onStop(SMIL smil);
  }
  
  public interface DebugDestination {
    void debug(String s);
  }
}
