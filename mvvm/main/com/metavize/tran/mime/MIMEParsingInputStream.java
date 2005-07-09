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
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.IOException;
import com.metavize.tran.util.*;
import java.nio.*;
import java.util.*;

import static com.metavize.tran.util.Ascii.*;
import static com.metavize.tran.util.ASCIIUtil.*;
import com.metavize.tran.util.ByteBufferBuilder;


/**
 * Specialized Stream with methods useful for 
 * parsing MIME messages.  It defines a "line" as
 * being terminated by CRLF, CR, or LF.  The
 * sequences "CRCR" and "LFLF" would be considered
 * two lines.
 * <p>
 * For the MIME classes, this class supports querying
 * for the relative position within the larger stream via
 * the {@link #position position() method}.  Note that 
 * calling the {@link #skip skip method} affects the count.
 * <p>
 * This stream has support for <i>unreading</i>.  This is implemented
 * by pushing bytes back, such that subsequent reads will see 
 * the pushed-back bytes (like the JavaSoft "PushbackInputStream").
 */
public class MIMEParsingInputStream extends InputStream {

  private static final int LINE_SZ = 1024;

  private final DynPushbackInputStream m_wrapped;
  
  //A bit optimistic on the system to read more than
  //2 gigs, but since the Java APIs let you skip
  //with a long we should count that as well
  private long m_count = 0;
  
  
  /**
   * Construct a new MIMEParsingInputStream, wrapping the
   * given stream.  Note that this class does not do any
   * buffering, so if the underlying stream is to a file
   * it should be buffered.
   */
  public MIMEParsingInputStream(InputStream wrap) {
    m_wrapped = new DynPushbackInputStream(wrap, LINE_SZ, LINE_SZ);
  }
  
  /**
   * The current position (or "count").  This is the number
   * of bytes consumed from the underlying
   * wrapped stream.  Note that any bytes pushed-back
   * are not considered (i.e. unreading moves the
   * pointer "back").
   *
   * @return the position (num bytes read)
   */
  public long position() {
    return m_count;
  }
  
  /**
   * Unread the byte by placing it back into the
   * stream for the next call to {@link #read read}.
   * 
   * @param b the byte to be placed back-into the stream
   *
   * @exception IOException from the backing stream
   */  
  public void unread(int b) 
    throws IOException {
    m_wrapped.unread(b);
    m_count--;
  }
  
  
  /**
   * Unread the byte sequence by placing it back into the
   * stream for the next call to {@link #read read}.
   * 
   * @exception IOException from the backing stream
   */         
  public void unread(byte[] b)
    throws IOException {
    m_wrapped.unread(b);
    m_count-=b.length;
  }      
  
  
  /**
   * Unread the byte sequence by placing it back into the
   * stream for the next call to {@link #read read}.
   * 
   * @exception IOException from the backing stream
   */         
  public void unread(byte[] b, int off, int len) 
    throws IOException {
    m_wrapped.unread(b, off, len);
    m_count-=len;
  }            
  
  
  @Override       
  public int read() 
    throws IOException {
    int ret = m_wrapped.read();
    if(ret >= 0) {
      m_count++;
    }
    return ret;
  }
  
  
  @Override       
  public int read(byte[] b)
    throws IOException {
    int ret = m_wrapped.read(b);
    if(ret > 0) {
      m_count+=ret;
    }        
    return ret;
  }      
  
  
  @Override        
  public int read(byte[] b, int off, int len) 
    throws IOException {
    int ret = m_wrapped.read(b, off, len);
    if(ret > 0) {
      m_count+=ret;
    }        
    return ret;         
  }  
  
  
  /**
   * Reads a line from the underlying data source. 
   * <p>
   * If an EOF is encountered before a line terminator, 
   * the remaining bytes are considered a line and a Line is returned
   * with a zero-length {@link Line#getTermLen terminator}.  If an EOF
   * is encountered as the first byte, null is returned.  If 
   * only a terminator is encountered, a Line with a zero-length
   * {@link Line#getBuffer ByteBuffer} is returned.
   *
   * @param maxLen the maximum length to read before throwing
   *        LineTooLongException
   *
   * @return a Line or null (see desc above).
   *
   * @exception LineTooLongException if the line exceeds 
   *            <code>maxLen</code>
   * @exception IOException from the backing stream
   */
  public Line readLine(int maxLen) 
    throws IOException, LineTooLongException {
    
    byte b;
    int count = 0;
    ByteBufferBuilder bb = new ByteBufferBuilder(
      LINE_SZ, 
      ByteBufferBuilder.GrowthStrategy.INCREMENTAL);
      
          
    int read = read();
    count++;

    while(read >= 0 /*0 isn't legal, but...*/ && count < maxLen) {
      b = (byte) read;
      bb.add(b);
      count++;
      if(b == CR) {
        //We read a CR.  Check if the next is an LF
        read = read();
        if(read >= 0) {
          //Not EOF.  Check if the next byte was an LF
          if((byte) read == LF) {
            //Add the LF, return with a two-byte EOL
            bb.add((byte) read);
            return new Line(bb.toByteBuffer(), 2);
          }
          //Not a LF.  Put it back
          unread(read);
          return new Line(bb.toByteBuffer(), 1);
        }
        //Return from EOF
        return new Line(bb.toByteBuffer(), 1);
      }
      //We permit bare LFs
      if(b == LF) {
        return new Line(bb.toByteBuffer(), 1);
      }
      read = read();
    }
    if(count >= maxLen) {
      throw new LineTooLongException(maxLen);
    }
    return bb.size() == 0?
      null:
      new Line(bb.toByteBuffer(), 0);      
  }

