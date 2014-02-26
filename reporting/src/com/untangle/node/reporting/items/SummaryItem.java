/**
 * $Id$
 */
package com.untangle.node.reporting.items;

import java.io.Serializable;

@SuppressWarnings("serial")
public class SummaryItem implements Serializable
{
    private final String name;
    private final String title;

    public SummaryItem(String name, String title)
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