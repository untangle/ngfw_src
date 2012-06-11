/**
 * $Id: Section.java,v 1.00 2012/06/11 14:57:22 dmorris Exp $
 */
package com.untangle.uvm.reports;

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