/*
 * $Id: DefaultLicenseManagerImpl.java,v 1.00 2011/08/24 10:46:08 dmorris Exp $
 */
package com.untangle.uvm.engine;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.uvm.node.License;
import com.untangle.uvm.node.LicenseManager;

public class DefaultLicenseManagerImpl implements LicenseManager
{
    private final Logger logger = Logger.getLogger(getClass());

    private final List<License> licenses = new LinkedList<License>();
    
    DefaultLicenseManagerImpl() {}

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
        else if ("untangle-node-ips".equals(identifier)) return null;
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
