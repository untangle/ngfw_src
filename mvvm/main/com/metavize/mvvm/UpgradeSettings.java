/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: UpgradeSettings.java,v 1.6 2005/02/25 02:45:29 amread Exp $
 */

package com.metavize.mvvm;

import java.io.Serializable;

/**
 * Describes the upgrade preferences.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="upgrade_settings"
 */
public class UpgradeSettings implements Serializable
{
    private static final long serialVersionUID = -6231213396376580006L;

    private boolean autoUpgrade;
    private Long id;
    private Period period;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate settings.
     */
    public UpgradeSettings() { }

    public UpgradeSettings(Period period)
    {
        this.period = period;
    }

    // accessors --------------------------------------------------------------

    /**
     * @hibernate.id
     * column="UPGRADE_SETTINGS_ID"
     * generator-class="native"
     */
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
     *
     * @return true if we autoupgrade.
     * @hibernate.property
     * column="AUTO_UPGRADE"
     * not-null="true"
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
     * @hibernate.many-to-one
     * column="PERIOD"
     * not-null="true"
     * cascade="all"
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
