/**
 * 
 */
package com.brunschen.christian.smil.sound;

import com.brunschen.christian.smil.sound.CircularByteBuffer;

import junit.framework.TestCase;


/**
 * @author Christian Brunschen
 *
 */
public class CircularByteBufferTest extends TestCase {
  public void testThrowingCircularByteBuffer() throws Exception {
    CircularByteBuffer cbb = CircularByteBuffer.throwingBuffer(5);
    assertEquals(5, cbb.capacity());
    assertEquals(0, cbb.size());
    assertTrue(cbb.isEmpty());
    
    cbb.write((byte)0x01);
    assertEquals(5, cbb.capacity());
    assertEquals(1, cbb.size());
    assertFalse(cbb.isEmpty());
    
    cbb.write(new byte[]{ (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05 });
    assertEquals(5, cbb.capacity());
    assertEquals(5, cbb.size());
    assertFalse(cbb.isEmpty());
    
    try {
      cbb.write((byte)0x06);
      fail();
    } catch (IndexOutOfBoundsException e) {
      assertEquals(5, cbb.capacity());
      assertEquals(5, cbb.size());
      assertFalse(cbb.isEmpty());
    }
    
    assertEquals((byte)0x01, cbb.read());
    assertEquals(4, cbb.size());
    assertFalse(cbb.isEmpty());
   
    byte[] buf = new byte[3];
    assertEquals(2, cbb.read(buf, 1, 2));
    assertEquals((byte)0x02, buf[1]);
    assertEquals((byte)0x03, buf[2]);
    assertEquals(2, cbb.size());
    assertFalse(cbb.isEmpty());

    assertEquals(2, cbb.read(buf));
    assertEquals((byte)0x04, buf[0]);
    assertEquals((byte)0x05, buf[1]);
    assertEquals(0, cbb.size());
    assertTrue(cbb.isEmpty());
  }
  
  public void testOverwritingCircularByteBuffer() throws Exception {
    CircularByteBuffer cbb = CircularByteBuffer.overwritingBuffer(5);
    assertEquals(5, cbb.capacity());
    assertEquals(0, cbb.size());
    assertTrue(cbb.isEmpty());
    
    assertFalse(cbb.write((byte)0x01));
    assertEquals(5, cbb.capacity());
    assertEquals(1, cbb.size());
    assertFalse(cbb.isEmpty());
    
    assertFalse(cbb.write(new byte[]{ (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05 }));
    assertEquals(5, cbb.capacity());
    assertEquals(5, cbb.size());
    assertFalse(cbb.isEmpty());
   
    assertTrue(cbb.write((byte)0x06));
    assertEquals(5, cbb.capacity());
    assertEquals(5, cbb.size());
    assertFalse(cbb.isEmpty());
    
    assertEquals((byte)0x02, cbb.read());
    assertEquals(4, cbb.size());
    assertFalse(cbb.isEmpty());
    
    byte[] buf = new byte[3];
    assertEquals(2, cbb.read(buf, 1, 2));
    assertEquals((byte)0x03, buf[1]);
    assertEquals((byte)0x04, buf[2]);
    assertEquals(2, cbb.size());
    assertFalse(cbb.isEmpty());

    assertEquals(2, cbb.read(buf));
    assertEquals((byte)0x05, buf[0]);
    assertEquals((byte)0x06, buf[1]);
    assertEquals(0, cbb.size());
    assertTrue(cbb.isEmpty());
 }
  
  public void testExtendingCircularByteBuffer() throws Exception {
    CircularByteBuffer cbb = CircularByteBuffer.extendingBuffer(5);
    assertEquals(5, cbb.capacity());
    assertEquals(0, cbb.size());
    assertTrue(cbb.isEmpty());
    
    assertFalse(cbb.write((byte)0x01));
    assertEquals(5, cbb.capacity());
    assertEquals(1, cbb.size());
    assertFalse(cbb.isEmpty());
    
    assertFalse(cbb.write(new byte[]{ (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05 }));
    assertEquals(5, cbb.capacity());
    assertEquals(5, cbb.size());
    assertFalse(cbb.isEmpty());
    
    assertTrue(cbb.write((byte)0x06));
    assertTrue(cbb.capacity() > 5);
    assertEquals(6, cbb.size());
    assertFalse(cbb.isEmpty());

    assertEquals((byte)0x01, cbb.read());
    assertEquals(5, cbb.size());
    assertFalse(cbb.isEmpty());
    
    byte[] buf = new byte[3];
    assertEquals(2, cbb.read(buf, 1, 2));
    assertEquals((byte)0x02, buf[1]);
    assertEquals((byte)0x03, buf[2]);
    assertEquals(3, cbb.size());
    assertFalse(cbb.isEmpty());

    assertEquals(3, cbb.read(buf));
    assertEquals((byte)0x04, buf[0]);
    assertEquals((byte)0x05, buf[1]);
    assertEquals((byte)0x06, buf[2]);
    assertEquals(0, cbb.size());
    assertTrue(cbb.isEmpty());
  }
  
  public void testWrapAroundWrite() throws Exception {
    CircularByteBuffer cbb = CircularByteBuffer.throwingBuffer(5);

    assertFalse(cbb.write(new byte[]{(byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04}));
    byte[] buf = new byte[3];
    assertEquals(3, cbb.read(buf));
    assertEquals(1, cbb.size());
    assertFalse(cbb.isEmpty());
    assertEquals((byte)0x01, buf[0]);
    assertEquals((byte)0x02, buf[1]);
    assertEquals((byte)0x03, buf[2]);
    
    assertFalse(cbb.write(new byte[]{(byte)0x05, (byte)0x06, (byte)0x07}));
    assertEquals(4, cbb.size());
    assertEquals(3, cbb.read(buf));
    assertEquals(1, cbb.size());
    assertFalse(cbb.isEmpty());
    assertEquals((byte)0x04, buf[0]);
    assertEquals((byte)0x05, buf[1]);
    assertEquals((byte)0x06, buf[2]);
   
    assertEquals((byte)0x07, cbb.read());
    assertEquals(0, cbb.size());
    assertTrue(cbb.isEmpty());
    
    try {
      cbb.read();
      fail();
    } catch (RuntimeException e) {
      assertTrue(e.getMessage().contains("empty"));
    }
  }
}
