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

import com.metavize.tran.mime.*;

import org.apache.log4j.Logger;

/**
 * <b>Work in progress</b>
 */
public abstract class BufferingSessionHandler
  implements SessionHandler {

  private final Logger m_logger = Logger.getLogger(BufferingSessionHandler.class);


  public enum BlockableAction {
    BLOCK,
    CLOSE_CONNECTION,
    PASS
  };

  public enum NonBlockableAction {
    CLOSE_CONNECTION,
    PASS
  };

  private long m_serverTimestamp;
  private long m_clientTimestamp;

  public BufferingSessionHandler() {
    updateTimestamps(true, true);
  }
  
  /**
   * Get the size over-which this class should no
   * longer buffer.  After this point, the
   * Buffering is abandoned and passed-along
   * to the next transform/casing.
   */
  public abstract int getGiveupSz();

  public abstract long getMaxClientWait();

  public abstract long getMaxServerWait();

  /**
   * If true, this handler will continue to buffer even after 
   * trickling has begun.  The MIMEMessage can no longer be modified
   * (i.e. it will not be passed downstream) but the subclass can
   * choose to close the connection or simply audit the
   * transmitted message.
   */
  public abstract boolean bufferAndTrickle();

  public abstract BlockableAction handleMessageCanBlock(MIMEMessageHolder msgHolder);

  public abstract NonBlockableAction handleMessageCanNotBlock(MIMEMessageHolder msgHolder);

  /**
   * Method subclasses can optionally override.  Lets
   * subclasses modify the response to an EHLO
   * command.
   * <br>
   * Default implementation does nothing (returns the argument)
   *
   * @param response the response bound for the client
   * @return the (possibly new) response which will be
   *         sent to the client.
   */
  public Response handleEHLOResponse(Response response) {
    return response;
  }
  
  public final void handleCommand(Command command,
    Session.SmtpCommandActions actions) {

    m_logger.debug("[handleCommand] with command of type \"" +
      command.getType() + "\"");    
    
    ResponseCompletion compl = null;
    
    if(command.getType() == Command.CommandType.EHLO) {
      compl = new EHLOResponseCompletion();
    }
    else {
      compl = new TimestampResponseCompletion(true, true);
    }

    actions.getTokenResultBuilder().addTokenForServer(command);
    actions.enqueueResponseHandler(compl);
  }

  public final void handleOpeningResponse(Response resp,
    Session.SmtpResponseActions actions) {
    m_logger.debug("[handleOpeningResponse]");
    updateTimestamps(true, true);
    actions.getTokenResultBuilder().addTokenForClient(resp);
  }    
    
  public final TransactionHandler createTxHandler(SmtpTransaction tx) {
    return new BufferingTransactionHandler(tx);
  }


  private final void updateTimestamps(boolean client,
    boolean server) {
    long now = System.currentTimeMillis();
    if(client) {
      m_clientTimestamp = now;
    }
    if(server) {
      m_serverTimestamp = now;
    }
  }




  //===================== Inner Class ====================

  /**
   * Completion which updates timestamps
   */
  private class TimestampResponseCompletion
    implements ResponseCompletion {

    private final boolean m_updateClient;
    private final boolean m_updateServer;

    TimestampResponseCompletion(boolean uc,
      boolean us) {
      m_updateClient = uc;
      m_updateServer = us;
    }
    TimestampResponseCompletion() {
      this(true, true);
    }

    protected void callUpdateTimestamps() {
      updateTimestamps(m_updateClient, m_updateServer);
    }
    
    public void handleResponse(Response resp,
      Session.SmtpResponseActions actions) {
      callUpdateTimestamps();
      actions.getTokenResultBuilder().addTokenForClient(resp);
    }
  }  


  //===================== Inner Class ====================

  /**
   * Callback from the EHLO response
   */
  private class EHLOResponseCompletion
    extends TimestampResponseCompletion {

    EHLOResponseCompletion() {
      super(true, true);
    }
  
    public void handleResponse(Response resp,
      Session.SmtpResponseActions actions) {
      super.handleResponse(handleEHLOResponse(resp), actions);
    }
  }  


  private enum BufTxState {
    ENVELOPE,
    CLIENT_DATA_REQ,
    CLIENT_DATA_ACK,
    CLIENT_MAIL_TRANSMISSION,
    SVR_DATA_REQ,
    SVR_DATA_ACK,
    SVR_MAIL_TRANSMISSION,
    DONE
  }


  //===================== Inner Class ====================
  
  private class BufferingTransactionHandler
    extends TransactionHandler {
  
    BufferingTransactionHandler(SmtpTransaction tx) {
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
      actions.enqueueResponseHandler(new TimestampResponseCompletion());
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
    // Inner-Inner-Classes
    //===========================
  
  
    //****************** Inner-Inner Class Separator ******************
    
    private class DataTransmissionContinuation
      extends TimestampResponseCompletion {
  
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
    
  
  
    //****************** Inner-Inner Class Separator ******************
      
    private class ContinuationWithAddress
      extends TimestampResponseCompletion {
  
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
  
  
    //****************** Inner-Inner Class Separator ******************
  
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
  
  
    //****************** Inner-Inner Class Separator ******************
      
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
  
  }//ENDOF BufferingTransactionHandler Class Definition

}//ENDOF BufferingSessionHandler Class Definition