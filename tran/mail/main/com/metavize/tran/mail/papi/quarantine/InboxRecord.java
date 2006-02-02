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

package com.metavize.tran.mail.papi.quarantine;

import java.io.Serializable;


/**
 * Representation of a mail quarantined within
 * a given {@link com.metavize.tran.mail.papi.quarantine.InboxIndex inbox}.
 * <br><br>
 * Users are not supposed to subclass this.  Subclassing
 * is for the internals of the quarantine system (but may
 * be visible to you because of classloader issues).
 */
public abstract class InboxRecord
  implements Serializable {

  private String m_mailID;
  private long m_addedOn;
  private long m_size;
  private MailSummary m_mailSummary;
  private String[] m_recipients;

  public InboxRecord() {
  
  }
  
  public InboxRecord(String mailID,
    long addedOn,
    long size,
    MailSummary summary,
    String[] recipients) {
    
    m_mailID = mailID;
    m_addedOn = addedOn;
    m_size = size;
    m_mailSummary = summary;
    m_recipients = recipients;
  }


  /**
   * Get the recipients <b>for this inbox entry</b>.  This does
   * not mean all recipients of the email.  This refers to the
   * destination(s) of the mail which were re-routed to quarantine.
   * For the cases of address remapping, there may be more than
   * one recipient, and the owner of the inbox may not be among
   * the recipients.
   */
  public final String[] getRecipients() {
    return m_recipients;
  }
  public final void setRecipients(String[] recipients) {
    m_recipients = recipients;
  }

  /**
   * Get the unique (within the scope of a given inbox)
   * ID for this mail
   *
   * @return the unique ID
   */
  public final String getMailID() {
    return m_mailID;
  }
  public final void setMailID(String id) {
    m_mailID = id;
  }

  /**
   * Get the date (millis since 1970, GMT) that this
   * file was placed into the quarantine.  This
   * is <b>not</b> the DATE on the MIME message.
   */
  public final long getInternDate() {
    return m_addedOn;
  }
  public final void setInternDate(long date) {
    m_addedOn = date;
  }

  /**
   * Get the size of the mail's MIME file.
   */
  public final long getSize() {
    return m_size;
  }
  public final void setSize(long size) {
    m_size = size;
  }

  /**
   * Get the summary of the mail
   */
  public final MailSummary getMailSummary() {
    return m_mailSummary;
  }
  public final void setMailSummary(MailSummary summary) {
    m_mailSummary = summary;
  }  

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ID: ").append(getMailID()).
      append(", Date: ").append("" + getInternDate()).
      append(", Size: ").append("" + getSize()).
      append(", Summary:[").append(getMailSummary()).append("]");
    return sb.toString();
  }

  @Override
  public boolean equals(Object other) {
    if(!(other instanceof InboxRecord)) {
      return false;
    }
    return ((InboxRecord) other).getMailID().equals(getMailID());
  }
  
  @Override
  public int hashCode() {
    return getMailID().hashCode();
  }

}