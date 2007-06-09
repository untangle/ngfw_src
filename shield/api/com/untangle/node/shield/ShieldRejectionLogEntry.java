/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.untangle.node.shield;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

public class ShieldRejectionLogEntry implements Serializable
{
    private final Date   createDate;
    private final String client;
    private final String clientIntf;
    private final double reputation;
    private final int    limited;
    private final int    dropped;
    private final int    rejected;

    ShieldRejectionLogEntry( Date createDate, String client, String clientIntf,
                             double reputation, int limited, int dropped, int rejected )

    {
        if( createDate instanceof Timestamp )
            this.createDate = new Date(createDate.getTime());
        else
            this.createDate = createDate;
        this.client     = client;
        this.clientIntf = clientIntf;
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

    public String getClientIntf()
    {
        return this.clientIntf;
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
