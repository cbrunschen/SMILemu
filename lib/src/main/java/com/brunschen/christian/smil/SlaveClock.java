/**
 * 
 */
package com.brunschen.christian.smil;

/**
 * @author Christian Brunschen
 *
 */
public class SlaveClock<UnitT extends Clock.Unit> extends Clock<UnitT> {

  Clock<UnitT> masterClock;
  double speedup = 1.0;
  boolean willWait = true;
  
  public SlaveClock(Clock<UnitT> masterClock) {
    this.masterClock = masterClock;
  }

  public long now() {
    return masterClock.now();
  }

  /**
   * @return the masterClock
   */
  public Clock<UnitT> masterClock() {
    return masterClock;
  }

  /**
   * @param masterClock the masterClock to set
   */
  public void setMasterClock(Clock<UnitT> masterClock) {
    this.masterClock = masterClock;
  }

  @Override
  public long sleep(long delay, double speedup, boolean doWait) {
    return masterClock.sleep(delay, this.speedup * speedup, doWait && willWait);
  }

  @Override
  public long sleepUntil(long then, double speedup, boolean doWait) {
    return masterClock.sleepUntil(then, this.speedup * speedup, doWait && willWait);
  }

  public double speedup() {
    return speedup;
  }
  
  public void setSpeedup(double speedup) {
    this.speedup = speedup;
  }
  
  public void setWillWait(boolean willWait) {
    this.willWait = willWait;
  }
  
  public boolean willWait() {
    return willWait;
  }
  
  @Override
  public void reset() {
    masterClock.reset();
  }

  @Override
  public InterruptedException interruptedException() {
    return masterClock.interruptedException();
  }

  @Override
  public boolean wasInterrupted() {
    return masterClock.wasInterrupted();
  }

  @Override
  public void interruptSleepers() {
    masterClock.interruptSleepers();
  }
  
}
