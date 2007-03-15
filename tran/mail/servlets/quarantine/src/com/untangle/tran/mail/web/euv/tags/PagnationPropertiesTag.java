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
package com.untangle.tran.mail.web.euv.tags;

import com.untangle.tran.mail.papi.quarantine.InboxRecordCursor;
import com.untangle.tran.mail.web.euv.Constants;
import com.untangle.tran.mail.web.euv.Util;
import java.net.URLEncoder;

import javax.servlet.ServletRequest;

public final class PagnationPropertiesTag extends SingleValueTag {

  private static final String KEY = "untangle.pagnationproperties.rowsperpage";
  private String m_propName;

  public String getPropName() {
    return m_propName;
  }
  public void setPropName(String n) {
    m_propName = n;
  }

  public static final void setCurrentRowsPerPAge(ServletRequest request,
    String rows) {
    request.setAttribute(KEY, rows);
  }
  public static final void clearCurretRowsPerPAge(ServletRequest request) {
    request.removeAttribute(KEY);
  }

  /**
   * Returns null if there is no current number of rows
   */
  public static String getCurrentRowsPerPAge(ServletRequest request) {
    return (String) request.getAttribute(KEY);
  }
  static boolean hasCurrentRowsPerPAge(ServletRequest request) {
    return getCurrentRowsPerPAge(request) != null;
  }   

  @Override
  protected String getValue() {
    InboxRecordCursor cursor = InboxIndexTag.getCurrentIndex(pageContext.getRequest());
    if(cursor == null) {
      return "";
    }

    if(getPropName().equalsIgnoreCase("sorting")) {
      return Util.sortByToString(cursor.getSortedBy());
    }
    else if(getPropName().equalsIgnoreCase("ascending")) {
      return "" + cursor.isAscending();
    }
    else if(getPropName().equalsIgnoreCase("prevId")) {
      return "" + cursor.getPrevStartingAt();
    }
    else if(getPropName().equalsIgnoreCase("nextId")) {
      return "" + cursor.getNextStartingAt();
    }
    else if(getPropName().equalsIgnoreCase("thisId")) {
      return "" + cursor.getCurrentStartingAt();
    }
    else if(getPropName().equalsIgnoreCase("rPPOption")) {
      return getCurrentRowsPerPAge(pageContext.getRequest());
    }         

    return "";
  }
}
