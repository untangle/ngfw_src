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

class ImapCasing
  extends AbstractCasing {

  private final Logger m_logger =
    Logger.getLogger(ImapCasing.class);  

  private final ImapParser m_parser;
  private final ImapUnparser m_unparser;
  
  ImapCasing(TCPSession session,
    boolean clientSide) {

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
  void endSession(boolean clientSide) {
    //
  }


  public Parser parser() {
    return m_parser;
  }

  public Unparser unparser() {
    return m_unparser;
  }
}
