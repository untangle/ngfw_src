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

package com.metavize.tran.spyware;

import java.io.Serializable;
import java.util.Date;

import com.metavize.mvvm.tran.Direction;
import com.metavize.mvvm.tran.IPMaddr;

public class SpywareAccessLog implements Serializable
{
    private final Date createDate;
    private final IPMaddr ipMAddr;
    private final String ident;
    private final String clientAddr;
    private final int clientPort;
    private final String serverAddr;
    private final int serverPort;
    private final Direction direction;

    public SpywareAccessLog(Date createDate, IPMaddr ipMAddr, String ident,
                            String clientAddr, int clientPort,
                            String serverAddr, int serverPort,
                            Direction direction)
    {
        this.createDate = createDate;
        this.ipMAddr = ipMAddr;
        this.ident = ident;
        this.clientAddr = clientAddr;
        this.clientPort = clientPort;
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
        this.direction = direction;
    }

    // accessors --------------------------------------------------------------

    public Date createDate()
    {
        return createDate;
    }

    public IPMaddr getIpMAddr()
    {
        return ipMAddr;
    }

    public String getIdent()
    {
        return ident;
    }

    public String getClientAddr()
    {
        return clientAddr;
    }

    public int getCClientPort()
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
