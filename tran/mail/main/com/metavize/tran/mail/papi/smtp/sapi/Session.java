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

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.TokenResultBuilder;
import com.metavize.tran.token.Chunk;
import org.apache.log4j.Logger;
import java.util.LinkedList;
import java.util.List;


/**
 * Class which acts to listen on an SMTP Token Stream
 * and convert to a more managable
 * {@link com.metavize.tran.mail.papi.smtp.sapi.Session Session-oriented}
 * API.
 * <br>
 * @see com.metavize.tran.mail.papi.smtp.sapi.Session
 */
public final class Session
  extends SmtpTokenStream {


  //==========================
  // Inner-Interfaces
  //===========================
  
  /**
   * Base class of available Actions to take
   * during various SMTP events.  Instances
   * of SmtpActions are passed to callbacks
   * on {@link com.metavize.tran.mail.papi.smtp.sapi.SyntheticAction SyntheticAction}s
   */
  public interface SmtpActions {

    /**
     * Get the TokenResultBuilder, used to pass along tokens
     * to the Client or Server
     */
    public TokenResultBuilder getTokenResultBuilder();

    /**
     * Request that the Session enqueue a response handler.
     * <br>
     * This is generally called after the handler has
     * passed-along a request to the server.  The request
     * itself may be from the client, or issued by the
     * handler itself ("synthetic").
     *
     * @param cont the code to execute when a response
     *        comes back from the server.
     */
    public void enqueueResponseHandler(ResponseCompletion cont);

    /**
     * Cause client tokens to be queued before this
     * transform (and ultimatly perhaps to the client).
     */
    public void disableClientTokens();

    /**
     * Corresponding re-enablement method for the
     * {@link #disableClientTokens disableClientTokens} method.  Note
     * that this may be called even if the client is not disabled.
     */
    public void enableClientTokens();

  }

  /**
   * Callback interface for
   * {@link com.metavize.tran.mail.papi.smtp.sapi.SessionHandler Sessions}
   * or {@link com.metavize.tran.mail.papi.smtp.sapi.TransactionHandler Transactions}
   * to take action when an SMTP command arrives.
   */
  public interface SmtpCommandActions
    extends SmtpActions {
    
    /**
     * Append a synthetic response.  This is used when the
     * Handler has decided not to pass-along a client
     * request to the server.  This causes the SyntheticAction
     * to be placed within the internal Outstanding Request
     * queue such that the Synthetic response will be issued
     * in the correct order.
     * <br>
     * If the handler using this Object makes this call and there
     * are currently no outstanding requests, at the completion
     * of the handler callback the <code>synth</code>
     * will be called to issue the "fake" response (with
     * the same TokenStreamer, to preserve ordering).
     */
    public void appendSyntheticResponse(SyntheticAction synth);

  }

  /**
   * Set of actions available to
   * {@link com.metavize.tran.mail.papi.smtp.sapi.ResponseCompletion ResponseCompletion}
   * instances while they are called-back.
   */
  public interface SmtpResponseActions
    extends SmtpActions {

  }


  //==========================
  // Data Members
  //===========================
  

  private MySmtpTokenStreamHandler m_streamHandler =
    new MySmtpTokenStreamHandler();

  private SessionHandler m_sessionHandler;//Sink
  private TransactionHandler m_currentTxHandler;//Sink

  private List<OutstandingRequest> m_outstandingRequests;




  
  //==========================
  // Construction
  //===========================

  /**
   * Construct a new Session
   *
   * @param session the TCPSession to listen-on
   * @param handler the Session handler
   */
  public Session(TCPSession session,
    SessionHandler handler) {
    super(session);
    super.setHandler(m_streamHandler);
    m_sessionHandler = handler;
    m_outstandingRequests = new LinkedList<OutstandingRequest>();

    //Tricky little thing here.  The first message passed for SMTP
    //is actualy from the server.  Place a Response handler
    //into the OutstandingRequest queue to handle this and
    //call our SessionHandler with the initial salutation
    m_outstandingRequests.add(new OutstandingRequest(
      new ResponseCompletion() {
        public void handleResponse(Response resp,
          Session.SmtpResponseActions actions) {
          m_sessionHandler.handleOpeningResponse(resp, actions);
        }
      }));
  }


  
  //==========================
  // Helpers
  //===========================

  /**
   * Process any Synthetic actions
   */
  private void processSynths(HoldsSyntheticActions synths,
    TokenResultBuilder trb) {
    
    SmtpActionsImpl actions = new SmtpActionsImpl(trb);
    SyntheticAction action = synths.popAction();
    while(action != null) {
      action.handle(actions);
      action = synths.popAction();
    }
  }

  /**
   * Helper 
   */
  private TransactionHandler getOrCreateTxHandler() {
    if(m_currentTxHandler == null) {
      m_currentTxHandler = m_sessionHandler.createTxHandler(new SmtpTransaction());
    }
    return m_currentTxHandler;
  }


  
  //==========================
  // Inner-Classes
  //===========================


  //=============== Inner Class Separator ====================
  
  private class SmtpActionsImpl
    implements SmtpActions {

    private final TokenResultBuilder m_ts;

    SmtpActionsImpl(TokenResultBuilder ts) {
      m_ts = ts;
    }
    public TokenResultBuilder getTokenResultBuilder() {
      return m_ts;
    }
    public void enqueueResponseHandler(ResponseCompletion cont) {
      m_outstandingRequests.add(new OutstandingRequest(cont));
    }
    public void disableClientTokens() {
    }

    public void enableClientTokens() {
    }
  }

  

  //=============== Inner Class Separator ====================

  private class SmtpResponseActionsImpl
    extends SmtpActionsImpl
    implements SmtpResponseActions {

    SmtpResponseActionsImpl(TokenResultBuilder ts) {
      super(ts);
    }
  }
  


  //=============== Inner Class Separator ====================
  
  private class SmtpCommandActionsImpl
    extends SmtpActionsImpl
    implements SmtpCommandActions {

    private HoldsSyntheticActions m_immediateActions = null;

    SmtpCommandActionsImpl(TokenResultBuilder ts) {
      super(ts);
    }

    public void appendSyntheticResponse(SyntheticAction synth) {
      if(m_outstandingRequests.size() == 0) {
        if(m_immediateActions != null) {
          m_immediateActions = new HoldsSyntheticActions();
        }
        m_immediateActions.pushAction(synth);
      }
    }
    void followup() {
      if(m_immediateActions != null) {
        processSynths(m_immediateActions, getTokenResultBuilder());
      }
    }
  }


  //=============== Inner Class Separator ====================

  private class HoldsSyntheticActions {
    private List<SyntheticAction> m_additionalActions;

    void pushAction(SyntheticAction synth) {
      if(m_additionalActions == null) {
        m_additionalActions = new LinkedList<SyntheticAction>();
      }
      m_additionalActions.add(synth);
    }
    //Must keep calling this until it returns null, as
    //one synthetic may enqueue another
    SyntheticAction popAction() {
      if(m_additionalActions == null) {
        return null;
      }
      return m_additionalActions.remove(0);
    }  
  }



  //=============== Inner Class Separator ====================

  private class OutstandingRequest
    extends HoldsSyntheticActions {
    
    final ResponseCompletion cont;

    OutstandingRequest(ResponseCompletion cont) {
      this.cont = cont;
    }

  }


  //=============== Inner Class Separator ====================

  private class MySmtpTokenStreamHandler
    implements SmtpTokenStreamHandler {
  
    public void passthru(TokenResultBuilder resultBuilder) {
      //Nothing to do
    }
  
    public void handleCommand(TokenResultBuilder resultBuilder,
      Command cmd) {
      SmtpCommandActionsImpl actions = new SmtpCommandActionsImpl(resultBuilder);
      if(m_currentTxHandler != null) {
        if(cmd.getType() == Command.CommandType.RSET) {
          m_currentTxHandler.handleRSETCommand(cmd, actions);
          m_currentTxHandler = null;
        }
        else {
          m_currentTxHandler.handleCommand(cmd, actions);
        }
      }
      else {
        m_sessionHandler.handleCommand(cmd, actions);
      }
      actions.followup();
    }
  
    public void handleMAILCommand(TokenResultBuilder resultBuilder,
      MAILCommand cmd) {
      TransactionHandler handler = getOrCreateTxHandler();
      SmtpCommandActionsImpl actions = new SmtpCommandActionsImpl(resultBuilder);
      handler.handleMAILCommand(cmd, actions);
      actions.followup();
    }
  
    public void handleRCPTCommand(TokenResultBuilder resultBuilder,
      RCPTCommand cmd) {
      TransactionHandler handler = getOrCreateTxHandler();
      SmtpCommandActionsImpl actions = new SmtpCommandActionsImpl(resultBuilder);
      handler.handleRCPTCommand(cmd, actions);
      actions.followup();
    }
  
    public void handleBeginMIME(TokenResultBuilder resultBuilder,
      BeginMIMEToken token) {
      TransactionHandler handler = getOrCreateTxHandler();
      SmtpCommandActionsImpl actions = new SmtpCommandActionsImpl(resultBuilder);
      handler.handleBeginMIME(token, actions);
      actions.followup();
    }
  
    public void handleContinuedMIME(TokenResultBuilder resultBuilder,
      ContinuedMIMEToken token) {
      
      TransactionHandler handler = getOrCreateTxHandler();
      if(token.isLast()) {
        m_currentTxHandler = null;
      }
      SmtpCommandActionsImpl actions = new SmtpCommandActionsImpl(resultBuilder);
      handler.handleContinuedMIME(token, actions);
      actions.followup();
    }
  
    public void handleResponse(TokenResultBuilder resultBuilder,
      Response resp) {

      if(m_outstandingRequests.size() == 0) {
        //TODO bscott Major programming error.  We should log this
        resultBuilder.addTokenForClient(resp);
        return;
      }
      OutstandingRequest or = m_outstandingRequests.remove(0);
      or.cont.handleResponse(resp, new SmtpResponseActionsImpl(resultBuilder));
      processSynths(or, resultBuilder);
    }
  
    public void handleChunkForClient(TokenResultBuilder resultBuilder,
      Chunk chunk) {
      resultBuilder.addTokenForClient(chunk);
    }
  
    public void handleChunkForServer(TokenResultBuilder resultBuilder,
      Chunk chunk) {
      resultBuilder.addTokenForServer(chunk);
    }
    public void handleCompleteMIME(TokenResultBuilder resultBuilder,
      CompleteMIMEToken token) {
      
      TransactionHandler handler = getOrCreateTxHandler();
      
      //Looks odd, but the Transaction is complete so just
      //assign the current handler to null.
      m_currentTxHandler = null;

      SmtpCommandActionsImpl actions = new SmtpCommandActionsImpl(resultBuilder);
      handler.handleCompleteMIME(token, actions);
      actions.followup();
    }    
  }//ENDOF MySmtpTokenStreamHandler Class Definition
 
}//ENDOF Session Class Definition