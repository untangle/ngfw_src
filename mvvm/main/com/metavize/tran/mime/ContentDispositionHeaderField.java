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
import com.metavize.tran.util.ASCIIStringBuilder;
import static com.metavize.tran.util.Ascii.*;
import java.io.*;

//===========================================
// Implementation Note.  We're currently
// leaning on the JavaMail API for the 
// heavy lifting (parsing).  We've created
// this wrapper in case we need to move alway
// from JavaMail in the future.
// -wrs 6/05
//===========================================


//TODO: bscott Make sure we also set the "name" attribute,
//      for compatability with crappy MUAs

/**
 * Object representing a "Content-Disposition" Header as found in an 
 * RFC 821/RFC 2045 document.
 */
public class ContentDispositionHeaderField 
  extends HeaderField {

  private static final String ATTACH_VAL = "attachment";
  private static final String INLINE_VAL = "inline";
  private static final String FILENAME_KEY = "filename";
  
  public enum DispositionType {
    ATTACH,
    INLINE
  }
  
  private DispositionType m_dispType;
  private ParameterList m_paramList;
 
  public ContentDispositionHeaderField(String name) {
    super(name, HeaderNames.CONTENT_DISPOSITION_LC);
  }
  public ContentDispositionHeaderField() {
    super(HeaderNames.CONTENT_DISPOSITION, HeaderNames.CONTENT_DISPOSITION_LC);
  }  

  
  /**
   * Get the DispositionType as defined by this
   * header.  As-per RFC2183, this defaults to
   * "attachment".
   *
   * @return the DispositionType
   */
  public DispositionType getDispositionType() {
    return m_dispType;
  }
  
  /**
   * Set the DispositionType as defined by this
   * header.  Note that converting from ATTACH
   * to INLINE does not implicitly remove
   * the {@link #getFilename filename} attribute.
   *
   * @param type the DispositionType
   */
  public void setDispositionType(DispositionType type) {
    m_dispType = type;
    changed();
  }  
  
  /**
   * Note that an attachment type of "inline" <b>with</b>
   * a FileName is considered an attachment.  This was a bug
   * with some stupid mailer in the past.
   */
  public boolean isAttachment() {

    return m_dispType == DispositionType.ATTACH ||
      getFilename() != null;
  }
  
  /**
   * May be null, even if {@link #isAttachment isAttachment}
   * is true.
   */
  public String getFilename() {
    return m_paramList == null?
      null:
      m_paramList.get(FILENAME_KEY);
  }
  /**
   * Set the filename attribute.  Note that this does
   * <b>not</b> implicitly set the {@link #getDispositionType DispositionType}
   * to ATTACH.  Passing null implicitly removes
   * this parameter.
   *
   * @param filename the name of the file
   */
  public void setFilename(String filename) {
    ensureParamList();
    if(filename == null) {
      m_paramList.remove(FILENAME_KEY);
    }
    else {
      m_paramList.set(FILENAME_KEY, filename);    
    }
    changed();
  }  
  
  
  /**
   * Converts the DispositionType to a String.
   */
  public static String dispositionTypeToString(DispositionType distType) {
    if(distType == null) {
      return "null";
    }
    return distType == ContentDispositionHeaderField.DispositionType.ATTACH?
      ATTACH_VAL:INLINE_VAL;
  }
  
  
  @Override  
  protected void parseStringValue() 
    throws HeaderParseException {
    
    ContentDisposition cd = null;
    try {
      cd = new ContentDisposition(getValueAsString());
      if(cd == null) {
        throw new Exception("Null returned from parse");
      }
    }
    catch(Exception pe) {
      throw new HeaderParseException("Cannot parse \"" + 
        getValueAsString() + "\" into ContentDisposition header line", pe);
    }
    
    String dispString = cd.getDisposition();
    
    
    //RFC2183 Section 2.8 says default to "attachment"
    m_dispType = DispositionType.ATTACH;
    if(dispString != null && INLINE_VAL.equals(dispString.trim().toLowerCase())) {
      m_dispType = DispositionType.INLINE;
    }
    m_paramList = cd.getParameterList();    
    
  }  
  
  @Override
  public void parseLines() 
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
   * for various protocols and MIME
   */  
  public String toString() {
  
    ASCIIStringBuilder sb = new ASCIIStringBuilder();
    sb.append(getName());
    sb.append(COLON);
    sb.append(dispositionTypeToString(getDispositionType()));    
    if(m_paramList != null) {
      sb.append(m_paramList.toString(sb.length()));
    }
    return sb.toString();
   }
  
  private void ensureParamList() {
    if(m_paramList == null) {
      m_paramList = new ParameterList();
    }
  }
}