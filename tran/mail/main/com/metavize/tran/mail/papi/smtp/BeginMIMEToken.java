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

package com.metavize.tran.mail.papi.smtp;

import java.nio.ByteBuffer;
import org.apache.log4j.Logger;
import com.metavize.tran.mime.*;
//If these lines are uncommented, some code generator
//based on doclet barfs. - wrs
//import com.metavize.tran.mime.MIMEMessageHeaders;
//import com.metavize.tran.mime.FileMIMESource;
import java.io.IOException;
import com.metavize.tran.token.Token;


/**
 * Token reprsenting the Begining
 * of a MIME message.
 * <br>
 * Warning - This is not a "Metadata" token.  It will
 * write-out.  Make sure not to duplicate headers
 * by passing this token along by accident.
 */
public class BeginMIMEToken
  implements Token {

  private static final ByteBuffer EMPTY_BUFFER =
    ByteBuffer.allocate(0);

  private final Logger m_logger =
    Logger.getLogger(BeginMIMEToken.class);

  private MIMEMessageHeaders m_headers;
  private FileMIMESource m_mimeSource;

  public BeginMIMEToken(MIMEMessageHeaders headers,
    FileMIMESource mimeSource) {
    m_headers = headers;
    m_mimeSource = mimeSource;
  }
  
  /**
   * Get the Headers of the MIME message
   */
  public MIMEMessageHeaders getHeaders() {
    return m_headers;
  }

  /**
   * Get the FileMIMESource, which holds
   * the Headers as well as any accumulated
   * portions of the body.
   */
  public FileMIMESource getMIMESource() {
    return m_mimeSource;
  }

  /**
   * Method returns the bytes of the header.
   */
  public ByteBuffer getBytes() {
    try {
      return m_headers == null?
        EMPTY_BUFFER:
        m_headers.toByteBuffer();
    }
    catch(IOException ex) {
      m_logger.error("Unable to write Headers Buffer", ex);
      return EMPTY_BUFFER;
    }
  }
}