/**
 * $Id: Application.java,v 1.00 2012/06/11 14:58:14 dmorris Exp $
 */
package com.untangle.node.reporting.items;

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