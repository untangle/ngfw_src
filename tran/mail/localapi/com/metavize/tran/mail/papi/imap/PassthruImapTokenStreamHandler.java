/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.mail.papi.imap;

import com.metavize.tran.mail.papi.ContinuedMIMEToken;
import com.metavize.tran.token.TokenResult;
import com.metavize.tran.token.Token;
import org.apache.log4j.Logger;


/**
 * Implementation of TokenStreamHandler which
 * does nothing except pass stuff through.
 */
public class PassthruImapTokenStreamHandler
  extends ImapTokenStreamHandler {

  private final Logger m_logger =
    Logger.getLogger(PassthruImapTokenStreamHandler.class);  

  @Override
  public boolean handleClientFin() {
    m_logger.debug("[handleClientFin]");
    return true;
  }
  
  @Override
  public boolean handleServerFin() {
    m_logger.debug("[handleServerFin]");
    return true;
  }
  
  @Override
  public TokenResult handleChunkFromServer(ImapChunk token) {
    m_logger.debug("[handleChunkFromServer]");
    return new TokenResult(new Token[] { token }, null);
  }
  
  @Override  
  public TokenResult handleBeginMIMEFromServer(BeginImapMIMEToken token) {
    m_logger.debug("[handleBeginMIMEFromServer]");
    return new TokenResult(new Token[] { token }, null);
  }
  
  @Override  
  public TokenResult handleContinuedMIMEFromServer(ContinuedMIMEToken token) {
    m_logger.debug("[handleContinuedMIMEFromServer]");
    return new TokenResult(new Token[] { token }, null);
  }
  
  @Override  
  public TokenResult handleCompleteMIMEFromServer(CompleteImapMIMEToken token) {
    m_logger.debug("[handleCompleteMIMEFromServer]");
    return new TokenResult(new Token[] { token }, null);
  }
  
  @Override  
  public void handleFinalized() {
    m_logger.debug("[handleFinalized]");
  }
  
}

