/**
 * $Id: Email.java,v 1.00 2012/06/11 14:57:07 dmorris Exp $
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