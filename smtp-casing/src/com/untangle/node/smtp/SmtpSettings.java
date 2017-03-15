/**
 * $Id$
 */
package com.untangle.node.smtp;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import com.untangle.node.smtp.quarantine.QuarantineSettings;
import com.untangle.node.smtp.safelist.SafelistSettings;

/**
 * Mail casing settings.
 */
@SuppressWarnings("serial")
public class SmtpSettings implements Serializable
{
    private boolean smtpEnabled = true;

    public static final long TIMEOUT_MAX = 86400000l;
    public static final long TIMEOUT_MIN = 0l;

    private long smtpTimeout;

    private QuarantineSettings quarantineSettings;
    private LinkedList<SafelistSettings> safelistSettings;

    // constructors -----------------------------------------------------------

    public SmtpSettings() {
    }

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
     * Quarantine properties associated with this casing.
     */
    public QuarantineSettings getQuarantineSettings()
    {
        return quarantineSettings;
    }

    public void setQuarantineSettings(QuarantineSettings s)
    {
        this.quarantineSettings = s;
    }

    /**
     * (actions cascade from parent to child/children and orphan children are deleted)
     * 
     * @return the list of Safelist settings
     */
    public List<SafelistSettings> getSafelistSettings()
    {
        if (safelistSettings != null)
            safelistSettings.removeAll(java.util.Collections.singleton(null));
        return safelistSettings;
    }

    public void setSafelistSettings(List<SafelistSettings> safelistSettings)
    {
        this.safelistSettings = new LinkedList<SafelistSettings>(safelistSettings);

        return;
    }
}
