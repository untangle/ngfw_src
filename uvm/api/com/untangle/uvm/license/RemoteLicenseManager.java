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

import java.util.Date;

public interface RemoteLicenseManager
{
    /**
     * Reload all of the available licenses.
     */
    public void reloadLicenses();

    /**
     * Get the status of a license on a product.
     */
    public LicenseStatus getLicenseStatus( String productIdentifier );
}
