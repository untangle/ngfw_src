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

package com.untangle.uvm.license;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;

@SuppressWarnings("serial")
public class LicenseSettings implements java.io.Serializable
{
    private Integer settingsVersion = new Integer(1);

    private List<License> licenses;
    
    public LicenseSettings( )
    {
        this.licenses = new LinkedList<License>();
    }

    public LicenseSettings( List<License> licenses )
    {
        this.licenses = licenses;
    }


    public Integer getSettingsVersion()
    {
        return settingsVersion;
    }

    public void setSettingsVersion( Integer settingsVersion )
    {
        this.settingsVersion = settingsVersion;
    }

    public List<License> getLicenses()
    {
        return this.licenses;
    }

    public void setLicenses(List<License> licenses)
    {
        this.licenses = licenses;
    }
}
