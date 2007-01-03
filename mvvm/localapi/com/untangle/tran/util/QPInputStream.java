/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.untangle.tran.util;

import java.io.*;
import static com.untangle.tran.util.Ascii.*;

/**
 *
 */
public class QPInputStream
  extends FilterInputStream {

  private final byte[] m_buf;
  private int m_queuedSpaces;

  /**
   *
   */
  public QPInputStream(InputStream in) {
    super(new PushbackInputStream(in, 2));
    m_buf = new byte[2];
    m_queuedSpaces = 0;
  }

  public int read()
    throws IOException {
    if(m_queuedSpaces>0) {
      m_queuedSpaces--;
      return SP;
    }
    
    int read = in.read();
    if(read==SP) {
      while ((read = in.read())==SP) {
        m_queuedSpaces++;
      }
      if(
        read==LF || 
        read==CR || 
        read==-1) {
        m_queuedSpaces = 0;
      }
      else {
        ((PushbackInputStream)in).unread(read);
        read = SP;
      }
      return read;
    }
    if(read==EQ) {
      int read2 = super.in.read();
      if(read2==LF) {
        return read();
      }
      if(read2==CR) {
        int peek = in.read();
        if (peek!=LF) {
          ((PushbackInputStream)in).unread(peek);
        }
        return read();
      }
      if(read2==-1) {
        return read2;
      }
      
      m_buf[0] = (byte)read2;
      m_buf[1] = (byte)in.read();
      try {
        return Integer.parseInt(new String(m_buf, 0, 2), 16);
      }
      catch (NumberFormatException e) {
        ((PushbackInputStream)in).unread(m_buf);
      }
      return read;
    }
    else
      return read;
  }


  public int read(byte[] bytes, int off, int len)
    throws IOException {
    int nextInsert = 0;
    try {
      while (nextInsert<len) {
        int c = read();
        if (c==-1) {
          if(nextInsert==0) {
            nextInsert = -1;
          }
          break;
        }
        bytes[off+nextInsert] = (byte)c;
        nextInsert++;
      }
    }
    catch (IOException e) {
      nextInsert = -1;
    }
    return nextInsert;
  }


  /**
   * Mark is not supported, so this method does nothing 
   *
   * @exception IOException from the backing stream
   */       
  @Override       
  public void mark(int readlimit) {
    //Do nothing
  }

  
  
  /**
   * Since marks are not supported, this always throws
   * an exception  
   *
   * @exception IOException (always)
   */         
  @Override     
  public void reset()
    throws IOException {
    throw new IOException("mark not supported");
  }
  
  
  /**
   * Always returns false  
   *
   * @exception IOException from the backing stream
   */         
  @Override     
  public boolean markSupported() {
    return false;
  }  


  @Override
  public int available()
    throws IOException {
    return in.available() + m_queuedSpaces;
  }
  
}