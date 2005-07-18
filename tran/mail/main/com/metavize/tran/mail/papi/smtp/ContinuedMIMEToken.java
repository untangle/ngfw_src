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
 * Token which follows a {@link com.metavize.tran.mail.BeginMIMEToken BeginMIMEToken}.
 * There may be one or more ContinuedMIMETokens after the begin token.  The last
 * ContinuedMIMEToken can be detected via the {@link #isLast isLast} property.
 */
public class ContinuedMIMEToken
  extends Chunk {

  private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);

  public static final ContinuedMIMEToken EMPTY_BODY =
    new ContinuedMIMEToken(EMPTY_BUFFER, true, true);

  private final Logger m_logger = Logger.getLogger(ContinuedMIMEToken.class);

  private final boolean m_isLast;
  private final boolean m_isMessageBlank;

  private ContinuedMIMEToken(ByteBuffer buf,
    boolean isLast,
    boolean blankMessage) {
    super(buf);
    m_isLast = isLast;
    m_isMessageBlank = blankMessage;
  }

  public ContinuedMIMEToken(ByteBuffer buf,
    boolean isLast) {
    this(buf, isLast, false);
  }

  /**
   * Does this chunk represent the past in a series
   * of MIME messages
   */
  public boolean isLast() {
    return m_isLast;
  }
  
  /**
   * Only applies for the first (and only!)
   * chunk of a MIME message, where there
   * was no body.  If this is true
   * then {@link #isLast isLast} is also true.
   */
  public boolean isMessageBlank() {
    return m_isMessageBlank;
  }
}