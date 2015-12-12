package com.brunschen.christian.smil.sound;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.brunschen.christian.graphic.ValueUpdatedListener;
import com.brunschen.christian.smil.SMIL;

public abstract class SoundGenerator {
  public static final long MILLIS_PER_SECOND = 1000L;
  public static final long NANOS_PER_SECOND = 1000000000L;
  public static final long NANOS_PER_TENTH_SECOND = NANOS_PER_SECOND / 10L;
  public static final long NANOS_PER_MILLI = NANOS_PER_SECOND / MILLIS_PER_SECOND;
  
  public float sampleRate;
  public int samplesPerTenthSecond;
  public int bitsPerSample;
  public int channels;
  public boolean signed;
  public boolean bigEndian;

  public int bufferSizeFrames;
  public int bytesPerSample;
  public int bytesPerFrame;
  public int bufferSizeBytes;
  public int amp;
  public int mid;

  public int bufferLengthMillis;
  public long updateIntervalNanos;
  public boolean playing = false;
  public boolean receiving = false;
  
  public Thread tickler = null;
  public ReentrantLock ticklerLock = new ReentrantLock();
  public Condition ticklerStateChange = ticklerLock.newCondition();
  public boolean tickling = false;
  
  public long referenceNanos = Long.MIN_VALUE;
  public int framesSinceReference = Integer.MIN_VALUE;
  public double value = 0.0;
  public double volume = 0.5;

  public CircularByteBuffer buffer;
  public Lock bufferLock = new ReentrantLock();
  public Condition bufferEmpty = bufferLock.newCondition();
  public byte[] generationBuffer;
  
  public Collection<ValueUpdatedListener<Integer>> bufferLengthMillisUpdatedListeners = new LinkedList<ValueUpdatedListener<Integer>>();
  public boolean pushingBufferLengthMillis;

  public void open() {
    tickler = new Thread() {
      @Override
      public void run() {
        if (!tickling) {
          return;
        }
        setName("Tickler");
//        System.err.format("Tickler thread: starting\n");
        
        while (tickling) {
          ticklerLock.lock();
          while (!playing && !interrupted()) {
            try {
//              System.err.format("Tickler thread: waiting to play.\n");
              ticklerStateChange.await();
            } catch (InterruptedException e) {
//              System.err.format("Tickler thread: exiting.\n");
              return;
            } finally {
              ticklerLock.unlock();
            }
          }
          if (!tickling) {
//            System.err.format("Tickler thread: exiting.\n");
            break;
          }
          
          long now = System.nanoTime();
          long nextWakeNanos = now + updateIntervalNanos;
          
          while (tickling && playing && !interrupted()) {
            now = System.nanoTime();
            if (nextWakeNanos <= now) {
//               System.err.format("player thread: refreshing value\n");
              refreshValue();
              int skipped = 0;
              while (nextWakeNanos <= now) {
                nextWakeNanos += updateIntervalNanos;
                skipped++;
              }
              if (skipped > 1) {
//                System.err.format("Tickler thread: missed %d ticks\n", skipped - 1);
              }
            }
            
            try {
//              System.err.format("Tickler thread sleeping %d nanos\n", nextWakeNanos - now);
              nanoSleep(nextWakeNanos - now);
            } catch (InterruptedException e) {
//              System.err.format("Tickler thread: done playing.\n");
              break;
            }
          }
        }
      }
    };
    ticklerLock.lock();
    try {
      tickling = true;
      ticklerStateChange.signal();
    } finally {
      ticklerLock.unlock();
    }
    tickler.start();
  }
  
  public void close() {
    ticklerLock.lock();
    try {
      tickling = false;
      ticklerStateChange.signal();
    } finally {
      ticklerLock.unlock();
    }
    stopThread(tickler);
    tickler = null;
  }

  public boolean running() {
    return playing && receiving;
  }

  public SoundGenerator() {
    // subclasses should set all the values! Example:
    
    sampleRate = 44100;
    samplesPerTenthSecond = (int) (sampleRate / 10);
    bitsPerSample = 16;
    channels = 2;
    signed = true;
    bigEndian = true;

    bytesPerSample = bitsPerSample >> 3;
    bytesPerFrame = bytesPerSample * channels;
    amp = (1 << bitsPerSample - 1) - 1;
    mid = signed ? 0 : amp + 1;

    setBufferLengthMillis(50);
  }
  
