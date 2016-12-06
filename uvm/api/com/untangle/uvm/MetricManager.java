/**
 * $Id$
 */
package com.untangle.uvm;

public interface MetricManager
{
    org.json.JSONObject getStats();

    org.json.JSONObject getMetricsAndStats();

    Long getMemTotal();
}
