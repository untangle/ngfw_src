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
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

public class NilLicenseManagerImpl implements LicenseManager
{
    private final Logger logger = Logger.getLogger(getClass());

    private final List<License> licenses = new LinkedList<License>();
    
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
        if ("untangle-node-adblocker".equals(identifier)) return null;
        else if ("untangle-node-clam".equals(identifier)) return null;
        else if ("untangle-node-cpd".equals(identifier)) return null;
        else if ("untangle-node-firewall".equals(identifier)) return null;
        else if ("untangle-node-openvpn".equals(identifier)) return null;
        else if ("untangle-node-phish".equals(identifier)) return null;
        else if ("untangle-node-protofilter".equals(identifier)) return null;
        else if ("untangle-node-reporting".equals(identifier)) return null;
        else if ("untangle-node-shield".equals(identifier)) return null;
        else if ("untangle-node-spamassassin".equals(identifier)) return null;
        else if ("untangle-node-spyware".equals(identifier)) return null;
        else if ("untangle-node-webfilter".equals(identifier)) return null;

        /**
         * This returns an invalid license for all requests
         * Note: this includes the free apps, however they don't actually check the license so it won't effect behavior
         * The UI will request the license of all app (including free)
         */
        logger.info("License Manager is not loaded. Returing invalid license for " + identifier + ".");
        return new License(identifier, "0000-0000-0000-0000", identifier, "Subscription", 0, 0, "invalid", 1, Boolean.FALSE, "Invalid (No License Manager)");
    }

    public List<License> getLicenses()
    {
        return this.licenses;
    }
    
    /**
     * Return true if the user has any premium products.
     */
    public boolean hasPremiumLicense()
    {
        return false;
    }

}
