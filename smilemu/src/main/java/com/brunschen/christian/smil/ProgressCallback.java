/**
 * 
 */
package com.brunschen.christian.smil;

class ProgressCallback {
  int total;
  int done = 0;

  public void starting(int n) {
    total = n;
  }

  public void starting(String filename) {
  }

  public void finished(String filename) {
    done++;
  }

  public void exception(String filename, Exception e) {
    finished(filename);
  }

  public void finished() {
  }
}