/**
 * $Id$
 */
package com.untangle.uvm;

public interface MetricManager
{
    org.json.JSONObject getMetricsAndStats();

    Long getMemTotal();
}
