/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.tapi;

import java.util.HashMap;
import java.util.Map;

public class Protocol
{
    public static final Protocol TCP = new Protocol("TCP", 6);
    public static final Protocol UDP = new Protocol("UDP", 17);

    private static final Map INSTANCES = new HashMap();

    static {
        INSTANCES.put(TCP.toString(), TCP);
        INSTANCES.put(UDP.toString(), UDP);
        INSTANCES.put(TCP.getId(), TCP);
        INSTANCES.put(UDP.getId(), UDP);
    }

    private final String name;
    private final int id;

    public static Protocol getInstance(String name)
    {
        return (Protocol)INSTANCES.get(name);
    }

    public static Protocol getInstance(int id)
    {
        return (Protocol)INSTANCES.get(id);
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
        return id;
    }

    // serialization helpers --------------------------------------------------

    Object readResolve()
    {
        return getInstance(name);
    }
}
