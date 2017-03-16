/**
 * $Id$
 */
package com.untangle.app.smtp.quarantine;

import java.util.List;

import com.untangle.app.smtp.quarantine.store.InboxSummary;

/**
 * Interface for Admins to browse/manipulate the Quarantine.
 */
public interface QuarantineMaintenenceView extends QuarantineManipulation
{

    /**
     * Total size of the entire store (in bytes)
     */
    public long getInboxesTotalSize() throws QuarantineUserActionFailedException;

    /**
     * Total size of the entire store (in kilobytes (inMB = false) or megabytes (inMB = true))
     */
    public String getFormattedInboxesTotalSize(boolean inMB);

    /**
     * List all inboxes maintained by this Quarantine
     * 
     * @return the list of all inboxes
     */
    public List<InboxSummary> listInboxes() throws QuarantineUserActionFailedException;

    /**
     * Delete the given inbox, even if there are messages within. This does <b>not</b> prevent the account from
     * automagically being recreated next time SPAM is sent its way.
     * 
     * @param account
     *            the email address
     */
    public void deleteInbox(String account) throws NoSuchInboxException, QuarantineUserActionFailedException;

    public void deleteInboxes(String[] accounts) throws NoSuchInboxException, QuarantineUserActionFailedException;

    public void rescueInbox(String account) throws NoSuchInboxException, QuarantineUserActionFailedException;

    public void rescueInboxes(String[] accounts) throws NoSuchInboxException, QuarantineUserActionFailedException;
}
