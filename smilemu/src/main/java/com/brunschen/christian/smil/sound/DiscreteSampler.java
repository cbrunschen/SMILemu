/**
 *
 */
package com.brunschen.christian.smil.sound;

class DiscreteSampler {
  private Function function;
  private double t;
  private double dt;

  public DiscreteSampler(Function function, double f) {
    t = 0.0;
    this.function = function;
    dt = 1.0 / f;
  }

  public double next() {
    double value = function.value(t);
    t += dt;
    double period = function.period();
    if (period > 0.0) {
      while (t > period) {
        t -= period;
      }
    }
    return value;
  }
}