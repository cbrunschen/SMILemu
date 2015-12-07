/**
 * 
 */
package com.brunschen.christian.smil;

/**
 * @author Christian Brunschen
 *
 */
public abstract class Clock<UnitT extends Clock.Unit> {
  
  public abstract void reset();
  public abstract long now();

  public abstract long sleep(long delay, double speedup, boolean doWait);
  public abstract long sleepUntil(long then, double speedup, boolean doWait);

  public long sleep(long delay, double speedup) {
    return sleep(delay, speedup, true);
  }
  
  public long sleepUntil(long then, double speedup) {
    return sleepUntil(then, speedup, true);
  }

  public long sleep(long delay, boolean doWait) {
    return sleep(delay, 1.0, doWait);
  }
  
  public long sleepUntil(long then, boolean doWait) {
    return sleepUntil(then, 1.0, doWait);
  }

  public long sleep(long delay) {
    return sleep(delay, 1.0, true);
  }
  
  public long sleepUntil(long then) {
    return sleepUntil(then, 1.0, true);
  }
  
  public void setWillWait(boolean willWait) {
    // ignore
  }

  public boolean willWait() {
    return true;
  }
  
  public double speedup() {
    return 1.0;
  }
  
  public void setSpeedup(double speedup) {
    // ignore
  }

  public abstract boolean wasInterrupted();
  public abstract InterruptedException interruptedException();
  
  public abstract void interruptSleepers();
    
  public interface Unit {};
  public interface UnitNanosecond extends Unit {};
  public interface UnitTick extends Unit {};
}
