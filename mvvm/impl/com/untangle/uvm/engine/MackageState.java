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

package com.untangle.mvvm.engine;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * State of mackage.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="mackage_state", schema="settings")
class MackageState
{
    private Long id;
    private String mackageName;
    private String extraName;
    private boolean enabled;

    MackageState() { }

    MackageState(String mackageName, String extraName, boolean enabled)
    {
        this.mackageName = mackageName;
        this.extraName = extraName;
        this.enabled = enabled;
    }

    @Id
    @Column(name="id")
    @GeneratedValue
    private Long getId()
    {
        return id;
    }

    private void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Name of mackage.
     *
     * @return name of mackage.
     */
    @Column(name="mackage_name", nullable=false)
    public String getMackageName()
    {
        return mackageName;
    }

    public void setMackageName(String mackageName)
    {
        this.mackageName = mackageName;
    }

    /**
     * Extra name of mackage.
     *
     * @return the mackage's extra name.
     */
    @Column(name="extra_name")
    public String getExtraName()
    {
        return extraName;
    }


    public void setExtraName(String extraName)
    {
        this.extraName = extraName;
    }

    /**
     * Status of transform.
     *
     * @return true if and only if mackage is enabled.
     */
    @Column(nullable=false)
    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }
}
