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

public class SpywareActiveXLog implements Serializable
{
    // XXX serial uid

    private final Date timeStamp;
    private final String url;
    private final String ident;
    private final String clientAddr;
    private final int clientPort;
    private final String serverAddr;
    private final int serverPort;
    private final Direction direction;

    public SpywareActiveXLog(Date timeStamp, String host, String uri,
                             String ident, String clientAddr,
                             int clientPort, String serverAddr,
                             int serverPort, Direction direction)
    {
        this.timeStamp = timeStamp;
        this.url = "http://" + host + uri;
        this.ident = ident;
        this.clientAddr = clientAddr;
        this.clientPort = clientPort;
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
        this.direction = direction;
    }

    // accessors --------------------------------------------------------------

    public Date timeStamp()
    {
        return timeStamp;
    }

    public String getUrl()
    {
        return url;
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
