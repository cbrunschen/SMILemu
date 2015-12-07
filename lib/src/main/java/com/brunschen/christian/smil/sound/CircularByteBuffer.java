package com.brunschen.christian.smil.sound;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CircularByteBuffer {
  protected int originalCapacity;
  protected byte[] items;
  protected int capacity;
  protected int nItems;
  protected OnOverrun defaultOnOverrun;

  protected int next;
  protected int first;
  
  public static CircularByteBuffer extendingBuffer(int capacity) {
    return new CircularByteBuffer(capacity, OnOverrun.EXTEND);
  }
    
  public static CircularByteBuffer overwritingBuffer(int capacity) {
    return new CircularByteBuffer(capacity, OnOverrun.OVERWRITE);
  }
    
  public static CircularByteBuffer throwingBuffer(int capacity) {
    return new CircularByteBuffer(capacity, OnOverrun.THROW);
  }
  
  public CircularByteBuffer(int capacity, OnOverrun defaultOnOverrun) {
    this.capacity = originalCapacity = capacity;
    this.defaultOnOverrun = defaultOnOverrun;
    items = new byte[capacity];
    nItems = 0;
    next = first = 0;
  }
  
  public CircularByteBuffer(int capacity) {
    this(capacity, OnOverrun.EXTEND);
  }

  private synchronized void extend(int minCapacity) {
    int newCapacity = capacity + originalCapacity;
    while (newCapacity < minCapacity) {
      newCapacity += originalCapacity;
    }

    //System.err.format("extending circular buffer size from %d to %d\n", capacity, newCapacity);
    byte[] newItems = new byte[newCapacity];
    if (next > first) {
      System.arraycopy(items, first, newItems, 0, next - first);
    } else {
      System.arraycopy(items, first, newItems, 0, capacity - first);
      System.arraycopy(items, 0, newItems, capacity - first, next);
    }
    first = 0;
    next = nItems;
    capacity = newCapacity;
    items = newItems;
  }
  
  public synchronized byte read() {
    if (nItems == 0) {
      throw new RuntimeException("Cannot read from an empty circular buffer");
    }
    byte b = items[first];
    first = (first + 1) % capacity;
    nItems--;
    return b;
  }

  public synchronized int size() {
    return nItems;
  }

  public synchronized int capacity() {
    return capacity;
  }

  public synchronized boolean isEmpty() {
    return nItems == 0;
  }

  public synchronized int read(byte[] buffer, int offset, int len) {
    // System.err.format("want to read %d bytes, %d bytes available\n", len, nItems);
    if (nItems == 0) {
      return 0;
    }

    if (next > first) {
      int n = nItems < len ? nItems : len;
      System.arraycopy(items, first, buffer, offset, n);
      first += n;
      nItems -= n;
      return n;
    } else {
      // first batch - from 'first' to end of buffer
      int nAvailable = capacity - first;
      int n = nAvailable < len ? nAvailable : len;
      System.arraycopy(items, first, buffer, offset, n);
      first = (first + n) % capacity;
      nItems -= n;

      // if necessary, second batch - from 0 to next - using a recursive call
      if (len > n) {
        return n + read(buffer, offset + n, len - n);
      } else {
        return n;
      }
    }
  }

  public int read(byte[] buffer) {
    return read(buffer, 0, buffer.length);
  }

  public synchronized boolean write(byte[] buffer, int offset, int len, OnOverrun onOverrun) { 
    // System.err.format("want to write %d bytes, %d bytes space remaining\n", n, capacity -
    // nItems);
    boolean overran = false;
    if (nItems + len > capacity) {
      overran = true;
      switch (onOverrun) {
        case EXTEND:
          extend(nItems + len);
          break;
        case THROW:
          throw new IndexOutOfBoundsException();
        case OVERWRITE:
          if (len > capacity) {
            // only keep the last capacity bytes, by writing them to the buffer array starting at index 0:
            // adjust nItems to be 0
            nItems = 0;
            // adjust the 'next' and 'first' indexes to be 0 (beginning of the buffer)
            next = first = 0;
            // move the offset forward so only 'capacity' bytes remain
            offset += len - capacity;
            // adjust 'len' to indicate using the full capacity of the buffer
            len = capacity;
          } else {
            int excess = nItems + len - capacity;
            // move the 'first' index forward by the excess
            first = (first + excess) % capacity;
            // and reduce the 'nItems' count by the excess
            nItems -= excess;
          }
          break;
      }
    }
    
    if (next + len <= capacity) {
      System.arraycopy(buffer, offset, items, next, len);
      next = (next + len) % capacity;
    } else {
      int n1 = capacity - next;
      int n2 = len - n1;
      // write the first batch to the end of the buffer array
      System.arraycopy(buffer, offset, items, next, n1);
      // write the second batch to the beginning of the buffer array
      System.arraycopy(buffer, offset + n1, items, 0, n2);
      next = n2;
    }

    nItems += len;
    return overran;
  }

  public synchronized boolean write(byte[] buffer, int offset, int len) { 
    return write(buffer, offset, len, defaultOnOverrun);
  }

  public boolean write(byte[] buffer, OnOverrun onOverrun) {
    return write(buffer, 0, buffer.length, onOverrun);
  }

  public boolean write(byte[] buffer) {
    return write(buffer, defaultOnOverrun);
  }

  public synchronized boolean write(byte b, OnOverrun onOverrun) {
    byte[] buf = new byte[1];
    buf[0] = b;
    return write(buf, 0, 1, onOverrun);
  }

  public synchronized boolean write(byte b) {
    return write(b, defaultOnOverrun);
  }

  public synchronized byte[] readAll() {
    byte[] buffer = new byte[nItems];
    read(buffer, 0, nItems);
    return buffer;
  }
  
  public synchronized void clear() {
    next = first = 0;
    nItems = 0;
  }
  
  public InputStream getInputStream() {
    return new InputStream() {
      @Override
      public int read() {
        return CircularByteBuffer.this.read();
      }
      @Override 
      public int read(byte[] buf, int offset, int len) {
        return CircularByteBuffer.this.read(buf, offset, len);
      }
    };
  }
  
  public OutputStream getOutputStream() {
    return new OutputStream() {
      byte[] buf = new byte[1];
      @Override 
      public void write(byte[] buf, int offset, int len) throws IOException {
        if (buf == null) {
          throw new NullPointerException();
        } else if ((offset < 0) || (offset > buf.length) || (len < 0)
            || ((offset + len) > buf.length) || ((offset + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        try {
          CircularByteBuffer.this.write(buf, offset, len);
        } catch (IndexOutOfBoundsException e) {
          IOException e1 = new IOException(e.getMessage());
          e1.initCause(e);
          throw e1;
        }
      }
      @Override
      public void write(int b) throws IOException {
        buf[0] = (byte)b;
        write(buf, 0, 1);
      }
    };
  }
  
  public static enum OnOverrun {
    THROW,
    EXTEND,
    OVERWRITE
  }
}
