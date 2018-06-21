/**
 * $Id$
 */
package com.untangle.uvm.app;

import java.util.List;

public interface LicenseManager
{
    /**
     * Reload all of the available licenses.
     */
    public void reloadLicenses( boolean blocking );

    /**
     * Return a list of all licenses for this server
     */
    public List<License> getLicenses();

    /**
     * Get the status of a license on a product.
     */
    public License getLicense( String productIdentifier );

    /**
     * Get the status of a license on a product.
     * if exactMatch is true it looks for an exact match of productIndentifier
     * if exactMatch is false, it will accept any license whose identifier starts with productIndentifier
     */
    public License getLicense( String productIdentifier, boolean exactMatch );
    
    /**
     * Checks is a the license for a given product exists and is currently valid
     */
    public boolean isLicenseValid( String productIdentifier );

    /**
     * Return true if the user has any premium products.
     */
    public boolean hasPremiumLicense();

    /**
     * Returns the number of valid premium licenses
     */
    public int validLicenseCount();
    
    /**
     * Returns the max number of seats of the smallest valid non-trial premium license
     * If there are no valid non-trial premium liecnses, it will return -1
     */
    public int getSeatLimit();

    /**
     * Returns the max number of seats of the smallest valid non-trial premium license
     * If there are no valid non-trial premium liecnses, it will return -1
     * boolean specified if lienency should be applied to the seat
     */
    public int getSeatLimit( boolean lienency );
    
    /**
     * Requests a trial license from license server and refreshes licenses.
     * If it already has a valid license it does nothing and returns.
     * If it can not connect to the license server for the request it throws an exception
     * It does not guaratee a new license will be fetched.
     * The server can deny the request in which case this will return with no exception
     */
    public void requestTrialLicense( String appName ) throws Exception;
}
