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

public class NilLicenseManagerImpl implements LicenseManager
{
    private final Logger logger = Logger.getLogger(getClass());

    NilLicenseManagerImpl() {}

    /**
     * Reload all of the available licenses
     */
    public void reloadLicenses()
    {
        //no-op
        return;
    }

    /**
     * Get the status of a license on a product.
     */
    public License getLicense(String identifier)
    {
        logger.error("License Manager is not loaded. Failed to return license: " + identifier);
        return new License(identifier, identifier, "Subscription", 0, 0, "invalid", 1, Boolean.FALSE);
    }

    /**
     * Return true if the user has any premium products.
     */
    public boolean hasPremiumLicense()
    {
        return false;
    }

}
