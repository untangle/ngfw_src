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

import com.metavize.tran.mail.papi.quarantine.InboxRecord;
import javax.servlet.jsp.PageContext;


/**
 * Works with InboxIndexTag
 * 
 */
public final class InboxRecordTag
  extends SingleValueTag {

  private static final String INBOX_RECORD_PS_KEY = "metavize.inbox_record";

  public static final String MID_PROP = "mid";
  public static final String FROM_PROP = "from";
  public static final String SUBJECT_PROP = "subject";
  public static final String SCORE_PROP = "detail";

  private String m_propName;


  public void setProp(String s) {
    m_propName = s;
  }
  public String getProp() {
    return m_propName;
  }
  
  @Override
  protected String getValue() {
    return propNameToProp(getCurrent(pageContext), getProp());
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
      return record.getMailSummary().getQuarantineDetail();
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
}
