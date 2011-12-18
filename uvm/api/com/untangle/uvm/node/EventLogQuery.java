/**
 * $Id: EventLogQuery.java,v 1.00 2011/12/17 17:16:45 dmorris Exp $
 */
package com.untangle.uvm.node;

import java.io.Serializable;

/**
 * This class represents a unique Event Log query and stores all the information the UI needs to
 * render and exec the query
 */
@SuppressWarnings("serial")
public class EventLogQuery implements Serializable
{
    private String name;
    private String query;

    public EventLogQuery( String name, String query )
    {
        this.name = name;
        this.query = query;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName( String name)
    {
        this.name = name;
    }

    public String getQuery()
    {
        return this.query;
    }

    public void setQuery( String query)
    {
        this.query = query;
    }
    
}