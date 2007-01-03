/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.mail.impl;

import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.tran.token.AbstractCasing;
import java.nio.ByteBuffer;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import com.untangle.mvvm.tapi.event.TCPStreamer;
import com.untangle.tran.mail.impl.CasingTracer;
import java.io.File;


public abstract class AbstractMailCasing
  extends AbstractCasing {

  private final boolean m_trace;
  private final CasingTracer m_tracer;

  private final Logger m_logger =
    Logger.getLogger(AbstractMailCasing.class);    


  public AbstractMailCasing(TCPSession session,
    boolean clientSide,
    String protocolString,
    boolean trace) {
  
    m_trace = trace;

    if(m_logger.isEnabledFor(Level.DEBUG)) {
      m_logger.debug("Creating " +
        (clientSide?"client":"server") + " " + protocolString + " Casing.  Client: " +
        session.clientAddr() + "(" + Integer.toString((int) session.clientIntf()) + "), " +
        "Server: " +
        session.serverAddr() + "(" + Integer.toString((int) session.serverIntf()) + ")");
    }

    if(m_trace) {
      m_tracer = new CasingTracer(
        new File(System.getProperty("user.dir"), protocolString),
        session.id() + "_" + protocolString,
        clientSide);
    }
    else {
      m_tracer = null;
    }    
  }

  /**
   * Test if tracing is enabled for this casing
   */
  public final boolean isTrace() {
    return m_trace;
  }

  /**
   * Callback from either parser or unparser,
   * indicating that we are entering passthru mode.
   * This is required as the passthru token may only
   * flow in one direction.  The casing will ensure that
   * both parser and unparser enter passthru.
   */
  public final void passthru() {
    ((AbstractMailParser) parser()).passthru();
    ((AbstractMailUnparser) unparser()).passthru();
  }

  /**
   * Trace a parse message.  If tracing is not enabled,
   * this call is a noop
   */
  public final void traceParse(ByteBuffer buf) {
    if(m_trace) {
      m_tracer.traceParse(buf);
    }
  }
  /**
   * Trace an unparse message.  If tracing is not enabled,
   * this call is a noop
   */
  public final void traceUnparse(ByteBuffer buf) {
    if(m_trace) {
      m_tracer.traceUnparse(buf);
    }
  }


  /**
   * Wraps a stream for tracing.  If tracing is not enabled,
   * <code>streamer</code> is returned
   */
  public final TCPStreamer wrapUnparseStreamerForTrace(TCPStreamer streamer) {
    if(m_trace) {
      return m_tracer.wrapUnparseStreamerForTrace(streamer);    
    }
    else {
      return streamer;
    }
  }

  public void endSession(boolean calledFromParser) {
    if(m_trace) {
      m_tracer.endSession(calledFromParser);
    }
  }
}