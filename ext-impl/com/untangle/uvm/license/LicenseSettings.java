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
    private final List<License> licenses;
    
    LicenseSettings( Collection<License> licenses )
    {
        this.licenses = Collections.unmodifiableList( new ArrayList<License>( licenses ));
    }

    /**
     * This is the current set of product licenses.  Perhap, this
     * should be a map of product names to the license?  Actually, the
     * same product may have multiple licenses, why delete the trial
     * license?  that will prevent it from getting trials twice.
     *
     * @return The product licenses.
     */
    public List<License> getLicenses()
    {
        return this.licenses;
    }
}
