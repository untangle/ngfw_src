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
package com.untangle.node.mail.web.euv.tags;

import com.untangle.node.mail.papi.quarantine.InboxRecord;


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


