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
import java.util.*;
import static com.metavize.tran.util.ASCIIUtil.*;
import static com.metavize.tran.util.Ascii.*;
import java.nio.*;


/**
 * Class which creates HeaderFields from raw data (or creates new ones).  This
 * class has been created to let subclasses define further parsing for headers
 * known to be interesting.  The default implementation simply returns
 * the basic HeaderField class.
 */
public class HeaderFieldFactory {


  /**
   * Create and parse a HeaderField object from the given 
   * data.  The default implementation only returns base
   * {@link HeaderField HeaderField} instances.
   *
   * @param mixedCaseName the name as found in original
   *        data (may be mixed case).
   *
   * @param lines lines comprising the value of the header field
   *        <b>including</b> the name and colon.
   *
   * @param valueStartOffset  The offset (within the first Line)
   *        where the value starts.   
   */
/*   
  public HeaderField createAndParse(String mixedCaseName,
    Line[] lines,
    int valueStartOffset) 
    throws HeaderParseException {
    
    HeaderField field = create(mixedCaseName, lines, valueStartOffset);
    field.parse();
    return field;
  }
*/  
  
  /**
   * Create and parse a HeaderField object from the given 
   * data.  The default implementation only returns base
   * {@link HeaderField HeaderField} instances.
   *
   * @param mixedCaseName the name as found in original
   *        data (may be mixed case).
   *
   * @param valueLine lines comprising the value of the header field
   *
   */
/*   
  public HeaderField createAndParse(String mixedCaseName,
    String valueLine) 
    throws HeaderParseException {
    
    HeaderField field = create(mixedCaseName, valueLine);
    field.parseAndAssignFromString(valueLine);
    return field;
  }  
*/  

  /**
   * Create a HeaderField object from the given 
   * data.  The default implementation returns base
   * {@link HeaderField HeaderField} instances.
   *
   * @param mixedCaseName the name as found in original
   *        data (may be mixed case).
   *
   * @param lines lines comprising the value of the header field
   *        <b>including</b> the name and colon.
   *
   * @param valueStartOffset  The offset (within the first Line)
   *        where the value starts.   
   */  
/*   
  protected HeaderField create(String mixedCaseName,
    Line[] lines,
    int valueStartOffset) {

    
    return new HeaderField(mixedCaseName,
      new LCString(mixedCaseName),
      lines,
      valueStartOffset);   
  }
*/  
  
  /**
   * Create and parse a HeaderField object from the given 
   * data.  The default implementation only returns base
   * {@link HeaderField HeaderField} instances.
   *
   * @param mixedCaseName the name as found in original
   *        data (may be mixed case).
   *
   * @param valueLine lines comprising the value of the header field
   *
   */
/*   
  protected HeaderField create(String mixedCaseName,
    String valueLine) {

    
    return new HeaderField(mixedCaseName,
      new LCString(mixedCaseName),
      valueLine);   
  } 
*/  
  
  
  /**
   * TODO: bscott Doc me
   *
   */
  protected HeaderField createHeaderField(String mixedCaseName) {  
    return new HeaderField(mixedCaseName,
      new LCString(mixedCaseName)); 
  }
   
  
  protected Headers createHeaders(MIMESource source,
    int sourceStart,
    int sourceLen,
    List<HeaderField> headersInOrder,
    Map<LCString, List<HeaderField>> headersByName) {
    
    return new Headers(this,
      source,
      sourceStart,
      sourceLen,
      headersInOrder,
      headersByName);
    
  }
  
  
  /**
   * Helper method.  Reads a HeaderFieldName from the Buffer.
   * <p>
   * Returns null if a header name (key) cannot be found, meaning there
   * was nothing before the ":", or there was no ":".  If not found, the Buffer
   * is reset.  Otherwise, the Buffer is advanced past the colon (":") and any
   * LWS
   */
  public static String readHeaderFieldName(ByteBuffer buf) {
    
    buf.mark();
    String headerFieldName = readString(buf,
      (byte) COLON,
      false);
//    System.out.println("[HeaderFieldFactory] readHeaderFieldName: " + 
//    headerFieldName);
    if(headerFieldName == null) {
      buf.reset();
      return null;
    }
    eatWhitespace(buf, false);
    
    return headerFieldName;
  }

}