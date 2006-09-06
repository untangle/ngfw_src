 /*
  * Copyright (c) 2005 Metavize Inc.
  * All rights reserved.
  *
  * This software is the confidential and proprietary information of
  * Metavize Inc. ("Confidential Information").  You shall
  * not disclose such Confidential Information.
  *
  * $Id$
  */
package com.metavize.tran.mime;
import static com.metavize.tran.mime.HeaderNames.*;
import java.util.*;
import java.io.*;


/**
 * <b>Work in progress</b>
 */
public class MIMEPartHeaders 
  extends Headers {

  public MIMEPartHeaders(MIMEPartHeaderFieldFactory factory) {
    super(factory);
  }
  
  public MIMEPartHeaders() {
    super(new MIMEPartHeaderFieldFactory());
  }  
  
  public MIMEPartHeaders(MIMEPartHeaderFieldFactory factory,
    MIMESource source,
    int sourceStart,
    int sourceLen,
    List<HeaderField> headersInOrder,
    Map<LCString, List<HeaderField>> headersByName) {
    
    super(factory, source, sourceStart, sourceLen, headersInOrder, headersByName);
    
  }
  
  
  public ContentTypeHeaderField getContentTypeHF() {
    List<HeaderField> headers = getHeaderFields(CONTENT_TYPE_LC);
    return (ContentTypeHeaderField) ((headers == null || headers.size() == 0)?
      null:
      headers.get(0));
  }
  
  public ContentDispositionHeaderField getContentDispositionHF() {
    List<HeaderField> headers = getHeaderFields(CONTENT_DISPOSITION_LC);
    return (ContentDispositionHeaderField) ((headers == null || headers.size() == 0)?
      null:
      headers.get(0));  
  }
  
  public ContentXFerEncodingHeaderField getContentXFerEncodingHF() {
    List<HeaderField> headers = getHeaderFields(CONTENT_TRANSFER_ENCODING_LC);
    return (ContentXFerEncodingHeaderField) ((headers == null || headers.size() == 0)?
      null:
      headers.get(0));  
  }
  /**
   * Only applies to attachments, but still may be null
   */
  public String getFilename() {
    return getContentDispositionHF()==null?
      null:getContentDispositionHF().getFilename();
  }

  /**
   * Helper method.  Parses the headers from source
   * in one call.
   */
  public static MIMEPartHeaders parseMPHeaders(MIMEParsingInputStream stream,
    MIMESource streamSource)
    throws IOException, 
      InvalidHeaderDataException, 
      HeaderParseException {
    return parseMPHeaders(stream, streamSource, new MIMEPolicy());
  }  

  /**
   * Helper method.  Parses the headers from source
   * in one call.
   */
  public static MIMEPartHeaders parseMPHeaders(MIMEParsingInputStream stream,
    MIMESource streamSource,
    MIMEPolicy policy)
    throws IOException, 
      InvalidHeaderDataException, 
      HeaderParseException {
    return (MIMEPartHeaders) parseHeaders(stream,
      streamSource,
      new MIMEPartHeaderFieldFactory(),
      policy);
  }
  

}
