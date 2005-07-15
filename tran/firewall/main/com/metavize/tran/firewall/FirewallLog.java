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
    private static final long serialVersionUID = 8643460289284345065L;

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

    // util -------------------------------------------------------------------
    public String getReason(){
	return reason;
    }
    
    public String getAction(){
	if(blocked)
	    return "block";
	else
	    return "pass";
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

    public int getServerPort()
    {
        return serverPort;
    }

    public Direction getDirection()
    {
        return direction;
    }
}
