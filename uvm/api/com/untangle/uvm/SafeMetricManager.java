/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import org.json.JSONObject;

/**
 * Reviewed callable operations for SafeMetricManager.
 */
public interface SafeMetricManager
{
    /**
     * getMemTotal through the reviewed RPC contract.
     * @return The delegated result
     */
    Long getMemTotal();

    /**
     * getMetricsAndStats through the reviewed RPC contract.
     * @return The delegated result
     */
    JSONObject getMetricsAndStats();
}
