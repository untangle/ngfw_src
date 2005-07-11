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
import java.util.HashMap;
import java.util.Map;

import com.metavize.mvvm.argon.IntfConverter;

public class Interface implements Serializable
{
    private static final long serialVersionUID = -2938902418634358424L;

    public static Interface ANY = new Interface(null);
    public static Interface INSIDE = new Interface(IntfConverter.INSIDE);
    public static Interface OUTSIDE = new Interface(IntfConverter.OUTSIDE);

    private static final Map<Byte, Interface> INSTANCES
        = new HashMap<Byte, Interface>();

    static {
        INSTANCES.put(ANY.getIface(), ANY);
        INSTANCES.put(INSIDE.getIface(), INSIDE);
        INSTANCES.put(OUTSIDE.getIface(), OUTSIDE);
    }

    private final Byte iface;

    // constructors -----------------------------------------------------------

    private Interface(Byte iface)
    {
        this.iface = iface;
    }

    // static factories -------------------------------------------------------

    public static Interface getInstance(Byte iface)
    {
        return INSTANCES.get(iface);
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

    // Object methods ---------------------------------------------------------

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof Interface)) {
            return false;
        }

        Interface i = (Interface)o;

        return iface == i.iface;
    }

    @Override
    public int hashCode()
    {
        return 17 * 37 + iface;
    }

    // serialization support --------------------------------------------------

    Object readResolve()
    {
        return getInstance(iface);
    }
}