  public CircularByteBuffer makeBuffer(int capacity) {
    return CircularByteBuffer.overwritingBuffer(capacity);
  }

  public void setBufferLengthMillis(int newBufferLengthMillis) {
    if (pushingBufferLengthMillis) {
      return;
    }
    
    boolean wasRunning = running();
    if (wasRunning) {
      stop(false, true);
    }
    
    int oldBufferLengthMillis = bufferLengthMillis;
    
    bufferLengthMillis = newBufferLengthMillis;
    bufferSizeFrames = (int) Math.floor(sampleRate * (double) bufferLengthMillis / (double) MILLIS_PER_SECOND);
    bufferSizeBytes = bufferSizeFrames * bytesPerFrame;
//    System.err.format("buffer length %d ms => %d samples == %d bytes\n", 
//        newBufferLengthMillis, bufferSizeFrames, bufferSizeBytes);
    
    updateIntervalNanos = NANOS_PER_MILLI * bufferLengthMillis / 4;

    CircularByteBuffer oldBuffer = buffer;
    buffer = makeBuffer(4 * bufferSizeBytes);
    if (oldBuffer != null) {
      buffer.write(oldBuffer.readAll());
    }
    generationBuffer = new byte[bufferSizeBytes];
    
    pushingBufferLengthMillis = true;
    for (ValueUpdatedListener<Integer> listener : bufferLengthMillisUpdatedListeners) {
      listener.valueUpdated(oldBufferLengthMillis, newBufferLengthMillis);
    }
    pushingBufferLengthMillis = false;

    if (wasRunning) {
      start();
    }
  }

  private static byte getByte(int value, int i) {
    int shift = i << 3;
    return (byte) ((value & 0xff << shift) >> shift);
  }

  public byte[] bytesForValue(double value) {
    int val = mid + (int) Math.round(amp * value * volume);
    byte[] bytes = new byte[bytesPerSample];
    if (bigEndian) {
      for (int i = 0; i < bytesPerSample; i++) {
        bytes[i] = getByte(val, bytesPerSample - 1 - i);
      }
    } else {
      for (int i = 0; i < bytesPerSample; i++) {
        bytes[i] = getByte(val, i);
      }
    }
    return bytes;
  }

  public int fillBuffer(byte[] buffer, int offset, int nFrames, byte[] bytes) {
    if (offset >= buffer.length) {
//      System.err.format("called with offset %d >= buffer.length %d\n", offset, buffer.length);
    }
    int pos = offset;
    int repeats = channels * nFrames;
    for (int i = 0; i < repeats; i++) {
      for (byte element : bytes) {
        buffer[pos++] = element;
      }
    }
    return pos;
  }

  public abstract boolean canGenerateSound();

  // private SineGenerator sineGen = new SineGenerator(440.0, 1.0 /
  // sampleRate);
  public int createBuffer(int nFrames, double value) {
    if (nFrames > 0) {
      int needBytes = nFrames * bytesPerFrame;
      if (needBytes > generationBuffer.length) {
        generationBuffer = new byte[needBytes];
      }
      byte[] bytes = bytesForValue(value);
      fillBuffer(generationBuffer, 0, nFrames, bytes);
      return needBytes;
    } else {
      return 0;
    }
  }

  public double value() {
    return value;
  }
  
  public abstract void pushBufferToDestination();

