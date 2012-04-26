/**
 * $Id: Stats.java,v 1.00 2012/04/26 15:17:40 dmorris Exp $
 */
package com.untangle.uvm.message;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class Stats implements Serializable
{
    private final Map<String, CounterStats> metrics;
    private final Map<String, CounterStats> activities;
    private final Map<String, LoadMaster> loads;

    public Stats(Map<String, BlingBlinger> metrics, Map<String, BlingBlinger> activities, Map<String, LoadMaster> loads)
    {
        this.metrics = convert(metrics);
        this.activities = convert(activities);
        this.loads = loads;
    }

    public Map<String, CounterStats> getMetrics()
    {
        return metrics;
    }

    public Map<String, CounterStats> getActivities()
    {
        return activities;
    }

    public Map<String, LoadMaster> getLoads()
    {
        return loads;
    }
    
    private Map<String, CounterStats> convert(Map<String, BlingBlinger> stats)
    {
        Map<String, CounterStats> m = new HashMap<String, CounterStats>(stats.size());
        for (String s : stats.keySet()) {
            CounterStats cs = stats.get(s);
            if ( cs == null ) {
                continue;
            }
            m.put(s, cs);
        }

        return m;
    }
}
