/**
 * 
 */
package com.brunschen.christian.smil.sound;

/**
 * @author Christian Brunschen
 *
 */
public class ConstantFunction implements Function {
  
  private double value;

  public ConstantFunction(double value) {
    this.value = value;
  }

  public double period() {
    return 1.0;
  }

  public double value(double t) {
    return value;
  }

}
