/**
 * $Id: ColumnDesc.java,v 1.00 2012/06/11 14:58:09 dmorris Exp $
 */
package com.untangle.uvm.reports;

import java.io.Serializable;

@SuppressWarnings("serial")
public class ColumnDesc implements Serializable
{
    private final String name;
    private final String title;
    private final String type;

    public ColumnDesc(String name, String title, String type)
    {
        this.name = name;
        this.title = title;
        this.type = type;
    }

    public String getName()
    {
        return name;
    }

    public String getTitle()
    {
        return title;
    }

    public String getType()
    {
        return type;
    }
}