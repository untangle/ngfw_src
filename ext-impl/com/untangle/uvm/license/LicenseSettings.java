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

class LicenseSettings
{
    private List<License> licenses;
    
    LicenseSettings( List<License> licenses )
    {
        this.licenses = licenses;
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
