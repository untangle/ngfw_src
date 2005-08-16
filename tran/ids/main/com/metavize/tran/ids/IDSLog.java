/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: ProtoFilterLog.java 1355 2005-07-13 22:51:22Z inieves $
 */

package com.metavize.tran.ids;

import java.io.Serializable;
import java.util.Date;

import com.metavize.mvvm.tran.Direction;

public class IDSLog implements Serializable {
    
	private final Date createDate;
    private final String message;
    private final boolean blocked;
    private final String clientAddr;
    private final int clientPort;
    private final String serverAddr;
    private final int serverPort;
    private final Direction direction;

    public IDSLog(Date createDate, String message, boolean blocked,
                          String clientAddr, int clientPort,
                          String serverAddr, int serverPort,
                          Direction direction)
    {
        this.createDate = createDate;
        this.message = message;
        this.blocked = blocked;
        this.clientAddr = clientAddr;
        this.clientPort = clientPort;
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
        this.direction = direction;
    }

    // util -------------------------------------------------------------------
    public String getAction(){
	if(blocked)
	    return "block";
	else
	    return "pass";
    }
    
    public String getReason(){
	if(blocked)
	    return "blocked in block list";
	else
	    return "not blocked in block list";
    }
    // accessors --------------------------------------------------------------

    public Date getCreateDate()
    {
        return createDate;
    }

    public String getMessage()
    {
        return message;
    }

    public boolean getBlocked()
    {
        return blocked;
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
