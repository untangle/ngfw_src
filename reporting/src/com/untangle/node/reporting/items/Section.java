/**
 * $Id$
 */
package com.untangle.node.reporting.items;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Section implements Serializable
{
    private final String name;
    private final String title;

    public Section(String name, String title)
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