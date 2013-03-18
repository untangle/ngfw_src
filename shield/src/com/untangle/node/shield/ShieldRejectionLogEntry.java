/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.untangle.node.shield;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

@SuppressWarnings("serial")
public class ShieldRejectionLogEntry implements Serializable
{
    private final Date   createDate;
    private final String client;
    private final double reputation;
    private final int    limited;
    private final int    dropped;
    private final int    rejected;

    ShieldRejectionLogEntry( Date createDate, String client, double reputation, int limited, int dropped, int rejected )
    {
        if( createDate instanceof Timestamp )
            this.createDate = new Date(createDate.getTime());
        else
            this.createDate = createDate;
        this.client     = client;
        this.reputation = reputation;
        this.limited    = limited;
        this.dropped    = dropped;
        this.rejected   = rejected;
    }

    // accessors --------------------------------------------------------------
    public Date getCreateDate()
    {
        return this.createDate;
    }

    public String getClient()
    {
        return this.client;
    }

    public double getReputation()
    {
        return this.reputation;
    }

    public int getLimited()
    {
        return this.limited;
    }

    public int getDropped()
    {
        return this.dropped;
    }

    public int getRejected()
    {
        return this.rejected;
    }
}
