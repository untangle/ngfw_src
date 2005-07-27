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
 * Base class for the SmtpClient/ServerUnparser
 */
abstract class SmtpUnparser
  extends AbstractUnparser {

  private final Pipeline m_pipeline;
  private final SmtpCasing m_parentCasing;
  private final Logger m_logger = Logger.getLogger(SmtpUnparser.class);
  private boolean m_passthru = false;

  protected SmtpUnparser(TCPSession session,
    SmtpCasing parent,
    boolean clientSide) {
    
    super(session, clientSide);
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
    getSmtpCasing().endSession(isClientSide());
    return null;
  }  
  
}  