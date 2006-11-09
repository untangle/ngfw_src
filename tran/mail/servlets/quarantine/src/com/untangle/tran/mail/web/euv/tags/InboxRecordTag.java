/*
 * Copyright (c) 2003-2006 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.untangle.tran.mail.web.euv.tags;

import com.untangle.tran.mail.papi.quarantine.InboxRecord;
import com.untangle.tran.util.JSEscape;
import javax.servlet.jsp.PageContext;


/**
 * Works with InboxIndexTag
 * 
 */
public final class InboxRecordTag
  extends SingleValueTag {

  private static final String INBOX_RECORD_PS_KEY = "untangle.inbox_record";

  public static final String MID_PROP = "mid";
  public static final String FROM_PROP = "from";
  public static final String SUBJECT_PROP = "subject";
  public static final String SCORE_PROP = "detail";
  public static final String DATE_PROP = "idate";
  public static final String SIZE_PROP = "size";

  private String m_propName;
  private boolean m_jsEscape = true;

  public void setProp(String s) {
    m_propName = s;
  }
  public String getProp() {
    return m_propName;
  }

  public void setJSEscape(boolean escape) {
    m_jsEscape = escape;
  }

  public boolean isJSEscape() {
    return m_jsEscape;
  }

  @Override
  protected String getValue() {
    String ret = propNameToProp(getCurrent(pageContext), getProp());
    ret = m_jsEscape?JSEscape.escapeJS(ret):ret;
    return ret;
  }

  private String propNameToProp(InboxRecord record, String name) {
    if(record == null || name == null) {
      return null;
    }
    name = name.trim().toLowerCase();
    if(name.equals(MID_PROP)) {
      return record.getMailID();
    }
    if(name.equals(FROM_PROP)) {
      return record.getMailSummary().getSender();
    }
    if(name.equals(SUBJECT_PROP)) {
      return record.getMailSummary().getSubject();
    }
    if(name.equals(SCORE_PROP)) {
        return record.getMailSummary().getFormattedQuarantineDetail();
    }
    if(name.equals(DATE_PROP)) {
        return record.getFormattedDate();
    }
    if(name.equals(SIZE_PROP)) {
        return record.getFormattedSize();
    }
    return null;          
  }

  /**
   * Returns null if not found
   */
  public static InboxRecord getCurrent(PageContext pageContext) {
    return (InboxRecord) pageContext.getAttribute(INBOX_RECORD_PS_KEY);
  }

  public static void setCurrent(PageContext pageContext, InboxRecord record) {
    pageContext.setAttribute(INBOX_RECORD_PS_KEY, record, PageContext.PAGE_SCOPE);
  }

  public void release() {
    m_jsEscape = true;
    super.release();
  }   
}
