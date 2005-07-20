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

package com.metavize.tran.mail.papi.smtp;


import java.util.List;
import java.util.ArrayList;
import com.metavize.tran.mime.EmailAddress;
import org.apache.log4j.Logger;


/**
 * Class representing an SmtpTransaction.  Maintains
 * the state of the transaction (see the enum, which
 * I don't know how to reference with JavaDoc)
 * as well as any TO/FROM EmailAddresses.
 * <br>
 * Since there is some gray area in the "middle"
 * between client and server, this class maintains
 * envelope data (recipients and sender)
 * as "provisional" until they are "confirmed".
 * Confirmed means the server accepted, provisional 
 * means the request was issued by client yet final
 * disposition is unknown.
 */
public final class SmtpTransaction {

  /**
   * Enum of Transaction states.
   */
  public enum TransactionState {
    OPEN,
    COMMITTED,
    RESET,
    FAILED
  }

  private TransactionState m_state = TransactionState.OPEN;
  private EmailAddress m_from;
  private boolean m_fromConfirmed;
  private List<EmailAddressWithStatus> m_recipients;

  public SmtpTransaction() {
    m_recipients = new ArrayList<EmailAddressWithStatus>();
    m_from = null;
    m_fromConfirmed = false;
  }

  /**
   * Access the state of the transaction
   */
  public TransactionState getState() {
    return m_state;
  }

  /**
   * Change the state to "COMITTED"
   */
  public void commit() {
    m_state = TransactionState.COMMITTED;
  }

  /**
   * Change the state to "RESET"
   */  
  public void reset() {
    m_state = TransactionState.RESET;
  }

  /**
   * Change the state to "FAILED"
   */  
  public void failed() {
    m_state = TransactionState.FAILED;
  }

  /**
   * Test if this transaction is still open.
   */
  public boolean isOpen() {
    return m_state == TransactionState.OPEN;
  }

  /**
   * Get the recipients ("RCPT TO...") for this
   * Transaction.
   *
   * @param confirmedOnly if true, only those recipients
   *        who have been positivly acknowledged by the
   *        server are returned.
   *
   * @return the recipients
   */
  public List<EmailAddress> getRecipients(boolean confirmedOnly) {
    List<EmailAddress> ret = new ArrayList<EmailAddress>();
    for(EmailAddressWithStatus eaws : m_recipients) {
      if((confirmedOnly && eaws.confirmed) ||
        (!confirmedOnly)) {
        ret.add(eaws.addr);
      }
    }
    return ret;
  }

  /**
   * A recipient has been requested (an "RCPT TO...")
   * issued.  Queue the recipient provisionally.
   */
  public void toRequest(EmailAddress addr) {
    m_recipients.add(new EmailAddressWithStatus(addr));
  }

  /**
   * The server has responded to a
   * previous RCPT TO... request.  The Transaction
   * should change its internal recipient
   * collection accordingly
   *
   * @param accept if true, the server accepted
   *        the recipient.
   */
  public void toResponse(EmailAddress addr,
    boolean accept) {
    //In case someone (dumb) attempts
    //to request the same recipient twice,
    //scan from top-down for the to
    for(int i = 0; i<m_recipients.size(); i++) {
      EmailAddressWithStatus eaws = m_recipients.get(i);
      if(!eaws.addr.equals(addr)) {
        continue;
      }
      if(accept) {
        eaws.confirmed = true;
      }
      else {
        m_recipients.remove(i);
      }
      return;
    }
    //If we're here, there is a programming
    //error with the caller
    //TODO bscott assert?  Warn?
  }

  /**
   * The client has issued a "MAIL FROM..." command.
   * The transaction will record this address as
   * the FROM provisionally.
   */
  public void fromRequest(EmailAddress addr) {
    m_from = addr;
    m_fromConfirmed = false;
  }

  /**
   * Change the internal envelope data to reflect
   * the server's response to the "MAIL" command
   *
   * @param accept did the server accept the address.
   */
  public void fromResponse(EmailAddress addr,
    boolean accept) {
    if(m_from == null) {
      //TODO bscott programming error
      m_from = null;
      return;
    }
    if(accept) {
      m_fromConfirmed = true;
    }
    else {
      m_from = null;
      m_fromConfirmed = false;
    }
  }


  /**
   * Get the FROM for the envelope.  May
   * be null.  To test if this has
   * been confirmed, use {@link #isFromConfirmed}
   */
  public EmailAddress getFrom() {
    return m_from;
  }

  /**
   * Test if the FROM has been confirmed by the
   * server.  
   */
  public boolean isFromConfirmed() {
    return m_fromConfirmed;
  }



  private class EmailAddressWithStatus {
    final EmailAddress addr;
    boolean confirmed;

    EmailAddressWithStatus(EmailAddress addr) {
      this.addr = addr;
      this.confirmed = false;
    }
  }

}