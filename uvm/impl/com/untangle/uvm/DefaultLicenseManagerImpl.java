/**
 * $Id$
 */

package com.untangle.uvm;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.uvm.app.License;
import com.untangle.uvm.app.LicenseManager;
import com.untangle.uvm.util.I18nUtil;

/**
 * The Default License Manager
 */
public class DefaultLicenseManagerImpl implements LicenseManager
{
    private final Logger logger = Logger.getLogger(getClass());

    private final List<License> licenses = new LinkedList<License>();

    /**
     * Constructor
     */
    DefaultLicenseManagerImpl()
    {
    }

    /**
     * Reload all of the available licenses
     * 
     * @param blocking
     *        Blocking operation flag
     */
    public void reloadLicenses(boolean blocking)
    {
        //no-op
        return;
    }

    /**
     * Get the status of a license on a product.
     * 
     * @param identifier
     *        The product identifier
     * @return The license status
     */
    public boolean isLicenseValid(String identifier)
    {
        if (isGPLApp(identifier)) return true;
        else return false; /*
                            * always return false as the real license manager is
                            * needed for valid licenses
                            */
    }

    /**
     * Get the status of a license on a product
     * 
     * @param identifier
     *        The product identifier
     * @return The corresponding License or null for invalid identifier
     */
    public License getLicense(String identifier)
    {
        if (isGPLApp(identifier)) return null;

        /**
         * This returns an invalid license for all requests Note: this includes
         * the free apps, however they don't actually check the license so it
         * won't effect behavior The UI will request the license of all app
         * (including free)
         */
        logger.info("License Manager is not loaded. Returing invalid license for " + identifier + ".");
        return new License(identifier, "0000-0000-0000-0000", identifier, "Subscription", 0, 0, "invalid", 1, Boolean.FALSE, I18nUtil.marktr("No License Found"));
    }

    /**
     * Get the license for a product. TODO - It seems like this function calls
     * itself, which would cause and endless loop.
     * 
     * @param productIdentifier
     *        The product identifier
     * @param exactMatch
     *        Exact match flag
     * @return
     */
    public License getLicense(String productIdentifier, boolean exactMatch)
    {
        return getLicense(productIdentifier, true);
    }

    /**
     * Get a list of all licenses
     * 
     * @return A list of all licenses
     */
    public List<License> getLicenses()
    {
        return this.licenses;
    }

    /**
     * Requests a trial license
     * 
     * @param appName
     *        The application name for the request
     * @throws Exception
     */
    public void requestTrialLicense(String appName) throws Exception
    {
        return;
    }

    /**
     * Check for availability of a premium license
     * 
     * @return True if a premium license is available, otherwise false
     */
    public boolean hasPremiumLicense()
    {
        return false;
    }

    /**
     * Get the valid license count
     * 
     * @return The valid license count
     */
    public int validLicenseCount()
    {
        return 0;
    }

    /**
     * Get the seat limit
     * 
     * @return The seat limit
     */
    public int getSeatLimit()
    {
        return -1;
    }

    /**
     * Get the seat limit with lienency
     * 
     * @param lienency
     *        The lienency flag
     * @return The seat limit with lienency
     */
    public int getSeatLimit(boolean lienency)
    {
        return -1;
    }

    /**
     * Check to see if an application is a GPL application
     * 
     * @param identifier
     *        The application identifier
     * @return True for a GPL app, otherwise false
     */
    private boolean isGPLApp(String identifier)
    {
        if ("ad-blocker".equals(identifier)) return true;
        else if ("virus-blocker-lite".equals(identifier)) return true;
        else if ("captive-portal".equals(identifier)) return true;
        else if ("firewall".equals(identifier)) return true;
        else if ("intrusion-prevention".equals(identifier)) return true;
        else if ("openvpn".equals(identifier)) return true;
        else if ("phish-blocker".equals(identifier)) return true;
        else if ("application-control-lite".equals(identifier)) return true;
        else if ("reports".equals(identifier)) return true;
        else if ("router".equals(identifier)) return true;
        else if ("shield".equals(identifier)) return true;
        else if ("spam-blocker-lite".equals(identifier)) return true;
        else if ("web-monitor".equals(identifier)) return true;
        else if ("tunnel-vpn".equals(identifier)) return true;

        if ("license".equals(identifier)) return true;
        else if ("http".equals(identifier)) return true;
        else if ("ftp".equals(identifier)) return true;
        else if ("smtp".equals(identifier)) return true;

        return false;
    }
}
