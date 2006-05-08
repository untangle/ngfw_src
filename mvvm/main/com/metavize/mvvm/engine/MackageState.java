/*
 * Copyright (c) 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;

/**
 * State of mackage.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="MACKAGE_STATE"
 */
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

    /**
     * @hibernate.id
     * column="ID"
     * generator-class="native"
     */
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
     * @hibernate.property
     * column="MACKAGE_NAME"
     */
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
     * @hibernate.property
     * column="EXTRA_NAME"
     */
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
     * @hibernate.property
     * column="ENABLED"
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
