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

import java.nio.ByteBuffer;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.mvvm.tapi.Pipeline;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.tran.token.TokenStreamer;
import com.metavize.tran.token.AbstractParser;
import com.metavize.tran.token.ParseResult;
import com.metavize.tran.util.AsciiCharBuffer;
import org.apache.log4j.Logger;


/**
 * Base class for the ImapClient/ServerParser
 */
abstract class ImapParser
  extends AbstractParser {

  private final Pipeline m_pipeline;
  private final ImapCasing m_parentCasing;
  private final Logger m_logger = Logger.getLogger(ImapParser.class);
  private boolean m_passthru = false;

  ImapParser(TCPSession session,
    ImapCasing parent,
    boolean clientSide) {

    super(session, clientSide);
    m_parentCasing = parent;
    m_pipeline = MvvmContextFactory.context().
      pipelineFoundry().getPipeline(session.id());
  }

  /**
   * Accessor for the parent casing
   */
  protected ImapCasing getImapCasing() {
    return m_parentCasing;
  }

  /**
   * Accessor for the pipeline of this session
   */  
  protected Pipeline getPipeline() {
    return m_pipeline;
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
    getImapCasing().endSession(true);
    return null;
  }

  public ParseResult parseEnd(ByteBuffer chunk) {
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
    int maxTokenSz) {
    if(buf.hasRemaining()) {
      //Note - do not compact, copy instead.  There was an issue
      //w/ the original buffer being passed as tokens (and we were modifying
      //the head).
      ByteBuffer ret = ByteBuffer.allocate(maxTokenSz+1024);
      ret.put(buf);
      return ret;

    }
    else {
      return null;
    }
  }
  
} 