/**
 * $Id$
 */
package com.untangle.node.smtp.quarantine.store;

import java.io.Serializable;

import com.untangle.node.smtp.quarantine.InboxRecord;
import com.untangle.node.smtp.quarantine.MailSummary;

/**
 * Private implementation of an Inbox record
 */
@SuppressWarnings("serial")
public final class InboxRecordImpl extends InboxRecord implements Serializable
{
    public InboxRecordImpl()
    {}

    public InboxRecordImpl(String mailID, long addedOn, MailSummary summary, String[] recipients)
    {

        super(mailID, addedOn, summary, recipients);

    }
}
