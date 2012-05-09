/**
 * $Id$
 */
package com.untangle.uvm.toolbox;

import java.io.Serializable;

import com.untangle.uvm.Period;

/**
 * Describes the upgrade preferences.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class UpgradeSettings implements Serializable
{

    private Long id;
    private boolean autoUpgrade = true;
    private Period period;

    // constructors -----------------------------------------------------------

    public UpgradeSettings() { }

    public UpgradeSettings(Period period)
    {
        this.period = period;
    }

    // accessors --------------------------------------------------------------

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Specifies if apt-get upgrade should be run automatically after
     * an update.
     */
    public boolean getAutoUpgrade()
    {
        return autoUpgrade;
    }

    public void setAutoUpgrade(boolean autoUpgrade)
    {
        this.autoUpgrade = autoUpgrade;
    }

    /**
     * Specifies when apt-get update should be run.
     *
     * @return upgrade period.
     */
    public Period getPeriod()
    {
        return period;
    }

    public void setPeriod(Period period)
    {
        this.period = period;
    }
}
