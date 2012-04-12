/**
 * $Id: NodeMetric.java,v 1.00 2012/04/05 17:00:24 dmorris Exp $
 */
package com.untangle.uvm.message;

import java.io.Serializable;

@SuppressWarnings("serial")
public class NodeMetric implements Serializable
{
    private Long id;
    private String name;
    private StatInterval interval;

    public NodeMetric() { }

    public NodeMetric(String name, StatInterval interval)
    {
        this.name = name;
        this.interval = interval;
    }

    private Long getId()
    {
        return id;
    }

    private void setId(Long id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public StatInterval getInterval()
    {
        return interval;
    }

    public void setInterval(StatInterval interval)
    {
        this.interval = interval;
    }

    public String toString()
    {
        return "NodeMetric[#" + id + "] name: " + name + " interval: " + interval;
    }
}