/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import org.json.JSONObject;

/**
 * Reviewed callable operations for SafeGeographyManager.
 */
public interface SafeGeographyManager
{
    /**
     * getGeoSessionStats through the reviewed RPC contract.
     * @return The delegated result
     */
    JSONObject[] getGeoSessionStats();
}
