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

public class SpywareLog implements Comparable<SpywareLog>, Serializable
{
    private static final long serialVersionUID = -394881366677267209L;

    public enum Type { COOKIE, ACTIVEX, ACCESS, BLACKLIST, NONE };

    private final Date createDate;
    private final Type type;
    private final String location;
    private final String ident;
    private final boolean blocked;
    private final String clientAddr;
    private final int clientPort;
    private final String serverAddr;
    private final int serverPort;
    private final Direction direction;

    public SpywareLog(Date createDate, String type, String location,
                      String ident, boolean blocked, String clientAddr,
                      int clientPort, String serverAddr, int serverPort,
                      Direction direction)
    {
        this.createDate = createDate;
        this.type = Type.valueOf(type);
        this.location = location;
        this.ident = ident;
        this.blocked = blocked;
        this.clientAddr = clientAddr;
        this.clientPort = clientPort;
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
        this.direction = direction;
    }

    // util -------------------------------------------------------------------

    public String getAction()
    {
        if (blocked) {
            return "block";
        } else {
            return "pass";
        }
    }

    public String getReason()
    {
        if (type == Type.COOKIE) {
            return "in Cookie List";
        } else if (type == Type.ACTIVEX) {
            return "in ActiveX List";
        } else if (type == Type.ACCESS) {
            return "in Subnet List";
        } else if (type == Type.BLACKLIST) {
            return "in URL List";
        } else {
            return null;
        }
    }

    // accessors --------------------------------------------------------------

    public Date getCreateDate()
    {
        return createDate;
    }

    public Type getType()
    {
        return type;
    }

    public String getLocation()
    {
        return location;
    }

    public String getIdent()
    {
        return ident;
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

    // Comparable methods -----------------------------------------------------

    public int compareTo(SpywareLog sl)
    {
        // Sort descending instead of ascending.
        return -createDate.compareTo(sl.createDate);
    }
}
