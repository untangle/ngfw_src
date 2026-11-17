/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.List;

import com.untangle.uvm.app.License;
import com.untangle.uvm.app.UserLicenseMessage;

/**
 * Reviewed callable operations for SafeLicenseManager.
 */
public interface SafeLicenseManager
{
    /**
     * getLicense through the reviewed RPC contract.
     * @param productIdentifier The productIdentifier argument
     * @return The delegated result
     */
    License getLicense(String productIdentifier);

    /**
     * getLicenseServerConnectivity through the reviewed RPC contract.
     * @return The delegated result
     */
    boolean getLicenseServerConnectivity();

    /**
     * getLicenses through the reviewed RPC contract.
     * @return The delegated result
     */
    List<License> getLicenses();

    /**
     * getUserLicenseMessages through the reviewed RPC contract.
     * @return The delegated result
     */
    List<UserLicenseMessage> getUserLicenseMessages();

    /**
     * isLicenseValid through the reviewed RPC contract.
     * @param productIdentifier The productIdentifier argument
     * @return The delegated result
     */
    boolean isLicenseValid(String productIdentifier);

    /**
     * isRestricted through the reviewed RPC contract.
     * @return The delegated result
     */
    boolean isRestricted();

    /**
     * reloadLicenses through the reviewed RPC contract.
     * @param blocking The blocking argument
     */
    void reloadLicenses(boolean blocking);
}
