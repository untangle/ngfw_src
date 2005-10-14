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

import com.metavize.tran.mail.papi.quarantine.InboxIndex;


/**
 * Outputs the count of the index, or 0 if there
 * is no current inbox.  
 */
public final class InboxMsgCountTag
  extends SingleValueTag {

  @Override
  protected String getValue() {
    InboxIndex index = InboxIndexTag.getCurrentIndex(pageContext.getRequest());
    return Integer.toString(index==null?0:index.size());
  }
}