  public void setValue(double newValue) {
//    System.err.format("changing value from %f to %f\n", value, newValue);
    if (receiving) {
      // write past frames to the buffer
      long now = System.nanoTime();
      long nanosPassed = now - referenceNanos;
      int expectedFramesSinceReference = (int) (nanosPassed * sampleRate / NANOS_PER_SECOND);
      int nFrames = expectedFramesSinceReference - framesSinceReference;
//      System.err.format("after %d nanos, expected %d frames, %d already, generating %d frames\n",
//          nanosPassed, expectedFramesSinceReference, framesSinceReference, nFrames);
      if (nFrames > 0) {
        int nBytes = createBuffer(nFrames, value);
        boolean overran = buffer.write(generationBuffer, 0, nBytes);
        
        if (overran) {
          setBufferLengthMillis((int) Math.floor(1 + bufferLengthMillis * 1.5));
        }

        framesSinceReference += nFrames;
        while (framesSinceReference > samplesPerTenthSecond
            && nanosPassed > NANOS_PER_TENTH_SECOND) {
          referenceNanos += NANOS_PER_TENTH_SECOND;
          framesSinceReference -= samplesPerTenthSecond;
          nanosPassed -= NANOS_PER_TENTH_SECOND;
        }
      }
    } else {
//      System.err.format("not receiving - not writing data to the buffer\n");
    }
    
    if (playing) {
      pushBufferToDestination();
        
      if (buffer.isEmpty()) {
        lockBuffer();
        try {
          signalBufferEmpty();
        } finally {
          unlockBuffer();
        }
      }
    } else {
//      System.err.format("not playing - not reading data from the buffer to the dataLine\n");
    }
    
    // update the current value
    value = Math.max(-1.0, Math.min(1.0, newValue));
  }

  public void refreshValue() {
    setValue(value());
  }
  
  public abstract void startDestination();

  public void start() {
    if (!canGenerateSound()) {
      return;
    }
    if (playing || receiving) {
      return;
    }

    try {
      startDestination();
      
      referenceNanos = System.nanoTime();
      framesSinceReference = 0;
      receiving = true;
      playing = true;
      
      if (Thread.currentThread() != tickler) {
        ticklerLock.lock();
        try {
          ticklerStateChange.signal();
        } finally {
          ticklerLock.unlock();
        }
      }
    } catch (RuntimeException e) {
      e.printStackTrace();
    }

//    System.err.format("%s: SoundGenerator started\n", java.lang.Thread.currentThread().getName());
  }

  private static void stopThread(Thread t) {
    if (t == null) {
      return;
    }
    t.interrupt();
    try {
      t.join();
    } catch (InterruptedException e) {
    }
  }
  
  public abstract void stopDestination(boolean finishPlaying, boolean retainData);

  public void stop(boolean finishPlaying, boolean retainData) {
//    System.err.format("%s: stopping SoundGenerator\n", java.lang.Thread.currentThread().getName());

    receiving = false;
    boolean isTickler = Thread.currentThread() == tickler;

    if (finishPlaying && !isTickler) {
      try {
        lockBuffer();
        awaitBufferEmpty();
      } catch (InterruptedException e) {
        e.printStackTrace();
      } finally {
        unlockBuffer();
      }
    } else if (!retainData) {
      try {
        lockBuffer();
        buffer.clear();
      } finally {
        unlockBuffer();
      }
    }
    
    playing = false;
    if (!isTickler) {
      tickler.interrupt();
    }

    stopDestination(finishPlaying, retainData);
//    System.err.format("%s: SoundGenerator stopped\n", java.lang.Thread.currentThread().getName());
  }

  public void stop() {
    stop(true, false);
  }

  public SMIL.Listener smilListener() {
    return new SMIL.Listener() {
      public void onStart(SMIL smil) {
        start();
      }
      public void onStop(SMIL smil) {
        stop();
      }
    };
  }

  private void lockBuffer() {
    bufferLock.lock();
  }

  private void unlockBuffer() {
    bufferLock.unlock();
  }

  private void awaitBufferEmpty() throws InterruptedException {
    while (!buffer.isEmpty()) {
      bufferEmpty.await();
    }
  }

  private void signalBufferEmpty() {
    bufferEmpty.signalAll();
  }

  public static class Thread extends java.lang.Thread {
    public static void nanoSleep(long nanos) throws InterruptedException {
      sleep(nanos / NANOS_PER_MILLI, (int) (nanos % NANOS_PER_MILLI));
    }
  }

  public double volume() {
    return volume;
  }

  public void setVolume(double newVolume) {
    volume = Math.max(0.0, Math.min(1.0, newVolume));
    refreshValue();
  }

  public void addBufferLengthMillisUpdatedListener(ValueUpdatedListener<Integer> valueUpdatedListener) {
    bufferLengthMillisUpdatedListeners.add(valueUpdatedListener);
  }
  
  public void removeBufferLengthMillisUpdatedListener(ValueUpdatedListener<Integer> valueUpdatedListener) {
    bufferLengthMillisUpdatedListeners.remove(valueUpdatedListener);
  }
}
