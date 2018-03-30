/**
 * $Id$
 */
package com.untangle.app.smtp;

import com.untangle.app.smtp.quarantine.QuarantineAppView;
import com.untangle.app.smtp.safelist.SafelistAppView;

public interface MailExport
{
    /**
     * Return export settings.
     * @return SmtpSettins object.
     */
    SmtpSettings getExportSettings();

    /**
     * Access the Object which is used to submit Mails to the quarantine.
     * 
     * @return the QuarantineAppView
     */
    QuarantineAppView getQuarantineAppView();

    /**
     * Access the object used to consult the Safelist manager while processing mails
     * 
     * @return the SafelistAppView
     */
    SafelistAppView getSafelistAppView();
}
