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

package com.metavize.tran.mail.papi.smtp.sapi;

import com.metavize.tran.mail.papi.smtp.*;

import org.apache.log4j.Logger;

/**
 * Implementation of SessionHandler which
 * does nothing except pass everything along
 * and perform some debug logging.
 */
public class SimpleSessionHandler
  implements SessionHandler {

  private final Logger m_logger = Logger.getLogger(SimpleSessionHandler.class);

  public void handleCommand(Command command,
    Session.SmtpCommandActions actions) {
    m_logger.debug("[handleCommand] with command of type \"" +
      command.getType() + "\"");
    actions.sendCommandToServer(command, new PassthruResponseCompletion());
/*    
    actions.getTokenResultBuilder().addTokenForServer(command);
    actions.enqueueResponseHandler(new PassthruResponseCompletion());
*/    
  }

  public void handleOpeningResponse(Response resp,
    Session.SmtpResponseActions actions) {
    m_logger.debug("[handleOpeningResponse]");
    actions.sendResponseToClient(resp);
//    actions.getTokenResultBuilder().addTokenForClient(resp);
  }    
    
  public TransactionHandler createTxHandler(SmtpTransaction tx) {
    return new SimpleTransactionHandler(tx);
  }
  public boolean handleServerFIN(TransactionHandler currentTX) {
    return true;
  }

  public boolean handleClientFIN(TransactionHandler currentTX) {
    return true;
  }


}