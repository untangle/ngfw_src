/**
 * $Id$
 */
package com.untangle.app.smtp;

import com.untangle.app.smtp.quarantine.QuarantineNodeView;
import com.untangle.app.smtp.safelist.SafelistNodeView;

public interface MailExport
{
    SmtpSettings getExportSettings();

    /**
     * Access the Object which is used to submit Mails to the quarantine.
     * 
     * @return the QuarantineNodeView
     */
    QuarantineNodeView getQuarantineNodeView();

    /**
     * Access the object used to consult the Safelist manager while processing mails
     * 
     * @return the SafelistNodeView
     */
    SafelistNodeView getSafelistNodeView();
}
