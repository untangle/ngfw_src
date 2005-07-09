 /*
  * Copyright (c) 2005 Metavize Inc.
  * All rights reserved.
  *
  * This software is the confidential and proprietary information of
  * Metavize Inc. ("Confidential Information").  You shall
  * not disclose such Confidential Information.
  *
  * $Id:$
  */
 
package com.metavize.tran.mime;

import java.nio.*;
import org.apache.log4j.Logger;

import static com.metavize.tran.util.Ascii.*;
import static com.metavize.tran.util.BufferUtil.*;
import static com.metavize.tran.util.ASCIIUtil.*;

/**
 * Class representing a raw line.  Maintains the terminator
 * character(s) from the original line.  Note that there
 * may not be any terminators on a given line (i.e. if a line
 * is the last in a stream).
 */
public class Line {

  private final ByteBuffer m_buf;
  private final int m_termLen;
  
  
  /**
   * PRE: Although not enforced, the ByteBuffer should
   *      have a backing array 
   * 
   * @param buf the Buffer <b>with</b> any terminating characters within its limit
   */
  public Line(ByteBuffer buf,
    int termLen) {

    m_buf = (ByteBuffer) buf.rewind();
    m_termLen = termLen;
  }
  
  /**
   * Returns a new Line (with shared content)
   * which has no terminator
   */
  public Line removeTerminator() {
    if(m_termLen == 0) {
      return this;
    }
    ByteBuffer newBuf = m_buf.slice();
    newBuf.limit(newBuf.limit() - m_termLen);
    return new Line(newBuf, 0);
  }
  
  /**
   * Get a ByteBuffer representing the bytes of the line.  Callers may modify the
   * limit and position as-per the rules of ByteBuffer.  Any changes
   * to the contents <b>will</b> be seen by other callers of this method (i.e.
   * there is one backing array, but new slices returned by each call).  
   *
   * @param includeTermInView if true, any characters which terminated this
   *        line are also returned within the limit of the buffer.
   * 
   * @return the ByteBuffer with the line, positioned at the start
   *         of the line bytes with the limit set including the 
   *         terminator character(s) if <code>includeTermInView</code> 
   */
  public ByteBuffer getBuffer(boolean includeTermInView) {
    ByteBuffer ret = m_buf.slice();
    if(!includeTermInView) {
      ret.limit(ret.limit() - m_termLen);
    }
    return ret;
  }
  /**
   * Gets the bytes of the line as a ByteBuffer, without 
   * any line terminators within the window.  Equivilant
   * to calling <code>getBuffer(false)</code>
   *
   * @return the ByteBuffer with the line, positioned at the start
   *         of the line bytes with the limit set just before the 
   *         terminator character(s)
   */
  public ByteBuffer getBuffer() {
    return getBuffer(false);
  }
  
  /**
   * The length of a buffer returned
   * from {@link #getBuffer getBuffer(false)}.
   */
  public int bufferLen() {
    return m_buf.remaining();
  }
  
  /**
   * Get the number of characters used in the original line
   * for termination.  May be zero.
   *
   * @return length of terminator character(s)
   */
  public int getTermLen() {
    return m_termLen;
  }

  public boolean bufferStartsWith(String aStr) {
    return startsWith(getBuffer(false), aStr);
  }

  public boolean bufferEndsWith(String aStr) {
    return endsWith(getBuffer(false), aStr);
  }  
  
  public String bufferToString() {
    return bbToString(getBuffer(false));
  }
  


  public static String linesToString(Line[] lines,
    int startingAt,
    boolean unfoldLines) {
    return linesToString(lines, startingAt, Integer.MAX_VALUE, unfoldLines);
  }
  /**
   * Helper method (since I didn't think there was enough of a reason
   * to create a "LineList" or something).
   * <p>
   * This method does assist in changing the
   * position of the first Line.
   * <p>
   * <code>len</code> includes any folding for InternetHeader lines.
   * However, they are not returned in the returned String
   */    
  public static String linesToString(Line[] lines,
    int startingAt,
    int len,
    boolean unfoldLines) {
    
    StringBuilder sb = new StringBuilder();
    int xFered = 0;
    char c;
    
    ByteBuffer bb;
    
    for(int i = 0; i<lines.length; i++) {
      bb = lines[i].getBuffer(true);
//      bb.mark();
      if(i == 0) {
        bb.position(bb.position() + startingAt);
      }
      while(xFered < len && bb.hasRemaining()) {
        c = (char) bb.get();
        xFered++;
        if(unfoldLines && (c == CR || c == LF)) {
          xFered+=eatWhitespace(bb, len - xFered);
          //Nasty hack.  Replace "c" with 
          //a SP, and let the append below pick it up
          c = SP;
          if(xFered >= len) {
 //           bb.reset();
            sb.append(SP);
            return sb.toString();
          }            
        }
        sb.append(c);
      }
//      bb.reset();
      if(xFered >= len) {
        return sb.toString();
      }
    }
    return sb.toString();
    
  }
  
  private static int eatWhitespace(ByteBuffer buf,
    int maxToConsume) {
    int count = 0;
    while (buf.hasRemaining() && (count < maxToConsume)) {
      byte b = buf.get();
      if(!(
        b == CR ||
        b == LF ||
        b == HTAB ||
        b == SP)
        ) {
        buf.position(buf.position()-1);
        break;
      }
      count++;
    }  
    return count;
  }
}