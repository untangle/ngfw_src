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
 * Summary of an Inbox (I had already used "InboxSummary" as a class
 * name somewhere else, and didn't feel like changing it).
 */
public final class Inbox
  implements Serializable {

  private final String m_address;
  private long m_totalSz;
  private int m_numMails;

  public Inbox(String address) {
    this(address, 0, 0);
  }
  
  public Inbox(String address,
    long totalSz,
    int numMails) {
    m_address = address;
    m_totalSz = totalSz;
    m_numMails = numMails;
  }

  public String getAddress() {
    return m_address;
  }

  public void setTotalSz(long totalSz) {
    m_totalSz = totalSz;
  }
  public long getTotalSz() {
    return m_totalSz;
  }
  public void setNumMails(int numMails) {
    m_numMails = numMails;
  }
  public int getNumMails() {
    return m_numMails;
  }     
  


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Address: ").append(getAddress());
    return sb.toString();
  }

  @Override
  public boolean equals(Object other) {
    if(!(other instanceof Inbox)) {
      return false;
    }
    return ((Inbox) other).getAddress().equalsIgnoreCase(getAddress());
  }
  
  @Override
  public int hashCode() {
    return getAddress().hashCode();
  }

}