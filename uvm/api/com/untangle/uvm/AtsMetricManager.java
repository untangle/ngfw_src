package com.untangle.uvm;

import org.json.JSONObject;

/**
 * Combined Safe and additional ATS operations.
 */
public interface AtsMetricManager extends SafeMetricManager
{
    /**
     * getStats through the additional ATS contract.
     * @return The delegated result
     */
    JSONObject getStats();
}
