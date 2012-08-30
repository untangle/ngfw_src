/**
 * $Id: Host.java,v 1.00 2012/06/11 14:58:04 dmorris Exp $
 */
package com.untangle.node.reporting.items;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Host implements Serializable, Comparable<Host>
{
    private final String name;

    public Host(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public int compareTo(Host h)
    {
        return name.compareTo(h.name);
    }
}