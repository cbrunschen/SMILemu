/**
 * 
 */
package com.brunschen.christian.smil;

/**
 * @author Christian Brunschen
 *
 */
public class TickClock extends Clock<Clock.UnitTick> {
  
  public static final long NANOS_PER_SECOND = 1000000000L; 
  
  Clock<Clock.UnitNanosecond> nanosClock;
  
  double speedup;
  long totalElapsedTicks;
  
  long ticksPerSecond;

  public TickClock(Clock<Clock.UnitNanosecond> nanosClock, long ticksPerSecond) {
    this.nanosClock = nanosClock;
    this.ticksPerSecond = ticksPerSecond;
    this.reset();
  }
  
  @Override
  public void reset() {
    nanosClock.reset();
    resetTotalElapsedTicks();
  }
  
  @Override
  public long now() {
    return totalElapsedTicks;
  }
  
  protected void elapseTicks(long ticks) {
    totalElapsedTicks += ticks;
  }
  
  protected void resetTotalElapsedTicks() {
    totalElapsedTicks = 0;
  }

  @Override
  public long sleep(long delay, double speedup, boolean wait) {
    long delayNanos = delay * NANOS_PER_SECOND / ticksPerSecond;
    nanosClock.sleep(delayNanos, speedup, wait);
    elapseTicks(delay);
    return now();
  }

  @Override
  public long sleepUntil(long then, double speedup, boolean wait) {
    return sleep(then - now(), speedup, wait);
  }

  @Override
  public InterruptedException interruptedException() {
    return nanosClock.interruptedException();
  }

  @Override
  public boolean wasInterrupted() {
    return nanosClock.wasInterrupted();
  }

  @Override
  public void interruptSleepers() {
    nanosClock.interruptSleepers();
  }
    
}
