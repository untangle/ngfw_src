/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.uvm.toolbox;

import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.Period;

/**
 * Describes the upgrade preferences.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="u_upgrade_settings", schema="settings")
public class UpgradeSettings implements Serializable
{
    private static final long serialVersionUID = -6231213396376580006L;

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

    @Id
    @Column(name="upgrade_settings_id")
    @GeneratedValue
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
     */
    @Column(name="auto_upgrade", nullable=false)
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
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="period", nullable=false)
    public Period getPeriod()
    {
        return period;
    }

    public void setPeriod(Period period)
    {
        this.period = period;
    }
}
