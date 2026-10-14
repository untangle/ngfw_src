/**
 * $Id$
 */
package com.untangle.uvm;

public interface MetricManager
{
    org.json.JSONObject getStats();

    org.json.JSONObject getMetricsAndStats();

    org.json.JSONObject getMetricsAndStatsV2();

    Long getMemTotal();
}
