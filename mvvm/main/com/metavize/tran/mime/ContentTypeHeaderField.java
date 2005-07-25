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


//===========================================
// Implementation Note.  We're currently
// leaning on the JavaMail API for the 
// heavy lifting (parsing).  We've created
// this wrapper in case we need to move alway
// from JavaMail in the future.
// -wrs 6/05
//===========================================

/**
 * Object representing a "Content-Type" Header as found in an 
 * RFC 821/RFC 2045 document.
 * <br>
 * So this Object is usefull (without a lot of conditional code), if 
 * the type is mal-formed it will return {@link #TEXT_PRIM_TYPE_STR text}/
 * {@link #PLAIN_SUB_TYPE_STR plain} as the 
 * {@link #getPrimaryType primary} and {@link getSubType sub} types
 * respectivly.  
 */
public class ContentTypeHeaderField 
  extends HeaderField {

  private static final String BOUNDARY_PARAM_KEY = "boundary";
  private static final String CHARSET_PARAM_KEY = "charset";  
  
  //Composite primary types
  public static final String MULTIPART_PRIM_TYPE_STR = "multipart";
  public static final String MESSAGE_PRIM_TYPE_STR = "message";    
  
  //discrete primary types
  public static final String TEXT_PRIM_TYPE_STR = "text";  
  public static final String IMAGE_PRIM_TYPE_STR = "image";  
  public static final String AUDIO_PRIM_TYPE_STR = "audio";  
  public static final String VIDEO_PRIM_TYPE_STR = "video";  
  public static final String APPLICATION_PRIM_TYPE_STR = "application";  
  
  //popular subtypes
  public static final String PLAIN_SUB_TYPE_STR = "plain";
  public static final String HTML_SUB_TYPE_STR = "html";  
  public static final String RFC222_SUB_TYPE_STR = "rfc822";
  public static final String MIXED_SUB_TYPE_STR = "mixed";   

  public static final String MULTIPART_MIXED = MULTIPART_PRIM_TYPE_STR + "/" + MIXED_SUB_TYPE_STR;
  public static final String MESSAGE_RFC822 = MESSAGE_PRIM_TYPE_STR + "/" + RFC222_SUB_TYPE_STR;
  public static final String TEXT_PLAIN = TEXT_PRIM_TYPE_STR + "/" + PLAIN_SUB_TYPE_STR;
  

  
  private String m_primaryType;
  private String m_subtype;
  private ParameterList m_paramList;  
 
  public ContentTypeHeaderField() {
    super(HeaderNames.CONTENT_TYPE, HeaderNames.CONTENT_TYPE_LC);
  }  
  public ContentTypeHeaderField(String name) {
    super(name, HeaderNames.CONTENT_TYPE_LC);
  }
  
  /**
   * Get the primary type defined by this header,
   * or {@link #TEXT_PRIM_TYPE_STR text} if either the
   * parsed primary type or {@link #getSubType sub type}
   * were null.
   */
  public String getPrimaryType() {

    return (m_primaryType==null || m_subtype == null)?
      TEXT_PRIM_TYPE_STR:
      m_primaryType;
  }
  
  /**
   * Set the primary type defined by this header
   */
  public void setPrimaryType(String type) {
    m_primaryType = type;
    changed();    
  }  
  
  /**
   * Get the sub type defined by this header,
   * or {@link PLAIN_SUB_TYPE_STR text} if either the
   * parsed {@link #getPrimaryType primary type} or sub type
   * were null.
   */
  public String getSubType() {
    return (m_primaryType==null || m_subtype == null)?
      PLAIN_SUB_TYPE_STR:
      m_subtype;
  }
  
  
  /**
   * Set the subtype defined by this header
   */
  public void setSubType(String subtype) {
    m_subtype = subtype;
    changed();
  }  
  
  /**
   * Equivilant to:
   * <pre>
   * String s = getPrimaryType() + "/" + getSubType()
   * </pre>
   */
  public String getContentType() {
    StringBuilder sb = new StringBuilder();
    sb.append(getPrimaryType());
    sb.append('/');
    sb.append(getSubType());
    return sb.toString();
  }
  
  /**
   * Get the Boundary defined by this header.  This value
   * only applies to multipart sections, and may be null.
   */
  public String getBoundary() {
    return getAttribute(BOUNDARY_PARAM_KEY);
  }
  
  /**
   * Set the Boundary defined by this header.  Note that this
   * method does <b>not</b> implicitly change the type
   * to a composite type just because you set this value
   * (i.e. you must make {@link #setPrimaryType the primary type}
   * {@link #MULTIPART_PRIM_TYPE_STR multipart})
   * <br>
   * Note that the boundary may start with "--", but when used as
   * a boundary two more dashes ("--") will be prepended to each occurance
   * within the content and two more appended to the terminating
   * boundary.
   * <br>
   * Setting this to null causes the Boundary attribute to be
   * removed.
   *
   * @param boundary the boundary.  If null, this "unsets" 
   *        the boundary attribute.
   */
  public void setBoundary(String boundary) {
    setAttribute(BOUNDARY_PARAM_KEY, boundary);
  }
  
  public String getCharset() {
    return getAttribute(CHARSET_PARAM_KEY);
  }
  public void setCharset(String charset) {
    setAttribute(CHARSET_PARAM_KEY, charset);
  }
  
  /**
   * Convienence method which determines if the {@link #getPrimaryType primary type}
   * is multipart.
   * 
   */
  public boolean isMultipart() {
    return getPrimaryType() != null &&
      MULTIPART_PRIM_TYPE_STR.equalsIgnoreCase(getPrimaryType());
  }
  
  /**
   * Convienence method which determines if this is "message/rfc822" 
   * type
   */
  public boolean isMessageRFC822() {
    return getPrimaryType().equalsIgnoreCase(MESSAGE_PRIM_TYPE_STR) &&
      getSubType().equalsIgnoreCase(RFC222_SUB_TYPE_STR);
  }
  
    
  
  @Override  
  protected void parseStringValue() 
    throws HeaderParseException {
    
    ContentType ct = null;
    try {
      ct = new ContentType(getValueAsString());
      if(ct == null) {
        throw new Exception("Null returned from parse");
      }
    }
    catch(Exception pe) {
      throw new HeaderParseException("Cannot parse \"" + 
        getValueAsString() + "\" into ContentType header line", pe);
    }
    
    m_primaryType = ct.getPrimaryType();
    m_subtype = ct.getSubType();
    
    m_paramList = ct.getParameterList();  

    
  }
  
  @Override
  protected void parseLines() 
    throws HeaderParseException {
    
    parseStringValue();
    
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
    sb.append(getPrimaryType());
    sb.append('/');
    sb.append(getSubType());
    if (m_paramList!=null) {
      sb.append(m_paramList.toString(sb.length()));
    }
    return sb.toString();
  }
  
  private String getAttribute(String attribName) {
    return m_paramList == null?
      null:
      m_paramList.get(attribName);    
  }
  
  private void setAttribute(String attribName,
    String val) {
    ensureParamList();
    if(val == null) {
      m_paramList.remove(attribName);
    }
    else {
      m_paramList.set(attribName, val);    
    }
    changed();      
  }
  
  private void ensureParamList() {
    if(m_paramList == null) {
      m_paramList = new ParameterList();
    }
  }    
}  