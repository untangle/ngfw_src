/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import org.json.JSONObject;

/**
 * Reviewed callable operations for SafeConnectivityTester.
 */
public interface SafeConnectivityTester
{
    /**
     * getStatus through the reviewed RPC contract.
     * @return The delegated result
     */
    JSONObject getStatus();
}
