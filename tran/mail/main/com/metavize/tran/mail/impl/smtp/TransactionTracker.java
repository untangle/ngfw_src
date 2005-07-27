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
 * transaction-impacting Commands and Responses to accumulate
 * who is part of a transaction (TO/FROM).
 */
class TransactionTracker {

  private final Logger m_logger = Logger.getLogger(TransactionTracker.class);

  private SmtpTransaction m_currentTransaction;
  private List<ResponseAction> m_outstandingRequests;

  TransactionTracker() {
    m_outstandingRequests = new LinkedList<ResponseAction>();
    //Add response for initial salutation
    m_outstandingRequests.add(new ResponseAction());    
  }

  /**
   * Get the underlying transaction.  May be null if
   * this tracker thinks there is no outstanding transaction.
   */
  SmtpTransaction getCurrentTransaction() {
    return m_currentTransaction;
  }

  void beginMsgTransmission() {
    getOrCreateTransaction();
    m_outstandingRequests.add(new TransmissionResponseAction());

  }
  
  void commandReceived(Command command) {
    ResponseAction action = null;
    if(command.getType() == Command.CommandType.MAIL) {
      EmailAddress addr = ((MAILCommand) command).getAddress();
      getOrCreateTransaction().fromRequest(addr);
      action = new MAILResponseAction(addr);
    }
    else if(command.getType() == Command.CommandType.RCPT) {
      EmailAddress addr = ((RCPTCommand) command).getAddress();
      getOrCreateTransaction().toRequest(addr);
      action = new RCPTResponseAction(addr);
    }
    else if(command.getType() == Command.CommandType.RSET) {
      getOrCreateTransaction().reset();
      m_currentTransaction = null;
      action = new ResponseAction();
    }
    else if(command.getType() == Command.CommandType.DATA) {
      action = new DATAResponseAction();
    }
    else {
      action = new ResponseAction();
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
  


  private class ResponseAction {
    void response(int code) {
      //Do nothing
    }
  }

  
  private class MAILResponseAction
    extends ResponseAction {
    private final EmailAddress m_addr;
    MAILResponseAction(EmailAddress addr) {
      m_addr = addr;
    }
    void response(int code) {
      if(m_currentTransaction != null) {
        m_currentTransaction.fromResponse(m_addr, code<300);
      }
    }    
  }

  
  private class RCPTResponseAction
    extends ResponseAction {
    private final EmailAddress m_addr;
    RCPTResponseAction(EmailAddress addr) {
      m_addr = addr;
    }
    void response(int code) {
      if(m_currentTransaction != null) {
        m_currentTransaction.toResponse(m_addr, code<300);
      }
    }    
  }

  private class DATAResponseAction
    extends ResponseAction {
    void response(int code) {
      if(code >= 400) {
        getOrCreateTransaction().failed();
        m_currentTransaction = null;
      }
    }      
  }  

  private class TransmissionResponseAction
    extends ResponseAction {
    void response(int code) {
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