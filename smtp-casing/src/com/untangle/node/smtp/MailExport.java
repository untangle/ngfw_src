/**
 * $Id: MailExport.java 34290 2013-03-17 00:00:19Z dmorris $
 */
package com.untangle.node.smtp;

import com.untangle.node.smtp.quarantine.QuarantineNodeView;
import com.untangle.node.smtp.safelist.SafelistNodeView;

public interface MailExport
{
    SmtpNodeSettings getExportSettings();

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
