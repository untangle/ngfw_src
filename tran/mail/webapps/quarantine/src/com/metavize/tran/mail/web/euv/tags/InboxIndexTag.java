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
package com.metavize.tran.mail.web.euv.tags;

import javax.servlet.ServletRequest;
import com.metavize.tran.mail.papi.quarantine.InboxIndex;
import com.metavize.tran.mail.papi.quarantine.InboxRecord;
import java.util.Iterator;


/**
 *
 */
public final class InboxIndexTag
  extends IteratingTag<InboxRecord> {

  private static final String INBOX_INDEX_KEY = "metavize.inbox_index";

  @Override
  protected Iterator<InboxRecord> createIterator() {
    return hasCurrentIndex(pageContext.getRequest())?
      getCurrentIndex(pageContext.getRequest()).iterator():
      null;
  }

  @Override
  protected void setCurrent(InboxRecord s) {
    InboxRecordTag.setCurrent(pageContext, s);
  }

  public static final void setCurrentIndex(ServletRequest request,
    InboxIndex index) {
    request.setAttribute(INBOX_INDEX_KEY, index);
  }
  public static final void clearCurrentIndex(ServletRequest request) {
    request.removeAttribute(INBOX_INDEX_KEY);
  }

  /**
   * Returns null if there is no index
   */
  static InboxIndex getCurrentIndex(ServletRequest request) {
    return (InboxIndex) request.getAttribute(INBOX_INDEX_KEY);
  }

  static boolean hasCurrentIndex(ServletRequest request) {
    InboxIndex index = getCurrentIndex(request);
    return index != null && index.size() > 0;
  }
}
