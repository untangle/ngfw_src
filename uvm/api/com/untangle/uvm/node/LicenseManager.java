/*
 * $Id: LicenseManager.java 30329 2011-12-03 22:54:14Z dmorris $
 */
package com.untangle.uvm.node;

import java.util.List;

public interface LicenseManager
{
    /**
     * Reload all of the available licenses.
     */
    public void reloadLicenses();

    /**
     * Return a list of all licenses for this server
     */
    public List<License> getLicenses();

    /**
     * Get the status of a license on a product.
     */
    public License getLicense( String productIdentifier );

    /**
     * Checks is a the license for a given product exists and is currently valid
     */
    public boolean isLicenseValid( String productIdentifier );

    /**
     * Return true if the user has any premium products.
     */
    public boolean hasPremiumLicense();
}
