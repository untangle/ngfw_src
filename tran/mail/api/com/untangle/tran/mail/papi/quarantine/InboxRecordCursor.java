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

package com.untangle.tran.mail.papi.quarantine;

import com.untangle.tran.util.Pair;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Hack class, for providing cursor-like functionality
 * when going through InboxRecords.  This is a hack because
 * the whole thing is fake, as we have access to the entire
 * set. 
 */
public final class InboxRecordCursor
  implements Iterable<InboxRecord> {

  private int m_thisStartingWith;
  private int m_firstID;
  private int m_lastID;
  private InboxRecord[] m_records;
  private long m_inboxCount;
  private long m_inboxSize;
  private boolean m_hasNext;
  private boolean m_hasPrev;
  private InboxRecordComparator.SortBy m_sortedBy;
  private boolean m_ascending;

  private InboxRecordCursor(
    int thisStartingWith,
    int firstID,
    int lastID,
    InboxRecord[] records,
    int inboxCount,
    long inboxSize,
    boolean hasNext,
    boolean hasPrev,
    InboxRecordComparator.SortBy sortBy,
    boolean ascending) {

    m_thisStartingWith = thisStartingWith;
    m_firstID = firstID;
    m_lastID = lastID;
    m_records = records;
    m_inboxCount = inboxCount;
    m_inboxSize = inboxSize;
    m_hasNext = hasNext;
    m_hasPrev = hasPrev;
    m_sortedBy = sortBy;
    m_ascending = ascending;
  }

  /**
   * Get the <i>Cur</i>rent <i>S</i>et <i>o</i>f <i>R</i>ecords.
   */
  public InboxRecord[] getRecords() {
    return m_records;
  }

  /**
   * Get an iterator over the {@link #getRecords current records}
   */
  public Iterator<InboxRecord> iterator() {
    return Arrays.asList(m_records).iterator();
  }

  /**
   * Get the number of records in the current set.
   */
  public int size() {
    return m_records.length;
  }

  /**
   * Get the total number of records in the current set.
   */
  public long inboxCount() {
    return m_inboxCount;
  }
  
  /**
   * Get the total size of records in the current set.
   */
  public long inboxSize() {
    return m_inboxSize;
  }
  
  /**
   * Is there a logical "next" to the current records, based on
   * its position in the larger set
   */
  public boolean hasNext() {
    return m_hasNext;
  }
  
  /**
   * Is there a logical "previous" to the current records, based on
   * its position in the larger set
   */
  public boolean hasPrev() {
    return m_hasPrev;
  }

  /**
   * If {@link #hasNext there is a next set}, this defines the starting
   * index to be passed to {@link #get get}.
   */
  public int getNextStartingAt() {
    return m_lastID;
  }

  /**
   * If {@link hasPrev there is a previous set}, this defines the starting
   * index to be passed to {@link #get get}.
   */   
  public int getPrevStartingAt(int windowSz) {
    int ret = m_firstID - windowSz;
    return ret<0?0:ret;
  }

  public int getCurrentStartingAt() {
    return m_thisStartingWith;
  }

  public InboxRecordComparator.SortBy getSortedBy() {
    return m_sortedBy;
  }

  public boolean isAscending() {
    return m_ascending;
  }

  public static InboxRecordCursor get(
    InboxRecord[] allRecords,
    InboxRecordComparator.SortBy sortBy,
    boolean ascending,
    int startingAt,
    int windowSz) {

    Arrays.sort(allRecords, InboxRecordComparator.getComparator(sortBy, ascending));

    if(startingAt >= allRecords.length) {
      startingAt = allRecords.length - 1 - windowSz;
    }
    
    if(startingAt < 0) {
      startingAt = 0;
    }

    if((startingAt + windowSz) > allRecords.length) {
      windowSz = allRecords.length - startingAt;
    }

    InboxRecord[] ret = new InboxRecord[windowSz];
    System.arraycopy(allRecords, startingAt, ret, 0, windowSz);

    long inboxSize = 0;
    for (InboxRecord iRecord : allRecords) {
        inboxSize += iRecord.getSize();
    }

    return new InboxRecordCursor(startingAt,
      startingAt,
      startingAt + windowSz,
      ret,
      allRecords.length,
      inboxSize,
      startingAt + windowSz < allRecords.length,
      startingAt > 0,
      sortBy,
      ascending);
  }
}
