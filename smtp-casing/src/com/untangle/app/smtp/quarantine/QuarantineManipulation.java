/**
 * $Id$
 */
package com.untangle.app.smtp.quarantine;

import java.util.List;

/**
 * Base-interface for Admin and User views into the Quarantine
 */
public interface QuarantineManipulation
{

    /**
     * Purge the given mails for the named account. <br>
     * <br>
     * If the mails do not exist, this is silently ignored.
     * 
     * @param account
     *            the email address in question
     * @param doomedMails
     *            the {@link com.untangle.app.smtp.quarantine.InboxRecord#getMailID IDs} of the message(s) to be
     *            deleted.
     * 
     * @return the contents of the inbox <i>after</i> the purge operation has taken place
     * 
     * @exception NoSuchInboxException
     *                if there isn't such an inbox (duh)
     * @exception QuarantineUserActionFailedException
     *                if some back-end error occured preventing the operation from successful completion
     */
    public InboxIndex purge(String account, String... doomedMails) throws NoSuchInboxException,
            QuarantineUserActionFailedException;

    /**
     * Rescue the given mails for the named account, sending them to <code>account</code>. <br>
     * <br>
     * If the mails do not exist, this is silently ignored.
     * 
     * @param account
     *            the email address in question
     * @param rescuedMails
     *            the {@link com.untangle.app.smtp.quarantine.InboxRecord#getMailID IDs} of the message(s) to be
     *            rescued.
     * 
     * @return the contents of the inbox <i>after</i> the rescue operation has taken place
     * 
     * @exception NoSuchInboxException
     *                if there isn't such an inbox
     * @exception QuarantineUserActionFailedException
     *                if some back-end error occured preventing the operation from successful completion
     */
    public InboxIndex rescue(String account, String... rescuedMails) throws NoSuchInboxException,
            QuarantineUserActionFailedException;

    /**
     * Get a listing of the contents of the inbox for <code>account</code>
     * 
     * @param account
     *            the account (email address)
     * 
     * @return the contents of the inbox
     * 
     * @exception NoSuchInboxException
     *                if there isn't such an inbox
     * @exception QuarantineUserActionFailedException
     *                if some back-end error occured preventing the operation from successful completion
     */
    public List<InboxRecord> getInboxRecords(String account) throws NoSuchInboxException,
            QuarantineUserActionFailedException;

    /**
     * Total hack for servlets, to test if a connection is still alive
     */
    public void test();

}
