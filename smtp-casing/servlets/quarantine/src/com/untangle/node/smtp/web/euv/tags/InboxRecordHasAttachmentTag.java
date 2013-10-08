/**
 * $Id: InboxRecordHasAttachmentTag.java 34293 2013-03-17 05:22:02Z dmorris $
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


