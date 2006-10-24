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


/**
 * Works with InboxIndexTag (i.e. must be within one).
 * 
 */
public final class InboxRecordHasAttachmentTag
  extends IfElseTag {

  @Override
  protected boolean isConditionTrue() {
    InboxRecord record = InboxRecordTag.getCurrent(pageContext);
    return record==null?false:record.getMailSummary().getAttachmentCount() > 0;
  }
}  


