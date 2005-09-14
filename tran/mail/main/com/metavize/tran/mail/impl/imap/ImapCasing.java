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

package com.metavize.tran.mail.impl.imap;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.AbstractCasing;
import com.metavize.tran.token.Parser;
import com.metavize.tran.token.Unparser;
import org.apache.log4j.Logger;
import com.metavize.tran.mail.impl.CasingTracer;
import java.io.File;
import com.metavize.mvvm.tapi.event.TCPStreamer;
import java.nio.ByteBuffer;


/**
 * 'name says it all...
 */
class ImapCasing
  extends AbstractCasing {

  private final Logger m_logger =
    Logger.getLogger(ImapCasing.class);  

  private final ImapParser m_parser;
  private final ImapUnparser m_unparser;

  private static final boolean TRACE = true;
  private final boolean m_trace;
  private final CasingTracer m_tracer;
  
  ImapCasing(TCPSession session,
    boolean clientSide) {

    m_trace = TRACE;

    //This sillyness is to work around some issues
    //with classloaders and logging
    try {
      new com.metavize.tran.mail.papi.imap.CompleteImapMIMEToken(null, null);
    }
    catch(Exception ignore){}

    if(m_trace) {
      m_tracer = new CasingTracer(
        new File(System.getProperty("user.dir"), "imap"),
        session.id() + "_imap",
        clientSide);
    }
    else {
      m_tracer = null;
    }

    m_logger.debug("Created");
    m_parser = clientSide? new ImapClientParser(session, this): new ImapServerParser(session, this);
    m_unparser = clientSide? new ImapClientUnparser(session, this): new ImapServerUnparser(session, this);
  }

  /**
   * Callback from either parser or unparser,
   * indicating that we are entering passthru mode.
   * This is required as the passthru token may only
   * flow in one direction.  The casing will ensure that
   * both parser and unparser enter passthru.
   */
  void passthru() {
    m_parser.passthru();
    m_unparser.passthru();
  }    

  /**
   * Callback from client/server indicating the end of the session.
   */
  void endSession(boolean parser) {
    if(m_trace) {
      m_tracer.endSession(parser);
    }
  }


  public Parser parser() {
    return m_parser;
  }

  public Unparser unparser() {
    return m_unparser;
  }


//============== For Tracing ================

  /**
   * Trace a parse message.  If tracing is not enabled,
   * this call is a noop
   */
  public void traceParse(ByteBuffer buf) {
    if(m_trace) {
      m_tracer.traceParse(buf);
    }
  }
  /**
   * Trace an unparse message.  If tracing is not enabled,
   * this call is a noop
   */
  public void traceUnparse(ByteBuffer buf) {
    if(m_trace) {
      m_tracer.traceUnparse(buf);
    }
  }


  /**
   * Wraps a stream for tracing.  If tracing is not enabled,
   * <code>streamer</code> is returned
   */
  public TCPStreamer wrapUnparseStreamerForTrace(TCPStreamer streamer) {
    if(m_trace) {
      return m_tracer.wrapUnparseStreamerForTrace(streamer);    
    }
    else {
      return streamer;
    }
  }


  
}
