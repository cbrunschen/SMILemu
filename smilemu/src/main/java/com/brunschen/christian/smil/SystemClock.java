/**
 * 
 */
package com.brunschen.christian.smil;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * @author Christian Brunschen
 *
 */
public class SystemClock extends Clock<Clock.UnitNanosecond> {
  
  // used to convert nanoseconds to milliseconds for calling Thread.sleep(millis, nanos)
  public static final int NANOS_PER_MILLI = 1000000;
  
  private Set<Thread> sleepers = new HashSet<Thread>();
  private Queue<Listener> listeners = new LinkedList<Listener>();
  private boolean wasInterrupted = false;
  private InterruptedException lastException = null;
  private long nominalNanoTime;
  private long maxNanosAhead;
  
  public SystemClock(long maxNanosAhead) {
    this.maxNanosAhead = maxNanosAhead;
  }
  
  public SystemClock() {
    this(50000000L); // by default, allow the nominal time to run max 50 ms ahead of the actual time
  }
  
  public long actualTime() {
    return System.nanoTime();
  }

  public long now() {
    return nominalNanoTime;
  }
  
  protected void spendTime(long now, long then) {
    for (Listener listener : listeners) {
      listener.onSpareTime(now, then);
    }
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

  @Override
  public long sleep(long delay, double speedup, boolean doWait) {
    return sleepUntil(nominalNanoTime + delay, speedup, doWait);
  }

  @Override
  public long sleepUntil(long then, double speedup, boolean doWait) {
    if (!doWait) {
      return nominalNanoTime;
    }
    
    // calculate the delay, and adjust the target nominal time, according to the speedup
    long delay = Math.round((then - nominalNanoTime) / speedup);
    nominalNanoTime += delay;
    
    long actualNanoTime = actualTime();
    delay = nominalNanoTime - actualNanoTime - maxNanosAhead;

    if (delay > 0) {
      // if we have some time left over, we might as well try to spend it
      spendTime(actualNanoTime, nominalNanoTime);
      actualNanoTime = actualTime();
      delay = nominalNanoTime - actualNanoTime - maxNanosAhead;

      if (delay > 0) {
        // even after trying to spend some time, we still have some time left, so we sleep
        
        synchronized(sleepers) {
          sleepers.add(Thread.currentThread());
        }
        try {
          Thread.sleep(delay / NANOS_PER_MILLI, (int) (delay % NANOS_PER_MILLI));
        } catch (InterruptedException e) {
          wasInterrupted = true;
          lastException = e;
        }
        synchronized(sleepers) {
          sleepers.remove(Thread.currentThread());
        }
      }
    }
    
    return nominalNanoTime;
  }

  public double speedup() {
    return 1.0;
  }

  @Override
  public void reset() {
    wasInterrupted = false;
    lastException = null;
    nominalNanoTime = System.nanoTime();
  }

  public boolean wasInterrupted() {
    return wasInterrupted;
  }
  
  public InterruptedException interruptedException() {
    return lastException;
  }
  
  public void interruptSleepers() {
    synchronized(sleepers) {
      for(Thread sleeper : sleepers) {
        sleeper.interrupt();
      }
    }
  }
  
  public interface Listener {
    void onSpareTime(long now, long until);
  }
}
