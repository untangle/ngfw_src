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

import com.metavize.tran.mail.papi.quarantine.InboxRecordCursor;
import com.metavize.tran.mail.web.euv.Constants;
import com.metavize.tran.mail.web.euv.Util;
import java.net.URLEncoder;


/**
 *
 * 
 */
public final class PagnationPropertiesTag
  extends SingleValueTag {

  private String m_propName;


  public String getPropName() {
    return m_propName;
  }
  public void setPropName(String n) {
    m_propName = n;
  }


  @Override
  protected String getValue() {
    InboxRecordCursor cursor =
      InboxIndexTag.getCurrentIndex(pageContext.getRequest());
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
      return "" + cursor.getPrevStartingAt(Constants.RECORDS_PER_PAGE);
    }
    else if(getPropName().equalsIgnoreCase("nextId")) {
      return "" + cursor.getNextStartingAt();
    }
    else if(getPropName().equalsIgnoreCase("thisId")) {
      return "" + cursor.getCurrentStartingAt();
    }       
    return "";
  }

}
