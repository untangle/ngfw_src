/**
 * $Id$
 */
package com.untangle.app.ftp;

import java.io.Serializable;

/**
 * Ftp casing settings.
 */
@SuppressWarnings("serial")
public class FtpSettings implements Serializable
{
    private boolean enabled = true;

    public FtpSettings() { }

    /**
     * Enabled status for casing.
     *
     * @return true when casing is enabled, false otherwise.
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }
}
