/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.ftp;

import java.io.Serializable;

import com.metavize.mvvm.security.Tid;

/**
 * Ftp casing settings.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_FTP_SETTINGS"
 */
public class FtpSettings implements Serializable
{
    private static final long serialVersionUID = -828243820153242416L;

    private Long id;
    private Tid tid;

    private boolean enabled = true;

    // constructors -----------------------------------------------------------

    public FtpSettings() { }

    // accessors --------------------------------------------------------------

    /**
     * @hibernate.id
     * column="SETTINGS_ID"
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
     * Transform id for these settings.
     *
     * @return tid for these settings.
     * @hibernate.many-to-one
     * column="TID"
     * unique="true"
     * not-null="true"
     */
    public Tid getTid()
    {
        return tid;
    }

    public void setTid(Tid tid)
    {
        this.tid = tid;
    }

    /**
     * Enabled status for casing.
     *
     * @return true when casing is enabled, false otherwise.
     * @hibernate.property
     * column="ENABLED"
     * not-null="true"
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
