/**
 * $Id$
 */
package com.untangle.app.smtp.quarantine;

import org.json.JSONObject;

import java.io.File;
import java.util.List;

import javax.mail.internet.InternetAddress;

/**
 * Interface for the apps to insert messages into the quarantine. This is not intended to be "remoted".
 */
public interface QuarantineAppView
{

    /**
     * Quarantine the given message, destined for the named recipients. <br>
     * <br>
     * Callers should be prepared for the case that after making this call, the underlying File from the MIMEMessage may
     * have been "stolen" (moved).
     * 
     * @param file
     *            the file containing the message to be quarantined
     * @param summary
     *            a summary of the mail
     * @param recipients
     *            any recipients for the mail
     * 
     * @return true if the mail was quarantined.
     */
    public boolean quarantineMail(File file, MailSummary summary, InternetAddress... recipients);

    // --QuarantineManipulation--

    JSONObject listInboxesV2() throws QuarantineUserActionFailedException;

    // --QuarantineManipulation--
    InboxIndex purgeV2(String account, String... doomedMails) throws NoSuchInboxException,
            QuarantineUserActionFailedException;

    InboxIndex rescueV2(String account, String... rescuedMails) throws NoSuchInboxException,
            QuarantineUserActionFailedException;

    List<InboxRecord> getInboxRecordsV2(String account) throws NoSuchInboxException,
            QuarantineUserActionFailedException;

    /**
     * Generate token.
     * @param  account Account to generate token for.
     * @return         String of token.
     */
    public String createAuthToken(String account);
}