/**
 * $Id$
 */
package com.untangle.node.reports.items;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Application implements Serializable
{
    
    private final String name;
    private final String title;

    public Application(String name, String title)
    {
        this.name = name;
        this.title = title;
    }

    public String getName()
    {
        return name;
    }

    public String getTitle()
    {
        return title;
    }
}