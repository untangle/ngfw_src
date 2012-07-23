/**
 * $Id$
 */
package com.untangle.node.mail.papi;

import java.io.Serializable;
import java.util.List;
import com.untangle.node.mail.papi.quarantine.QuarantineSettings;
import com.untangle.node.mail.papi.safelist.SafelistSettings;

/**
 * Mail casing settings.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class MailNodeSettings implements Serializable
{
    private boolean smtpEnabled = true;
    private boolean popEnabled = true;
    private boolean imapEnabled = true;

    public static final long TIMEOUT_MAX = 86400000l;
    public static final long TIMEOUT_MIN = 0l;

    private long smtpTimeout;
    private long popTimeout;
    private long imapTimeout;
    private QuarantineSettings quarantineSettings;
    private List<SafelistSettings> safelistSettings;
    private boolean smtpAllowTLS;

    // constructors -----------------------------------------------------------

    public MailNodeSettings() { }

    // accessors --------------------------------------------------------------

    /**
     * Enabled status of SMTP casing.
     *
     * @return true if SMTP casing is enabled, false otherwise.
     */
    public boolean isSmtpEnabled()
    {
        return smtpEnabled;
    }

    public void setSmtpEnabled(boolean smtpEnabled)
    {
        this.smtpEnabled = smtpEnabled;
    }

    /**
     * Enabled status of POP casing.
     *
     * @return true of POP casing is enabled, false otherwise.
     */
    public boolean isPopEnabled()
    {
        return popEnabled;
    }

    public void setPopEnabled(boolean popEnabled)
    {
        this.popEnabled = popEnabled;
    }

    /**
     * Enabled status of IMAP casing.
     *
     * @return true of IMAP casing is enabled, false otherwise.
     */
    public boolean isImapEnabled()
    {
        return imapEnabled;
    }

    public void setImapEnabled(boolean imapEnabled)
    {
        this.imapEnabled = imapEnabled;
    }

    /**
     * Timeout for SMTP traffic.
     *
     * @return for SMTP in millis.
     */
    public long getSmtpTimeout()
    {
        return smtpTimeout;
    }

    public void setSmtpTimeout(long smtpTimeout)
    {
        this.smtpTimeout = smtpTimeout;
    }

    /**
     * Whether or not to allow TLS.  Currently this controls both the extensions advertised
     * by the server, and commands allowed by the client.
     *
     * @return true if TLS is allowed, false if not allowed
     */
    public boolean getSmtpAllowTLS()
    {
        return smtpAllowTLS;
    }

    public void setSmtpAllowTLS(boolean smtpAllowTLS)
    {
         this.smtpAllowTLS = smtpAllowTLS;
    }

    /**
     * Timeout for POP traffic.
     *
     * @return timeout for POP in millis.
     */
    public long getPopTimeout()
    {
        return popTimeout;
    }

    public void setPopTimeout(long popTimeout)
    {
        this.popTimeout = popTimeout;
    }

    /**
     * Timeout for IMAP traffic.
     *
     * @return timeout for IMAP in millis.
     */
    public long getImapTimeout()
    {
        return imapTimeout;
    }

    public void setImapTimeout(long imapTimeout)
    {
        this.imapTimeout = imapTimeout;
    }

    /**
     * Quarantine properties associated with this casing.
     */
    public QuarantineSettings getQuarantineSettings() {
        return quarantineSettings;
    }

    public void setQuarantineSettings(QuarantineSettings s) {
        this.quarantineSettings = s;
    }

    /**
     * (actions cascade from parent to child/children and
     *  orphan children are deleted)
     *
     * @return the list of Safelist settings
     */
    public List<SafelistSettings> getSafelistSettings()
    {
        if (safelistSettings != null) safelistSettings.removeAll(java.util.Collections.singleton(null));
        return safelistSettings;
    }

    public void setSafelistSettings(List<SafelistSettings> safelistSettings)
    {
        this.safelistSettings = safelistSettings;

        return;
    }
}
