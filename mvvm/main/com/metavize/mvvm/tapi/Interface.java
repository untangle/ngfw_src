/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: Interface.java,v 1.2 2005/02/25 02:45:29 amread Exp $
 */

package com.metavize.mvvm.tapi;

import java.util.HashMap;
import java.util.Map;

import com.metavize.mvvm.argon.IntfConverter;

public class Interface
{
    public static Interface ANY = new Interface(null);
    public static Interface INSIDE = new Interface(IntfConverter.INSIDE);
    public static Interface OUTSIDE = new Interface(IntfConverter.OUTSIDE);

    private static final Map INSTANCES = new HashMap();

    static {
        INSTANCES.put(INSIDE.getIface(), INSIDE);
        INSTANCES.put(OUTSIDE.getIface(), OUTSIDE);
    }

    private Byte iface;

    // constructors -----------------------------------------------------------

    public Interface(Byte iface)
    {
        this.iface = iface;
    }

    // static factories -------------------------------------------------------

    public static Interface getInstance(Byte iface)
    {
        return (Interface)INSTANCES.get(iface);
    }

    // business methods -------------------------------------------------------

    public boolean matches(final byte iface)
    {
        return null == this.iface || iface == this.iface;
    }

    // accessors --------------------------------------------------------------

    public Byte getIface()
    {
        return iface;
    }

    // serialization support --------------------------------------------------

    Object readResolve()
    {
        return getInstance(iface);
    }
}
