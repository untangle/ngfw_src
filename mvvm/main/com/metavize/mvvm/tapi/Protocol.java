/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: Protocol.java,v 1.2 2005/02/25 02:45:29 amread Exp $
 */

package com.metavize.mvvm.tapi;

import java.util.HashMap;
import java.util.Map;

public class Protocol
{
    public static final Protocol TCP = new Protocol("TCP");
    public static final Protocol UDP = new Protocol("UDP");

    private static final Map INSTANCES = new HashMap();

    static {
        INSTANCES.put(TCP.toString(), TCP);
        INSTANCES.put(UDP.toString(), UDP);
    }

    private String name;

    public static Protocol getInstance(String name)
    {
        return (Protocol)INSTANCES.get(name);
    }

    private Protocol(String name)
    {
        this.name = name;
    }

    public String toString()
    {
        return name;
    }

    Object readResolve()
    {
        return getInstance(name);
    }
}
