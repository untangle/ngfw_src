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

package com.metavize.mvvm.tran;

import java.io.Serializable;

public class Direction implements Serializable
{
    private static final long serialVersionUID = 9115074195671091726L;

    private static final Direction OUTGOING
        = new Direction("outgoing", Interface.INSIDE, Interface.OUTSIDE);
    private static final Direction INCOMING
        = new Direction("incoming", Interface.OUTSIDE, Interface.INSIDE);

    private final String directionName;
    private final Interface clientIface;
    private final Interface serverIface;

    // constructors -----------------------------------------------------------

    private Direction(String directionName, Interface clientIface,
                      Interface serverIface)
    {
        this.directionName = directionName;
        this.clientIface = clientIface;
        this.serverIface = serverIface;
    }

    // factories --------------------------------------------------------------

    public static final Direction getDirection(Interface clientIface,
                                               Interface serverIface) {
        if (Interface.INSIDE == clientIface
            && Interface.OUTSIDE == serverIface) {
            return OUTGOING;
        } else if (Interface.OUTSIDE == clientIface
                   && Interface.INSIDE == serverIface) {
            return INCOMING;
        } else {
            return new Direction("other", clientIface, serverIface);
        }
    }

    public static final Direction getDirection(byte clientIface,
                                               byte serverIface) {
        return getDirection(Interface.getInstance(clientIface),
                            Interface.getInstance(serverIface));
    }

    // public methods ---------------------------------------------------------

    public Interface getClientIface()
    {
        return clientIface;
    }

    public Interface getServerIface()
    {
        return serverIface;
    }

    public String getDirectionName()
    {
        return directionName;
    }

    // Object methods ---------------------------------------------------------

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof Direction)) {
            return false;
        }

        Direction d = (Direction)o;

        return clientIface == d.clientIface && serverIface == d.serverIface;
    }

    @Override
    public int hashCode()
    {
        int result = 17;
        result = 37 * result + clientIface.hashCode();
        result = 37 * result + serverIface.hashCode();
        return result;
    }
}
