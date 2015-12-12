/**
 * 
 */
package com.brunschen.christian.smil;

import java.io.PrintWriter;
import java.io.StringWriter;

abstract class Operation {
  protected String name;

  public Operation(String name) {
    super();
    this.name = name;
  }

  public abstract void describe(PrintWriter pw, int address, int extras);

  public String describe(int address, int extras) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    describe(pw, address, extras);
    return sw.toString();
  }

  public abstract void perform(int address, int extras);

  public Traceable[] trace(int address, int extras) {
    return new Traceable[] {};
  }
  public Traceable[] traceBefore(int address, int extras) {
    return trace(address, extras);
  }
  public Traceable[] traceAfter(int address, int extras) {
    return trace(address, extras);
  }

}