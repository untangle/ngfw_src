/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
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
import com.metavize.tran.util.JSEscape;
import javax.servlet.jsp.PageContext;


/**
 * Works with RPPIteratorTag
 * 
 */
public final class RPPCurrentOptionTag
  extends SingleValueTag {

  private static final String KEY = "metavize.RPPCurrentOptionTag";


  @Override
  protected String getValue() {
    return getCurrent(pageContext);
  }

  /**
   * Returns null if not found
   */
  public static String getCurrent(PageContext pageContext) {
    return (String) pageContext.getAttribute(KEY);
  }

  public static void setCurrent(PageContext pageContext, String s) {
    pageContext.setAttribute(KEY, s, PageContext.PAGE_SCOPE);
  }
}
