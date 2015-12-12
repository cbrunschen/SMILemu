/**
 * 
 */
package com.brunschen.christian.smil;

/**
 * @author Christian Brunschen
 *
 */
public class DrumMemory extends Memory {
  
  int rows;
  int currentRow;
  Clock<Clock.UnitTick> clock;

  /**
   * @param rows
   * @param columns
   */
  public DrumMemory(int rows, int columns, int significantBits) {
    super(rows * columns, significantBits);
    this.rows = rows;
    this.currentRow = 0;
  }
  
  public void setClock(Clock<Clock.UnitTick> tickClock) {
    this.clock = tickClock;
  }
  
  private void waitForAddress(int address) {
    if (clock != null) {
      clock.sleep((address + rows - currentRow) % rows);
    }
  }
  
  @Override
  public long read(int address) {
    waitForAddress(address);
    return super.read(address);
  }
  
  @Override
  public void write(int address, long value) {
    waitForAddress(address);
    super.write(address, value);
  }
  
  @Override
  public void write(int address, long value, long mask) {
    waitForAddress(address);
    super.write(address, get(address) & ~mask | value & mask);
  }
  
  @Override
  public void write(int address, long[] values, int offset, int len) {
    waitForAddress(address);
    super.write(address, values, offset, len);
    waitForAddress(address + len);
  }
  
  public TickClock makeTickClock(Clock<Clock.UnitNanosecond> nanosClock, long ticksPerSecond) {
    return new RowClock(nanosClock, ticksPerSecond);
  }

  private final class RowClock extends TickClock {
    private RowClock(Clock<UnitNanosecond> nanosClock, long ticksPerSecond) {
      super(nanosClock, ticksPerSecond);
    }

    @Override
    protected void elapseTicks(long ticks) {
      super.elapseTicks(ticks);
      currentRow = (int) ((currentRow + ticks) % rows);
    }

    @Override
    protected void resetTotalElapsedTicks() {
      super.resetTotalElapsedTicks();
      currentRow = 0;
    }
  }
}
