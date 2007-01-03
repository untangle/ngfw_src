/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.mail.impl.quarantine.store;

import com.untangle.tran.mail.papi.quarantine.InboxRecord;
import com.untangle.tran.mail.papi.quarantine.InboxIndex;
import com.untangle.tran.mail.papi.quarantine.InboxRecordComparator;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Collections;
import java.io.Serializable;

/**
 * Private implementation of InboxIndex
 */
public final class InboxIndexImpl
  extends HashMap<String, InboxRecord>
  implements InboxIndex, Serializable {

  private String m_address;
  private long m_timestamp;

  public void setOwnerAddress(String address) {
    m_address = address;
  }
  
  public String getOwnerAddress() {
    return m_address;
  }

  public long getLastAccessTimestamp() {
    return m_timestamp;
  }
  public void setLastAccessTimestamp(long timestamp) {
    m_timestamp = timestamp;
  }

  /**
   * Helper method which returns the timestamp of the most recently
   * added mail, or 0 if the inbox is empty.
   */
  public long getNewestMailTimestamp() {
    if(size() == 0) {
      return 0;
    }
    InboxRecord rec = Collections.max(this.values(), InboxRecordComparator.getComparator(
    InboxRecordComparator.SortBy.INTERN_DATE, true));
    return rec==null?0:rec.getInternDate();
  }

  public Iterator<InboxRecord> iterator() {
    return values().iterator();
  }

  public int inboxCount() {
      return size();
  }

  public long inboxSize() {
      long inboxSize = 0;
      for(InboxRecord iRecord : this) {
          inboxSize += iRecord.getSize();
      }
      return inboxSize;
  }

  public InboxRecord getRecord(String mailID) {
    return get(mailID);
  }
  public InboxRecord[] getAllRecords() {
    return (InboxRecord[]) values().toArray(new InboxRecord[size()]);
  }

  protected void debugPrint() {
    System.out.println("Access: " + getLastAccessTimestamp());
    System.out.println("Owner: " + getOwnerAddress());
    for(InboxRecord record : this) {
      System.out.println("----- RECORD -----");
      System.out.println("\tID: " + record.getMailID());
      System.out.println("\tDate: " + record.getInternDate());
      System.out.println("\tSize: " + record.getSize());
      System.out.println("\tSender: " + record.getMailSummary().getTruncatedSender());
      System.out.println("\tSubject: " + record.getMailSummary().getTruncatedSubject());
      System.out.println("\tAttchCount: " + record.getMailSummary().getAttachmentCount());
      System.out.println("\tCat: " + record.getMailSummary().getQuarantineCategory());
      System.out.println("\tDetail: " + record.getMailSummary().getQuarantineDetail());
    }
  }
}
