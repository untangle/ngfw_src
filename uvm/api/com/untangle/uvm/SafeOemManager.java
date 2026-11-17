/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

/**
 * Reviewed callable operations for SafeOemManager.
 */
public interface SafeOemManager
{
    /**
     * getOemUrl through the reviewed RPC contract.
     * @return The delegated result
     */
    String getOemUrl();

    /**
     * getLicenseAgreementUrl through the reviewed RPC contract.
     * @return The delegated result
     */
    String getLicenseAgreementUrl();
}
