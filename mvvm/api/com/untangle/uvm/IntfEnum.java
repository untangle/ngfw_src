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

package com.untangle.mvvm;

import java.io.Serializable;
import java.util.*;

/**
 * This singleton enumerates the interfaces present in the MVVM.  It
 * is created via a factory method in <code>NetworkingManager</code>.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
public class IntfEnum implements Serializable
{
    private static final long serialVersionUID = -2938902418634358424L;

    public static final byte INTF_NONE_NUM = -1;

    private final TreeMap<Byte, String> intfs;

    // constructors -----------------------------------------------------------

    // Only used internally by NetworkingManagerImpl
    public IntfEnum(byte[] intfNums, String[] intfNames)
    {
        intfs = new TreeMap<Byte, String>();
        for (int i = 0; i < intfNums.length; i++)
            intfs.put(intfNums[i], intfNames[i]);
    }

    // Accessors

    public String getIntfName(byte intfNum)
    {
        return intfs.get(intfNum);
    }

    public byte getIntfNum(String intfName)
    {
        for (Byte num : intfs.keySet()) {
            String name = intfs.get(num);
            if (intfName.equalsIgnoreCase(name))
                return num;
        }
        return INTF_NONE_NUM;
    }

    // Enumerations -----------------------------------------------------------

    public byte[] getIntfNums()
    {
        byte[] result = new byte[intfs.size()];
        int i = 0;
        for (Byte num : intfs.keySet())
            result[i++] = num;
        return result;
    }

    public String[] getIntfNames()
    {
        String[] result = new String[intfs.size()];
        int i = 0;
        for (Byte num : intfs.keySet()) {
            String name = intfs.get(num);
            result[i++] = name;
        }
        return result;
    }

    // Object methods ---------------------------------------------------------

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof IntfEnum)) {
            return false;
        }

        IntfEnum i = (IntfEnum)o;

        return intfs.equals(i.intfs);
    }

    @Override
    public int hashCode()
    {
        return intfs.hashCode();
    }
}
