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
import com.metavize.tran.mime.EmailAddress;

/**
 * Implementation of TransactionHandler which
 * does nothing except pass everything along
 * and perform some debug logging.
 */
public class SimpleTransactionHandler
  extends TransactionHandler {

  private final Logger m_logger = Logger.getLogger(SimpleTransactionHandler.class);  

  public SimpleTransactionHandler(SmtpTransaction tx) {
    super(tx);
  }

  @Override
  public void handleRSETCommand(Command command,
    Session.SmtpCommandActions actions) {

    m_logger.debug("[handleRSETCommand] (will pass along to handleCommand)");
    getTransaction().reset();
    handleCommand(command, actions);
  }
  
  @Override
  public void handleCommand(Command command,
    Session.SmtpCommandActions actions) {
    
    m_logger.debug("[handleCommand]");
    actions.getTokenResultBuilder().addTokenForServer(command);
    actions.enqueueResponseHandler(new PassthruResponseCompletion());
  }
  @Override
  public void handleMAILCommand(MAILCommand command,
    Session.SmtpCommandActions actions) {
    
    m_logger.debug("[handleMAILCommand]");
    actions.getTokenResultBuilder().addTokenForServer(command);
    actions.enqueueResponseHandler(new MAILContinuation(command.getAddress()));       
  }
  @Override
  public void handleRCPTCommand(RCPTCommand command,
    Session.SmtpCommandActions actions) {
    
    m_logger.debug("[handleRCPTCommand]");
    actions.getTokenResultBuilder().addTokenForServer(command);
    actions.enqueueResponseHandler(new RCPTContinuation(command.getAddress()));
  }
  @Override
  public void handleBeginMIME(BeginMIMEToken token,
    Session.SmtpCommandActions actions) {
    m_logger.debug("[handleBeginMIME] (no response expected, so none will be queued");
    actions.getTokenResultBuilder().addTokenForServer(token);
  }
  @Override    
  public void handleContinuedMIME(ContinuedMIMEToken token,
    Session.SmtpCommandActions actions) {
    if(token.isLast()) {
      m_logger.debug("[handleContinuedMIME] (last token, so enqueue a continuation for the response");
      actions.getTokenResultBuilder().addTokenForServer(token);
      actions.enqueueResponseHandler(new DataTransmissionContinuation());
    }
    else {
      m_logger.debug("[handleContinuedMIME] (not last - no response expected, so none will be queued");
      actions.getTokenResultBuilder().addTokenForServer(token);    
    }
  }
  @Override
  public void handleCompleteMIME(CompleteMIMEToken token,
    Session.SmtpCommandActions actions) {
    throw new RuntimeException("TODO bscott implement me");
  }




  
  //==========================
  // Inner-Classes
  //===========================


  //=============== Inner Class Separator ====================
  
  private class DataTransmissionContinuation
    extends PassthruResponseCompletion {

    public void handleResponse(Response resp,
      Session.SmtpResponseActions actions) {
      m_logger.debug("[$DataTransmissionContinuation][handleResponse]");
      if(resp.getCode() < 300) {
        getTransaction().commit();
      }
      else {
        getTransaction().failed();
      }
      super.handleResponse(resp, actions);
    }
  }
  


  //=============== Inner Class Separator ====================
    
  private class ContinuationWithAddress
    extends PassthruResponseCompletion {

    private final EmailAddress m_addr;

    ContinuationWithAddress(EmailAddress addr) {
      m_addr = addr;
    }

    protected EmailAddress getAddress() {
      return m_addr;
    }
    
    public void handleResponse(Response resp,
      Session.SmtpResponseActions actions) {
      super.handleResponse(resp, actions);
    }
  }


  //=============== Inner Class Separator ====================  

  private class MAILContinuation
    extends ContinuationWithAddress {

    MAILContinuation(EmailAddress addr) {
      super(addr);
    }
    
    public void handleResponse(Response resp,
      Session.SmtpResponseActions actions) {
      m_logger.debug("[$MAILContinuation][handleResponse]");
      getTransaction().fromResponse(getAddress(), (resp.getCode() < 300));
      super.handleResponse(resp, actions);
    }
  }


  //=============== Inner Class Separator ====================  
     
  private class RCPTContinuation
    extends ContinuationWithAddress {

    RCPTContinuation(EmailAddress addr) {
      super(addr);
    }
    
    public void handleResponse(Response resp,
      Session.SmtpResponseActions actions) {
      m_logger.debug("[$RCPTContinuation][handleResponse]");
      getTransaction().toResponse(getAddress(), (resp.getCode() < 300));
      super.handleResponse(resp, actions);
    }
  }

}