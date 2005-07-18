/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id:$
 */

package com.metavize.tran.util;

import java.nio.ByteBuffer;
import static com.metavize.tran.util.Ascii.*;


/**
 * Utility class to manipulating "stuff" in the ASCII character set.
 * Where Strings are being produced, it is implicit that
 * they are in the ascii characterset.
 * <p>
 * This class is tolerant of different EOL characters.  An EOL
 * can be expressed as one or two characters, and be either CR or
 * LF.  This is the standard for internet protocols.  If two "LF"
 * characters are to be considered two lines (such as reading Unix files)
 * then use of this class is inappropriate (or it needs to be 
 * modified).
 * <p>
 * We use two terms to describe whitespace, loosely based on
 * RFC 822 (there are later RFC's which make whitespace more ambiguious).
 * <code>LWS</code> is defined as a horizontal tab or the space character.
 * "whitespace" is considered LWS and any EOL (see above) characters.
 * <p>
 * We also use the term "EOF" ("End of file") liberally.  When dealing 
 * with A ByteBuffer, EOF means "the end of the buffer".
 */
public final class ASCIIUtil {

  //Ensure this is only a collection of functions
  private ASCIIUtil() {}
  

  
  /**
   * Converts a ByteBuffer to a String
   * <p>
   * The buffer's position is reset to original position
   * after this method completes.
   * 
   * @param buf the buffer.
   * @return the String.  If the buffer is empty, a zero-length
   *         String should be returned.
   */
  public static String bbToString(ByteBuffer buf) {
    buf.mark();
    ASCIIStringBuilder sb = new ASCIIStringBuilder();
    while(buf.hasRemaining()) {
      sb.append(buf.get());
    }
    buf.reset();
    return sb.toString();
  }

  /**
   * Advances the position to endIndexExclusive
   */
  public static String readString(ByteBuffer buf,
    int endIndexExclusive) {
    ByteBuffer dup = buf.duplicate();
    dup.limit(endIndexExclusive);
    buf.position(endIndexExclusive);
    return bbToString(dup);
  }
    
  /**
   * Read an ASCII String from the buffer.  All
   * characters up-to the <code>delim</code> are considered
   * part of the returned String.  Note that the end
   * of the buffer ("EOF") is considered a delimiter.
   * <p>
   * This method advances the position of the buffer upon
   * completion (past what was returned in the String).
   * 
   * @param buf the Buffer
   * @param delim the delimiter byte
   * @param returnDelim if true, the delimiter will be returned
   *        as part of the String token.  Note that (obviously)
   *        EOF cannot be returned.
   */
  public static String readString(ByteBuffer buf,
    byte delim,
    boolean returnDelim) {
    return readString(
      buf,
      delim,
      false,
      false,
      true,
      returnDelim);
  }
  
  /**
   * Read an ASCII String from the buffer.  All
   * characters up-to the terminator are considered
   * part of the returned String.  
   * <p>
   * This method advances the position of the buffer upon
   * completion (past what was returned in the String).
   * 
   * @param buf the Buffer
   * @param delim the delimiter byte (set to 0 for no delimiter)
   * @param isEOLDelim is a line terminator considered a delimiter
   * @param isEOFDelim is EOF a delimiter
   * @param isLWSDelim is LWS a delimiter
   * @param returnDelim if true, the delimiter will be returned
   *        as part of the String token.  If <code>isEOFDelim</code>
   *        is false and the end of the buffer is reached without
   *        another delimiter, null is returned.
   */  
  public static String readString(ByteBuffer buf,
    byte delim,
    boolean isEOLDelim,
    boolean isEOFDelim,
    boolean isLWSDelim,
    boolean returnDelim) {
    
    ASCIIStringBuilder sb = new ASCIIStringBuilder();
    
    buf.mark();
    byte b;
    while(buf.hasRemaining()) {
      b = buf.get();
      if(isEOL(b)) {
        if(isEOLDelim) {
          if(returnDelim) {
            sb.append(b);
            transferWhitespace(buf, sb, isEOLDelim, isLWSDelim);
          }
          return sb.toString();
        }
      }
      if(isLWS(b)) {
        if(isLWSDelim) {
          if(returnDelim) {
            sb.append(b);
            transferWhitespace(buf, sb, isEOLDelim, isLWSDelim);
          }
          return sb.toString();
        }      
      }
      if(b == delim) {
        if(returnDelim) {
          sb.append(b);
        }
        return sb.toString();
      }
      sb.append(b);
    }
    
    //If we're here, we ran out of bytes
    if(isEOFDelim) {
      return sb.toString();
    }
    buf.reset();
    return null;
    
  }
  
