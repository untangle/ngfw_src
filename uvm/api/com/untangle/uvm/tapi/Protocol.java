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

package com.untangle.mvvm.tapi;

import java.util.HashMap;
import java.util.Map;

public class Protocol
{
    private static final Map<String,Protocol> NAME_TO_PROTOCOL_MAP = new HashMap<String,Protocol>();
    private static final Map<Integer,Protocol> ID_TO_PROTOCOL_MAP = new HashMap<Integer,Protocol>();

    public static final Protocol TCP  = makeInstance("TCP", 6);
    public static final Protocol UDP  = makeInstance("UDP", 17);
    public static final Protocol ICMP = makeInstance("ICMP", 1);

    private final String name;
    private final int id;

    public static Protocol getInstance(String name)
    {
        return NAME_TO_PROTOCOL_MAP.get(name);
    }

    public static Protocol getInstance(int id)
    {
        return ID_TO_PROTOCOL_MAP.get(id);
    }

    private Protocol(String name, int id)
    {
        this.name = name;
        this.id   = id;
    }

    public String toString()
    {
        return name;
    }

    public int getId()
    {
        return id;
    }

    // Object methods ---------------------------------------------------------

    public boolean equals(Object o)
    {
        if (!(o instanceof Protocol)) {
            return false;
        } else {
            Protocol p = (Protocol)o;
            return id == p.id;
        }
    }

    public int hashCode()
    {
        return ( 17 * 37 ) + id;
    }

    // serialization helpers --------------------------------------------------

    Object readResolve()
    {
        return getInstance(name);
    }

    private static Protocol makeInstance( String name, int id )
    {
        Protocol protocol = new Protocol( name, id );
        NAME_TO_PROTOCOL_MAP.put( name, protocol );
        ID_TO_PROTOCOL_MAP.put( id, protocol );
        return protocol;
    }
                                          
}
