/**
 * $Id: MessageQueue.java,v 1.00 2012/04/06 12:44:16 dmorris Exp $
 */
package com.untangle.uvm.message;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.untangle.uvm.node.NodeMetric;

@SuppressWarnings("serial")
public class MessageQueue implements Serializable
{
    private final Map<Long, List<NodeMetric>> metrics;
    private final Map<String, Object> systemStats;

    public MessageQueue( Map<Long, List<NodeMetric>> metrics, Map<String, Object> systemStats )
    {
        this.metrics = metrics;
        this.systemStats = systemStats;
    }

    public Map<Long, List<NodeMetric>> getMetrics()
    {
        return metrics;
    }

    public Map<String, Object> getSystemStats()
    {
        return systemStats;
    }
}