/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.firewall;

import java.io.Serializable;
import java.util.Date;

import com.metavize.mvvm.tran.Direction;

public class FirewallLog implements Serializable
{
    // XXX serial uid

    private final Date createDate;
    private final String reason = "in list";
    private final boolean blocked;
    private final String clientAddr;
    private final int clientPort;
    private final String serverAddr;
    private final int serverPort;
    private final Direction direction;

    public FirewallLog(Date createDate, boolean blocked, String clientAddr,
                      int clientPort, String serverAddr, int serverPort,
                      Direction direction)
    {
        this.createDate = createDate;
        this.blocked = blocked;
        this.clientAddr = clientAddr;
        this.clientPort = clientPort;
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
        this.direction = direction;
    }

    // accessors --------------------------------------------------------------

    public Date getCreateDate()
    {
        return createDate;
    }

    public boolean isBlocked()
    {
        return blocked;
    }

    public String getClientAddr()
    {
        return clientAddr;
    }

    public int getClientPort()
    {
        return clientPort;
    }

    public String getServerAddr()
    {
        return serverAddr;
    }

    public int getSServerPort()
    {
        return serverPort;
    }

    public Direction getDirection()
    {
        return direction;
    }
}
