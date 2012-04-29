/**
 * $Id: NodeMetric.java,v 1.00 2012/04/05 17:00:24 dmorris Exp $
 */
package com.untangle.uvm.node;

import java.io.Serializable;

@SuppressWarnings("serial")
public class NodeMetric implements Serializable
{
    private String name;
    private String displayName;
    private Long value;
    
    public NodeMetric() { }

    public NodeMetric(String name, String displayName)
    {
        this.name = name;
        this.displayName = displayName;
        this.value = 0L;
    }

    public NodeMetric(String name, String displayName, Long value)
    {
        this.name = name;
        this.displayName = displayName;
        this.value = value;
    }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public Long getValue() { return value; }
    public void setValue( Long value ) { this.value = value; }

    public String toString()
    {
        return "NodeMetric[" + name + "] = " + value;
    }
}