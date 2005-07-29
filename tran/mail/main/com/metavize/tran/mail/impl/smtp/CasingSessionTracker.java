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
import com.metavize.tran.mail.papi.smtp.*;
import com.metavize.tran.mime.*;
import java.util.*;
import org.apache.log4j.Logger;


/**
 * Class which is shared between Client Parser and Unparser, observing
 * state transitions (esp: transaction-impacting) Commands and
 * Responses to accumulate who is part of a transaction (TO/FROM)
 * and to align requests with responses.
 */
class CasingSessionTracker {

  /**
   * Interface for Object wishing to
   * be called-back when the response
   * to a given Command is received.
   */
  interface ResponseAction {
    /**
     * Callback corresponding to the
     * Command for-which this action
     * was registered.
     * <br><br>
     * Note that any changes to the internal
     * state of the Tracker have <b>already</b>
     * been made (i.e. the tracker sees the
     * response before the callback).
     */
    void response(int code);
  }

  private final Logger m_logger = Logger.getLogger(CasingSessionTracker.class);

  private SmtpTransaction m_currentTransaction;
  private List<ResponseAction> m_outstandingRequests;

  CasingSessionTracker() {
    m_outstandingRequests = new LinkedList<ResponseAction>();
    //Add response for initial salutation
    m_outstandingRequests.add(new SimpleResponseAction());    
  }

  /**
   * Get the underlying transaction.  May be null if
   * this tracker thinks there is no outstanding transaction.
   */
  SmtpTransaction getCurrentTransaction() {
    return m_currentTransaction;
  }

  void beginMsgTransmission() {
    beginMsgTransmission(null);
  }
  
  void beginMsgTransmission(ResponseAction chainedAction) {
    getOrCreateTransaction();
    m_outstandingRequests.add(new TransmissionResponseAction(chainedAction));

  }
  /**
   * Inform that the server has been shut-down.  This
   * enqueues an extra response handler (in case the server
   * ACKS the FIN).
   */
  void serverShutdown() {
    m_outstandingRequests.add(new SimpleResponseAction());
  }

  void commandReceived(Command command) {
    commandReceived(command, null);
  }
  
  void commandReceived(Command command,
    ResponseAction chainedAction) {
    
    ResponseAction action = null;
    if(command.getType() == Command.CommandType.MAIL) {
      EmailAddress addr = ((MAILCommand) command).getAddress();
      getOrCreateTransaction().fromRequest(addr);
      action = new MAILResponseAction(addr, chainedAction);
    }
    else if(command.getType() == Command.CommandType.RCPT) {
      EmailAddress addr = ((RCPTCommand) command).getAddress();
      getOrCreateTransaction().toRequest(addr);
      action = new RCPTResponseAction(addr, chainedAction);
    }
    else if(command.getType() == Command.CommandType.RSET) {
      getOrCreateTransaction().reset();
      m_currentTransaction = null;
      action = new SimpleResponseAction(chainedAction);
    }
    else if(command.getType() == Command.CommandType.DATA) {
      action = new DATAResponseAction(chainedAction);
    }
    else {
      action = new SimpleResponseAction(chainedAction);
    }
    m_outstandingRequests.add(action);              
  }

  
  void responseReceived(Response response) {
    if(m_outstandingRequests.size() == 0) {
      m_logger.error("Misalignment of req/resp tracking.  No outstanding response");
    }
    else {
      m_outstandingRequests.remove(0).response(response.getCode());
    }
  }

  private SmtpTransaction getOrCreateTransaction() {
    if(m_currentTransaction == null) {
      m_currentTransaction = new SmtpTransaction();
    }
    return m_currentTransaction;
  }
  
  private abstract class ChainedResponseAction
    implements ResponseAction {

    private final ResponseAction m_chained;

    ChainedResponseAction() {
      this(null);
    }
    ChainedResponseAction(ResponseAction chained) {
      m_chained = chained;
    }
    
    public final void response(int code) {
      responseImpl(code);
      if(m_chained != null) {
        m_chained.response(code);
      }
    }

    abstract void responseImpl(int code);
  }

  private class SimpleResponseAction
    extends ChainedResponseAction {

    private ResponseAction m_chained;

    SimpleResponseAction() {
      super();
    }
    SimpleResponseAction(ResponseAction chained) {
      super(chained);
    }
    
    void responseImpl(int code) {
      //Do nothing ourselves
    }
  }

  
  private class MAILResponseAction
    extends ChainedResponseAction {
    
    private final EmailAddress m_addr;

    MAILResponseAction(EmailAddress addr) {
      this(addr, null);
    }
    MAILResponseAction(EmailAddress addr,
      ResponseAction chained) {
      super(chained);
      m_addr = addr;
    }    

    void responseImpl(int code) {
      if(m_currentTransaction != null) {
        m_currentTransaction.fromResponse(m_addr, code<300);
      }
    }    
  }

  
  private class RCPTResponseAction
    extends ChainedResponseAction {
    
    private final EmailAddress m_addr;

    RCPTResponseAction(EmailAddress addr) {
      this(addr, null);
    }
    RCPTResponseAction(EmailAddress addr,
      ResponseAction chained) {
      super(chained);
      m_addr = addr;
    }      

    void responseImpl(int code) {
      if(m_currentTransaction != null) {
        m_currentTransaction.toResponse(m_addr, code<300);
      }
    }    
  }

  private class DATAResponseAction
    extends ChainedResponseAction {

    DATAResponseAction() {
      super();
    }
    DATAResponseAction(ResponseAction chained) {
      super(chained);
    }    
    
    void responseImpl(int code) {
      if(code >= 400) {
        getOrCreateTransaction().failed();
        m_currentTransaction = null;
      }
    }      
  }  

  private class TransmissionResponseAction
    extends ChainedResponseAction {

    TransmissionResponseAction() {
      super();
    }
    TransmissionResponseAction(ResponseAction chained) {
      super(chained);
    }      
    
    void responseImpl(int code) {
      if(m_currentTransaction != null) {
        if(code < 300) {
          m_currentTransaction.commit();
        }
        else {
          m_currentTransaction.failed();
        }
      }
      m_currentTransaction = null;
    }      
  }

}