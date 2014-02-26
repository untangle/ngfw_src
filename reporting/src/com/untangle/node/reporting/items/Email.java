/**
 * $Id$
 */
package com.untangle.node.reporting.items;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Email implements Serializable, Comparable<Email>
{
    private final String name;

    public Email(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public int compareTo(Email e)
    {
        return name.compareTo(e.name);
    }
}