/*
 * $Id$
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
     * Get the status of a license on a product.
     */
    public License getLicense( String productIdentifier );

    /**
     * Return a list of all licenses for this server
     */
    public List<License> getLicenses();

    /**
     * Return true if the user has any premium products.
     */
    public boolean hasPremiumLicense();
}
