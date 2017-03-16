/**
 * $Id$
 */
package com.untangle.app.smtp.quarantine;

import java.io.File;

/**
 * Callback interface from the Quarantine system. This is called as mails are being ejected (rescued or purged) from the
 * system. <br>
 * <br>
 * Implementations must "remove" the file from the Quarantine, but may do so by copying to another location.
 */
public interface QuarantineEjectionHandler
{

    /**
     * Eject the given mail.
     * 
     * @param record
     *            the record (metadata) for the mail
     * @param inboxAddress
     *            the inbox which contained the mail
     * @param recipients
     *            the recipient(s) of the mail. This may or may not contain the inboxAddress.
     * @param data
     *            the data file (MIME).
     */
    public void ejectMail(InboxRecord record, String inboxAddress, String[] recipients, File data);

}
