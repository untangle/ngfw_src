/**
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
import com.metavize.mvvm.tapi.event.TCPStreamer;
import com.metavize.tran.token.AbstractUnparser;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.UnparseException;
import com.metavize.tran.token.UnparseResult;
import org.apache.log4j.Logger;

/**
 * ...name says it all...
 */
class ImapClientUnparser
  extends ImapUnparser {

  private final Logger m_logger =
    Logger.getLogger(ImapClientUnparser.class);

  ImapClientUnparser(TCPSession session,
    ImapCasing parent) {
    super(session, parent, true);
    m_logger.debug("Created");
  }


  public UnparseResult unparse(Token token)
    throws UnparseException {

    //TEMP so folks don't hit my test code
    if(System.currentTimeMillis() > 0) {
      return new UnparseResult(((Chunk)token).getBytes());
    }
    
    m_logger.debug("=========unparse============");
    Chunk c = (Chunk)token;
    ByteBuffer buf = c.getBytes();

    m_logger.debug("BEGIN UNPARSING");
    m_logger.debug(com.metavize.tran.util.ASCIIUtil.bbToString(buf));
    m_logger.debug("ENDOF UNPARSING");    

    m_logger.debug(this + "unparsing: " + buf);

    return new UnparseResult(buf);

  }

  @Override
  public void handleFinalized() {
    m_logger.debug("[handleFinalized()]");
  }
}