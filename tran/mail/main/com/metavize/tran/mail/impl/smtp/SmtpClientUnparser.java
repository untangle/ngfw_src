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

import com.metavize.mvvm.*;
import com.metavize.mvvm.tapi.*;
import com.metavize.mvvm.tapi.event.TCPStreamer;
import com.metavize.tran.mail.papi.smtp.*;
import com.metavize.tran.token.*;
import com.metavize.tran.util.*;
import org.apache.log4j.Logger;

/**
 * ...name says it all...
 */
class SmtpClientUnparser
  extends SmtpUnparser {

  private final Logger m_logger = Logger.getLogger(SmtpClientUnparser.class);

  SmtpClientUnparser(TCPSession session,
    SmtpCasing parent,
    CasingSessionTracker tracker) {
    super(session, parent, tracker, true);
    m_logger.debug("Created");
  }


  public UnparseResult unparse(Token token)
    throws UnparseException {
    
    //-----------------------------------------------------------
    if(token instanceof PassThruToken) {
      m_logger.debug("Received PASSTHRU token.  Enter passthru mode");
      declarePassthru();//Inform the parser of this state
      return UnparseResult.NONE;
    }

    //-----------------------------------------------------------
    if(token instanceof MetadataToken) {
      //Don't pass along metadata tokens
      m_logger.debug("Pass along Metadata token as nothing");
      return UnparseResult.NONE;
    }

    //-----------------------------------------------------------
    if(token instanceof Response) {
      Response resp = (Response) token;
      getSessionTracker().responseReceived(resp);

      m_logger.debug("Passing response to client: " +
        resp.toDebugString());
    }
    else {
      m_logger.debug("Unparse token of type " + (token==null?"null":token.getClass().getName()));
    }

    getSmtpCasing().traceUnparse(token.getBytes());
    return new UnparseResult(token.getBytes());
  }


}
