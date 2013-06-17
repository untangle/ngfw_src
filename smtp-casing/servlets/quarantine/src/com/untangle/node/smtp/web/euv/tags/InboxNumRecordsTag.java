/**
 * $Id$
 */
package com.untangle.node.smtp.web.euv.tags;

import com.untangle.node.smtp.quarantine.InboxRecordCursor;

/**
 * Outputs the total number of records in the current index, or
 * unknown if there is no current index
 *
 */
@SuppressWarnings("serial")
public final class InboxNumRecordsTag extends SingleValueTag
{
    @Override
    protected String getValue() {
        InboxRecordCursor iCursor = QuarantineFunctions.getCurrentIndex(pageContext.getRequest());
        try {
            return Long.toString(iCursor == null ? 0 : iCursor.inboxCount()) + " mails";
        }
        catch(Exception ex) { return "<unknown> mails"; }
    }
}
