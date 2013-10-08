/**
 * $Id: InboxNumRecordsTag.java 34293 2013-03-17 05:22:02Z dmorris $
 */
package com.untangle.node.smtp.web.euv.tags;

import com.untangle.node.smtp.quarantine.InboxRecord;

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
        InboxRecord[] iCursor = QuarantineFunctions.getCurrentIndex(pageContext.getRequest());
        try {
            return Long.toString(iCursor == null ? 0 : iCursor.length) + " mails";
        }
        catch(Exception ex) { return "<unknown> mails"; }
    }
}
