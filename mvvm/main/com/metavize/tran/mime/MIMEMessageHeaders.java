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
import static com.metavize.tran.mime.HeaderNames.*;
import java.util.*;


/**
 * <b>Work in progress</b>
 */
public class MIMEMessageHeaders 
  extends MIMEPartHeaders {

  public MIMEMessageHeaders(MailMessageHeaderFieldFactory factory,
    MIMESource source,
    int sourceStart,
    int sourceLen,
    List<HeaderField> headersInOrder,
    Map<LCString, List<HeaderField>> headersByName) {
    
    super(factory, source, sourceStart, sourceLen, headersInOrder, headersByName);
    
  }

  

}