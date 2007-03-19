/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: SpamSettings.java 8965 2007-02-23 20:54:04Z cng $
 */

package com.untangle.tran.clamphish;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import com.untangle.mvvm.security.Tid;
import com.untangle.tran.spam.SpamSettings;

/**
 * Settings for the ClamPhish transform.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@PrimaryKeyJoinColumn(name="settings_id")
@Table(name="tr_clamphish_settings", schema="settings")
public class ClamPhishSettings extends SpamSettings implements Serializable
{
    // XXX
    //private static final long serialVersionUID = -7246008133224040004L;

    private boolean enableGooglePhishList = true;

    // constructors -----------------------------------------------------------

    public ClamPhishSettings() {}

    public ClamPhishSettings(Tid tid)
    {
        super(tid);
    }

    // accessors --------------------------------------------------------------

    @Column(name="enable_google_sb")
    public boolean getEnableGooglePhishList()
    {
        return enableGooglePhishList;
    }

    public void setEnableGooglePhishList(boolean enableGooglePhishList)
    {
        this.enableGooglePhishList = enableGooglePhishList;
    }
}
