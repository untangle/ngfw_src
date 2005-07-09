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
import static com.metavize.tran.mime.HeaderNames.*;


/**
 * Subclass of HeaderFieldFactory which adds strong typing 
 * for the following headers:
 * <ul>
 * <li><b>Content-Type</b> via the {@link com.metavize.tran.mime.ContentTypeHeaderField class}</li>
 * <li><b>Content-Disposition</b> via the {@link com.metavize.tran.mime.ContentDispositionHeaderField class}</li>
 * <li><b>Content-Transfer-Encoding</b> via the {@link com.metavize.tran.mime.ContentXFerEncodingHeaderField class}</li>
 * </ul>
 * 
 */
public class MIMEPartHeaderFieldFactory
  extends HeaderFieldFactory {

  @Override  
  protected HeaderField createHeaderField(String mixedCaseName) {


    LCString lcString = new LCString(mixedCaseName);
    
    if(lcString.equals(CONTENT_TYPE_LC)) {
      return new ContentTypeHeaderField(mixedCaseName);
    }
    if(lcString.equals(CONTENT_DISPOSITION_LC)) {
      return new ContentDispositionHeaderField(mixedCaseName);
    }  
    if(lcString.equals(CONTENT_TRANSFER_ENCODING_LC)) {
      return new ContentXFerEncodingHeaderField(mixedCaseName);
    }       
    
    return super.createHeaderField(mixedCaseName);   
  }
  
  @Override   
  protected Headers createHeaders(MIMESource source,
    int sourceStart,
    int sourceLen,
    List<HeaderField> headersInOrder,
    Map<LCString, List<HeaderField>> headersByName) {
    
    return new MIMEPartHeaders(this,
      source,
      sourceStart,
      sourceLen,
      headersInOrder,
      headersByName);
    
  }  
  
}