  /**
   * Is the given byte a CR or LF
   */
  public static boolean isEOL(byte b) {
    return b == CR || b == LF;
  }
  
  /**
   * Is the given byte a HTAB or space
   */
  public static boolean isLWS(byte b) {
    return b == SP || b == HTAB;
  }
  
  /**
   * Would the next call to "get" on the buffer
   * return a {@link #isLWS LWS} character.
   *
   * @param buf the buffer
   * @return true if a LWS character, false if not
   *         or the buffer is empty.
   */
  public static boolean isNextLWS(ByteBuffer buf) {
    if(buf.hasRemaining()) {
      if(isLWS(buf.get())) {
        buf.position(buf.position() - 1);
        return true;
      }
      else {
        buf.position(buf.position() - 1);
        return false;
      }
    }
    return false;
  }
  
  /**
   * Checks for a "blank" line, meaning the contents of the buffer
   * is only LWS.
   */
  public static boolean isAllLWS(ByteBuffer buf) {
    buf.mark();
    while(buf.hasRemaining()) {
      if(!isLWS(buf.get())) {
        buf.reset();
        return false;
      }
    }
    buf.reset();
    return true;
  }
  
  
  /**
   * Transfer whitespace from the given buffer
   * to the ASCIIStringBuilder.  The position
   * of the buffer is advanced for all bytes 
   * transferred.
   *
   * @param buf the source buffer
   * @param sb the target
   * @param isEOLWhitespace are line terminators considered 
   *        for transfer.  If not, they mark the end of the 
   *        transfer.
   * @param isLWSWhitespace are LWS characters considered 
   *        for transfer.  If not, they mark the end of the 
   *        transfer.
   */
  public static void transferWhitespace(ByteBuffer buf,
    ASCIIStringBuilder sb,
    boolean isEOLWhitespace,
    boolean isLWSWhitespace) {
    
    byte b;
    while(buf.hasRemaining()) {
      b = buf.get();
      if(isEOL(b)){
        if(isEOLWhitespace) {
          sb.append(b);
        }
        else {
          buf.position(buf.position() - 1);
          return;
        }
      }
      if(isLWS(b)) {
        if(isLWSWhitespace) {
          sb.append(b);     
        }
        else {
          buf.position(buf.position() - 1);
          return;
        }
      }
    }
  }
  
  /**
   * Consumes whitespace from the buffer, advancing its position.
   *
   * @param buf the buffer
   * @param isEOLWhitespace If true, EOL is considered whitespace
   *
   * @return the number of bytes consumed.
   */
  public static int eatWhitespace(ByteBuffer buf,
    boolean isEOLWhitespace) {
    
    return eatWhitespace(buf, isEOLWhitespace, Integer.MAX_VALUE);
  }      
    
  /**
   * Consumes whitespace from the buffer, advancing its position.
   *
   * @param buf the buffer
   * @param isEOLWhitespace If true, EOL is considered whitespace
   * @param maxToConsume the max bytes to consume.
   *
   * @return the number of bytes consumed, not exceeding
   *         <code>maxToConsume</code>
   */  
  public static int eatWhitespace(ByteBuffer buf,
    boolean isEOLWhitespace,
    int maxToConsume) {
    
    int count = 0;
    while (buf.hasRemaining() && (count < maxToConsume)) {
      byte b = buf.get();
      if(!(
        isLWS(b) ||
        (isEOLWhitespace && isEOL(b))
        )) {
        buf.position(buf.position()-1);
        break;
      }
      count++;
    }  
    return count;
  }        
  
}