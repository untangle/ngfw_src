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

package com.metavize.tran.mail.impl.smtp;

import static com.metavize.tran.util.Ascii.*;
import static com.metavize.tran.util.BufferUtil.*;

import com.metavize.tran.mail.papi.smtp.*;

import com.metavize.tran.mail.papi.ByteBufferByteStuffer;

import java.io.*;

import java.nio.ByteBuffer;

import com.metavize.mvvm.*;
import com.metavize.mvvm.tapi.*;
import com.metavize.mvvm.tapi.event.TCPStreamer;
import com.metavize.tran.mail.papi.ByteBufferByteStuffer;
import com.metavize.tran.mail.papi.smtp.*;
import com.metavize.tran.token.*;
import com.metavize.tran.util.*;
import org.apache.log4j.Logger;

/**
 * Base class for the SmtpClient/ServerParser
 */
abstract class SmtpParser
  extends AbstractParser {

  private final Pipeline m_pipeline;
  private final SmtpCasing m_parentCasing;
  private final Logger m_logger = Logger.getLogger(SmtpParser.class);
  private boolean m_passthru = false;
  private CasingSessionTracker m_tracker;

  protected  SmtpParser(TCPSession session,
    SmtpCasing parent,
    CasingSessionTracker tracker,
    boolean clientSide) {
    
    super(session, clientSide);
    m_tracker = tracker;
    m_parentCasing = parent;
    m_pipeline = MvvmContextFactory.context().
      pipelineFoundry().getPipeline(session.id());
  }

  protected SmtpCasing getSmtpCasing() {
    return m_parentCasing;
  }
  protected Pipeline getPipeline() {
    return m_pipeline;
  }
  protected CasingSessionTracker getSessionTracker() {
    return m_tracker;
  }

  /**
   * Is the casing currently in passthru mode
   */
  protected boolean isPassthru() {
    return m_passthru;
  }

  /**
   * Called by the unparser to declare that
   * we are now in passthru mode.  This is called
   * either because of a parsing error by the caller,
   * or the reciept of a passthru token.
   * 
   */
  protected void declarePassthru() {
    m_passthru = true;
    m_parentCasing.passthru();
  }

  /**
   * Called by the casing to declare that this
   * instance should now be in passthru mode.
   */
  protected final void passthru() {
    m_passthru = true;
  }

  public TokenStreamer endSession() {
    m_logger.debug("End Session");
    getSmtpCasing().endSession(isClientSide());
    return null;
  }

  public ParseResult parseEnd(ByteBuffer chunk)
    throws ParseException {
    if (chunk.hasRemaining()) {
      m_logger.debug("data trapped in read buffer: "
        + AsciiCharBuffer.wrap(chunk));
    }
    return new ParseResult();       
  }  

  /**
   * Helper which compacts (and possibly expands)
   * the buffer if anything remains.  Otherwise,
   * just returns null.
   */
  protected static ByteBuffer compactIfNotEmpty(ByteBuffer buf,
    int maxSz) {
    if(buf.hasRemaining()) {
      buf.compact();
      if(buf.limit() < maxSz) {
        ByteBuffer b = ByteBuffer.allocate(maxSz);
        buf.flip();        
        b.put(buf);
        return b;
      }
      return buf;
    }
    else {
      return null;
    }
  }
  
}  