/**
 * $Id: InboxSizeRecordsTag.java 34293 2013-03-17 05:22:02Z dmorris $
 */
package com.untangle.node.smtp.web.euv.tags;

import com.untangle.node.smtp.quarantine.InboxRecord;

/**
 * Outputs the total size of records in the current index, or
 * unknown if there is no current index
 *
 */
@SuppressWarnings("serial")
public final class InboxSizeRecordsTag extends SingleValueTag {

    @Override
    protected String getValue() {
        InboxRecord[] iCursor = QuarantineFunctions.getCurrentIndex(pageContext.getRequest());
        try {
            return "(" + String.format("%01.1f", new Float(iCursor.length / 1024.0)) + " KB)";
        }
        catch(Exception ex) { return "<unknown> KB"; }
    }
}
