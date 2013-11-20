/**
 * $Id$
 */
package com.untangle.node.smtp.web.euv.tags;

import com.untangle.node.smtp.quarantine.InboxRecord;

/**
 * Works with InboxIndexTag (i.e. must be within one).
 *
 */
@SuppressWarnings("serial")
public final class InboxRecordHasAttachmentTag extends IfElseTag
{
    @Override
    protected boolean isConditionTrue()
    {
        InboxRecord record = InboxRecordTag.getCurrent(pageContext);
        return record==null?false:record.getMailSummary().getAttachmentCount() > 0;
    }
}


