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

import javax.mail.internet.*;
import javax.mail.*;
import java.util.*;
import java.io.*;
import static com.metavize.tran.util.Ascii.*;


/**
 * Object representing a "Content-transfer-Encoding" Header as found in an 
 * RFC 821/RFC 2045 document.
 */
public class ContentXFerEncodingHeaderField
  extends HeaderField {

  public static final String SEVEN_BIT_STR = "7bit";
  public static final String EIGHT_BIT_STR = "8bit";
  public static final String BINARY_STR = "binary";
  public static final String QP_STR = "quoted-printable";
  public static final String BASE64_STR = "base64";
  public static final String UUENCODE_STR = "uuencode";
  public static final String UUENCODE_STR_ALT = "x-uuencode";  
  public static final String UNKNOWN_STR = BINARY_STR;

  public enum XFreEncoding {
    SEVEN_BIT,
    EIGHT_BIT,
    BINARY,
    QP,
    BASE64,
    UUENCODE,
    UNKNOWN
  };
  
  private XFreEncoding m_encoding;
 
  public ContentXFerEncodingHeaderField(String name) {
    super(name, HeaderNames.CONTENT_TRANSFER_ENCODING_LC);
  }
  public ContentXFerEncodingHeaderField() {
    super(HeaderNames.CONTENT_TRANSFER_ENCODING, HeaderNames.CONTENT_TRANSFER_ENCODING_LC);
  }  

    
  
  @Override  
  protected void parseStringValue() 
    throws HeaderParseException {
    
    String val = getValueAsString();
    
    if(val == null) {
      m_encoding = ContentXFerEncodingHeaderField.XFreEncoding.SEVEN_BIT;
      return;
    }
    
    //Shouldn't have other stuff but....
    if(val.indexOf(";") > 0) {
      val = val.substring(0, val.indexOf(";"));
    }
    val = val.trim().toLowerCase();
    
    if(val.equals(SEVEN_BIT_STR)) {
      m_encoding = ContentXFerEncodingHeaderField.XFreEncoding.SEVEN_BIT;
    }
    else if(val.equals(EIGHT_BIT_STR)) {
      m_encoding = ContentXFerEncodingHeaderField.XFreEncoding.EIGHT_BIT;
    }
    else if(val.equals(BINARY_STR)) {
      m_encoding = ContentXFerEncodingHeaderField.XFreEncoding.BINARY;
    }
    else if(val.equals(QP_STR)) {
      m_encoding = ContentXFerEncodingHeaderField.XFreEncoding.QP;
    }
    else if(val.equals(BASE64_STR)) {
      m_encoding = ContentXFerEncodingHeaderField.XFreEncoding.BASE64;
    }
    else if(val.equals(UUENCODE_STR) || val.equals(UUENCODE_STR_ALT)) {
      m_encoding = ContentXFerEncodingHeaderField.XFreEncoding.UUENCODE;
    }    
    else {
      m_encoding = ContentXFerEncodingHeaderField.XFreEncoding.UNKNOWN;
    }                    
    

  }
  
  public XFreEncoding getEncodingType() {
    return m_encoding;
  }
  public void setEncodingType(XFreEncoding encoding) {
    m_encoding = encoding;
    changed();
  }
  
  @Override
  protected void parseLines() 
    throws HeaderParseException {
    
    parseStringValue();
    
  }
  
  public static String enumToString(ContentXFerEncodingHeaderField.XFreEncoding encoding) {
    switch(encoding) {
      case SEVEN_BIT:
        return SEVEN_BIT_STR;
      case EIGHT_BIT:
        return EIGHT_BIT_STR;
      case BINARY:
        return BINARY_STR;
      case QP:
        return QP_STR;
      case BASE64:
        return BASE64_STR;
      case UUENCODE:
        return UUENCODE_STR;
    }
    return UNKNOWN_STR;
  }
  
  @Override
  public void writeToAssemble(MIMEOutputStream out)
    throws IOException {
    out.write(toString());
    out.writeLine();
  }    
  
  /**
   * Really only for debugging, not to produce output suitable
   * for output.
   */  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getName());
    sb.append(':');
    sb.append(enumToString(getEncodingType()));
    return sb.toString();
  }  
} 