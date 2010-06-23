/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.untangle.uvm.license;

import java.util.Date;

import org.apache.log4j.Logger;

public class RemoteLicenseManagerImpl implements RemoteLicenseManager
{
    private final Logger logger = Logger.getLogger(getClass());

    private final LocalLicenseManager licenseManager;

    RemoteLicenseManagerImpl(LocalLicenseManager licenseManager)
    {
        this.licenseManager = licenseManager;
    }

    /**
     * Reload all of the available licenses
     */
    public void reloadLicenses()
    {
        if (this.licenseManager == null) {
            logger
                    .warn("The license manager is not available, unable to load licenses");
            return;
        }

        licenseManager.reloadLicenses();
    }

    /**
     * Get the status of a license on a product.
     */
    public LicenseStatus getLicenseStatus(String identifier)
    {
        if (this.licenseManager == null) {
            /* no manager, so status is always no license */
            return new LicenseStatus(false, identifier, "no-mackage",
                    "invalid", new Date(0), "expired", false);
        }

        return this.licenseManager.getLicenseStatus(identifier);
    }

    /**
     * 
     */
    public synchronized LicenseStatus getMackageStatus(String mackageName)
    {
        if (this.licenseManager == null) {
            /* no manager, so status is always no license */
            return new LicenseStatus(false, "unknown", mackageName, "invalid",
                    new Date(0), "expired", false);
        }

        return this.licenseManager.getMackageStatus(mackageName);
    }

    /**
     * Return true if the user has any premium products.
     */
    public boolean hasPremiumLicense()
    {
        if (this.licenseManager == null) {
            /*
             * can only have a premium license if they have the full license
             * manager
             */
            return false;
        }

        return this.licenseManager.hasPremiumLicense();
    }

}
