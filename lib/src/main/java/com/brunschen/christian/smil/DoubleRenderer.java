/**
 * 
 */
package com.brunschen.christian.smil;

import javax.swing.table.DefaultTableCellRenderer;

class DoubleRenderer extends DefaultTableCellRenderer {
  public static final long serialVersionUID = 0L;

  public DoubleRenderer() {
    super();
  }

  public void setValue(Object value) {
    setText(value == null || !(value instanceof Number) ? "" : String.format("% 13.16f", ((Number) value)
        .doubleValue()));
  }
}