  /**
   * Read a Line of unlimited length
   *
   * @return a Line
   *
   * @exception LineTooLongException if the line exceeds the
   *            ability to buffer (~2 Gigs).
   * @exception IOException from the backing stream
   */
  public Line readLine()
    throws IOException, LineTooLongException {
    
    return readLine(Integer.MAX_VALUE);
  }
  
  /**
   * Unreads the line w/ terminator.  
   *
   * @param line the line to unread
   */
  public void unreadLine(Line line) 
    throws IOException {
    ByteBuffer buf = line.getBuffer(true);
    unread(buf.array(), buf.arrayOffset() + buf.position(), buf.remaining());
  }
  
  /**
   * Returns null boundary was not found and 
   * <code>isEOFTerminator</code> is false.
   */
  public List<Line> readLinesTillBoundary(String boundaryStr,
    boolean leaveBoundary,
    boolean isEOFTerminator,
    int maxLineLen) 
    throws IOException, LineTooLongException {
    
    String dashDashBoundary = "--" + boundaryStr;
    
    List<Line> lines = new ArrayList<Line>();
    
    Line aLine = readLine(maxLineLen);
    while(aLine != null) {
      if(aLine.bufferStartsWith(dashDashBoundary)) {
        if(lines.size() > 0) {
          lines.add(lines.size()-1, lines.get(lines.size()-1).removeTerminator());
        }      
        if(leaveBoundary) {
          unreadLine(aLine);
        }
        return lines;
      }
      lines.add(aLine);
    }
    
    return isEOFTerminator?
      lines:
      null;
  }
  
  
  /**
   * Helper method for MIME parsing.
   * Advance the position of this stream to a boundary.  After this
   * method is called, the stream will be positioned either at the start
   * or end of the boundary (depending on the value passed for
   * <code>leaveBoundary</code>).  When "at the start of the boundary",
   * this means after the CRLF which begun the boundary line.
   * <p>
   * Returns false if EOF is encountered before end of pattern, and 
   * <code>isEOLTerminator</code> is false.  
   * 
   * <p>
   * PRE: Stream is advanced to a new line.
   * 
   * @param boundary <b>without</b> leading "--" or trailing "--"
   * @param leaveBoundary should the boundary be left in the stream
   * @param isEOFTerminator should EOF be considered a terminator
   *
   * @exception IOException from the backing stream
   */         
  public boolean advanceToBoundary(String boundaryStr,
    boolean leaveBoundary,
    boolean isEOFTerminator) 
    throws IOException {  
    
    final byte[] matchPattern = new StringBuilder().
      append('-').
      append('-').
      append(boundaryStr).
      toString().getBytes();
    
    final int matchPatternLen = matchPattern.length;

    int read = read();
    
    int candidatePos = 0;//In case someone positioned us after a EOL,
                         //start at "0" instead of "-1"
                     
    
    while(read >= 0) {
      if(candidatePos == -1) {
        //-1 means "not starting search yet"
        if((char) read == CR) {
          int read2 = read();
          if(read2 < 0) {
            return isEOFTerminator;
          }
          if((char) read2 != LF) {
            //We'll permit a bare CR
            unread(read2);
          }
          candidatePos = 0;
        }
        else if((char) read == LF) {
          candidatePos = 0;
        }
        read = read();
        continue;
      }
      //If we're here, then we're scanning a candidate
      if(matchPattern[candidatePos++] == (byte) read) {
        if(candidatePos+1 == matchPatternLen) {
          if(leaveBoundary) {
            unread(matchPattern);    
          }
          return true;
        }
        read = read();
      }
      else {
        //Fell out of the candidate.  Let the byte be re-evaluated (it may be a CR/LF)
        candidatePos = -1;
      }
    }
    return  isEOFTerminator;
  }
  

  
  /**
   * Advances past the next EOL sequence (CRLF 
   * CR, or LF), or EOF
   *
   * @exception IOException from the backing stream
   */         
  public void advanceToNextLine()
    throws IOException {
    int b = read();
    while(b >= 0) {
      if(isEOL((byte) b)) {
        if(b == CR) {
          b = read();
          if(b >=0 && b != LF) {
            unread(b);
          }
        }
        return;
      }
      b = read();
    }
  }
  
  /** 
   * Skips to the end of the file.
   *
   * @exception IOException from the backing stream
   */         
  public void advanceToEOF()
    throws IOException {
    while(read() >= 0);
  }
 
  
  
  
  @Override       
  public long skip(long n)
    throws IOException {
    long ret = m_wrapped.skip(n);
    m_count+=ret;
    return ret;
  }
  
  
  @Override        
  public int available() throws IOException {
    return m_wrapped.available();
  }
  
  
  @Override      
  public void close()
    throws IOException {
    m_wrapped.close();
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
}