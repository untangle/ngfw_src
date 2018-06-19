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
     * @return Total size of all inboxes.
     * @throws QuarantineUserActionFailedException if action failed.
     */
    public long getInboxesTotalSize() throws QuarantineUserActionFailedException;

    /**
     * Total size of the entire store (in kilobytes (inMB = false) or megabytes (inMB = true))
     * @param inMB If true, return value in MB, otherwise return in bytes.
     * @return String of size.
     */
    public String getFormattedInboxesTotalSize(boolean inMB);

    /**
     * List all inboxes maintained by this Quarantine
     * 
     * @return the list of all inboxes
     * @throws QuarantineUserActionFailedException if action failed.
     */
    public List<InboxSummary> listInboxes() throws QuarantineUserActionFailedException;

    /**
     * Delete the given inbox, even if there are messages within. This does <b>not</b> prevent the account from
     * automagically being recreated next time SPAM is sent its way.
     * 
     * @param account
     *            the email address
     * @throws NoSuchInboxException                If inbox does not exist.
     * @throws QuarantineUserActionFailedException if action failed.
     */
    public void deleteInbox(String account) throws NoSuchInboxException, QuarantineUserActionFailedException;

    /**
     * Delete all inboxes from list.
     * @param  accounts                            Array of email addresses.
     * @throws NoSuchInboxException                If inbox does not exist.
     * @throws QuarantineUserActionFailedException if action failed.
     */
    public void deleteInboxes(String[] accounts) throws NoSuchInboxException, QuarantineUserActionFailedException;

    /**
     * Release all messages for account.
     * @param  account                             Email address.
     * @throws NoSuchInboxException                If inbox does not exist.
     * @throws QuarantineUserActionFailedException if action failed.
     */
    public void rescueInbox(String account) throws NoSuchInboxException, QuarantineUserActionFailedException;

    /**
     * Release all messages for all accounts.
     * @param  accounts                            List of email addresses.
     * @throws NoSuchInboxException                If inbox does not exist.
     * @throws QuarantineUserActionFailedException if action failed.
     */
    public void rescueInboxes(String[] accounts) throws NoSuchInboxException, QuarantineUserActionFailedException;
}
