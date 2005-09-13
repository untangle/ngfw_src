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
import com.metavize.mvvm.tapi.event.TCPStreamer;
import com.metavize.tran.token.AbstractUnparser;
import com.metavize.tran.token.ParseResult;
import com.metavize.tran.util.AsciiCharBuffer;
import org.apache.log4j.Logger;

/**
 * Base class for the ImapClient/ServerUnparser
 */
abstract class ImapUnparser
  extends AbstractUnparser {

  private final Pipeline m_pipeline;
  private final ImapCasing m_parentCasing;  
  private final Logger m_logger = Logger.getLogger(ImapUnparser.class);
  private boolean m_passthru = false;

  protected ImapUnparser(TCPSession session,
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

  public TCPStreamer endSession() {
    m_logger.debug("End Session");
    getImapCasing().endSession(false);
    return null;
  }  
  
}  