/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: Tid.java,v 1.10 2005/02/25 02:45:29 amread Exp $
 */

package com.metavize.mvvm.security;

import java.io.Serializable;
import java.security.Principal;

/**
 * Transform ID.
 *
 * @author <a href="mailto:amread@nyx.net">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TID"
 */
public final class Tid implements Principal, Serializable, Comparable
{
    private static final long serialVersionUID = -5009447155713499894L;

    private Long id;

    public Tid() { }

    public Tid(Long id)
    {
        this.id = id;
    }

    /**
     * The Long representation of this Tid.
     *
     * @return the Tid as a Long.
     * @hibernate.id
     * generator-class="assigned"
     * column="ID"
     */
    public Long getId()
    {
        return id;
    }

    private void setId(Long id)
    {
        this.id = id;
    }

    // XXX something more appropriate
    public String getName()
    {
        return Long.toString(id);
    }

    // Comparable methods -----------------------------------------------------

    public int compareTo(Object o)
    {
        if (!(o instanceof Tid)) { throw new IllegalArgumentException(); }

        Tid tid = (Tid)o;

        return id < tid.getId() ? -1 : (id > tid.getId() ? 1 : 0);
    }

    // Object methods ---------------------------------------------------------

    // XXX something more appropriate
    public String toString()
    {
        return getName();
    }

    public boolean equals(Object o)
    {
        if (!(o instanceof Tid)) {
            return false;
        }
        Tid t = (Tid)o;

        return id.equals(t.getId());
    }

    public int hashCode()
    {
        return id.hashCode();
    }
}
