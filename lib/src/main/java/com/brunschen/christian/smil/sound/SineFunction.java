package com.brunschen.christian.smil.sound;

public class SineFunction implements Function {
  public double f;

  public SineFunction(double f) {
    this.f = f;
  }

  public double period() {
    return 2.0 * Math.PI / f;
  }

  public double value(double t) {
    return Math.sin(f * t);
  }
}
