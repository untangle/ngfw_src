/**
 * $Id: Counters.java,v 1.00 2012/04/05 16:31:47 dmorris Exp $
 */
package com.untangle.uvm.message;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.NodeSettings;

/**
 * Class for registering LoadStats and CounterStats objects and
 * retrieving their values.
 */
public class Counters
{
    private final Long nodeId;

    private final Map<String, BlingBlinger> metrics = new LinkedHashMap<String, BlingBlinger>();

    private final Map<String, BlingBlinger> activities = new LinkedHashMap<String, BlingBlinger>();

    private final Map<String, LoadMaster> loads = new LinkedHashMap<String, LoadMaster>();

    public Counters(Long nodeId)
    {
        this.nodeId = nodeId;
    }

    public BlingBlinger getBlingBlinger(String name)
    {
        BlingBlinger b;

        synchronized (metrics) {
            b = metrics.get(name);
        }

        return b;
    }

    public BlingBlinger addMetric(String name, String displayName, String unit, boolean displayable)
    {
        BlingBlinger b = new BlingBlinger(name, displayName, unit, null,
                                          displayable);

        synchronized (metrics) {
            metrics.put(name, b);
        }

        return b;
    }

    public BlingBlinger addMetric(String name, String displayName, String unit)
    {
        return addMetric(name, displayName, unit, true);
    }

    public BlingBlinger addActivity(String name, String displayName, String unit, String action, boolean displayable)
    {
        BlingBlinger b = new BlingBlinger(name, displayName, unit, action,
                                          displayable);

        synchronized (metrics) {
            metrics.put(name, b);
        }

        synchronized (activities) {
            activities.put(name, b);
        }

        return b;
    }

    public BlingBlinger addActivity(String name, String displayName, String unit, String action)
    {
        return addActivity(name, displayName, unit, action, true);
    }

    public BlingBlinger delActivity(String name)
    {
        BlingBlinger b = null;
        synchronized (metrics) {
            b = metrics.remove(name);
        }
        synchronized (activities) {
            b  = (b == null) ? activities.remove(name) : b;
        }

        return b;
    }

    public void addLoadMaster(String name, LoadMaster loadMaster)
    {
        synchronized (loads) {
            loads.put(name, loadMaster);
        }
    }

    public LoadCounter makeLoadCounter(String name, String displayName)
    {
        LoadCounter lc = new LoadCounter();
        LoadMaster lm = new LoadMaster(lc, name, displayName);

        synchronized (loads) {
            loads.put(name, lm);
        }

        return lc;
    }

    public StatDescs getStatDescs()
    {
        UvmContext mctx = UvmContextFactory.context();
        List<NodeMetric> activeStats = mctx.messageManager().getActiveMetrics( nodeId );
        return new StatDescs(metrics.values(), activities.values(), activeStats);
    }

    public Stats getAllStats()
    {
        return new Stats(metrics, activities);
    }

    public Stats getAllStats(List<NodeMetric> l)
    {
        Map<String, BlingBlinger> m = new HashMap<String, BlingBlinger>();
        if(l!=null) {
            for (NodeMetric as : l) {
                String n = as.getName();
                m.put(n, metrics.get(n));
            }
        }
        return new Stats(m, activities);
    }
}