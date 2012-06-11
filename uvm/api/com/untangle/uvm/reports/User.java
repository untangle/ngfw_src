/**
 * $Id: User.java,v 1.00 2012/06/11 14:57:46 dmorris Exp $
 */
package com.untangle.uvm.reports;

import java.io.Serializable;

@SuppressWarnings("serial")
public class User implements Serializable, Comparable<User>
{
    private final String name;

    public User(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public String toString()
    {
        return name;
    }

    public int compareTo(User u)
    {
        return name.compareTo(u.name);
    }
}