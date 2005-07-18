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

import static com.metavize.tran.util.Ascii.*;
import static com.metavize.tran.util.BufferUtil.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.List;

import com.metavize.mvvm.*;
import com.metavize.mvvm.tapi.*;
import com.metavize.tran.token.*;
import com.metavize.tran.util.*;
import org.apache.log4j.Logger;
import com.metavize.tran.mime.*;


/**
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

  public MIMEMessageHeaders getHeaders() {
    return m_headers;
  }
  public FileMIMESource getMIMESource() {
    return m_mimeSource;
  }
  
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