/**
 * 
 */
package com.brunschen.christian.smil;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Christian Brunschen
 *
 */
public class ValueSynchronizer<T> {
  
  public interface Setter<T> {
    void setValue(T value);
  }

  private Map<Object, Setter<T>> settersByParticipant = new HashMap<Object, Setter<T>>();
  private boolean synchronizing;
  
  public void addParticipant(Object participant, Setter<T> setter) {
    settersByParticipant.put(participant, setter);
  }
  
  public synchronized void setValueFromParticipant(T value, Object source) {
    if (!synchronizing) {
      synchronizing = true;
      for (Map.Entry<Object, Setter<T>> entry : settersByParticipant.entrySet()) {
        if (!entry.getKey().equals(source)) {
          entry.getValue().setValue(value);
        }
      }
      synchronizing = false;
    }
  }
}
