/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: SScanner.java,v 1.4 2005/03/16 04:00:03 cng Exp $
 */

package com.metavize.tran.email;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// XXX convert to enum when we dump XDoclet

public class SScanner implements Serializable
{
    private static final long serialVersionUID = -4301105919913202908L;

    private static final Map INSTANCES = new HashMap();

    public static final SScanner SPAMAS = new SScanner('S', "SpamAssassin");
    public static final SScanner NOAS = new SScanner('N', "no anti-spam");

    static {
        INSTANCES.put(SPAMAS.getKey(), SPAMAS);
        INSTANCES.put(NOAS.getKey(), NOAS);
    }

    private char cKey;
    private String zName;

    private SScanner(char cKey, String zName)
    {
        this.cKey = cKey;
        this.zName = zName;
    }

    public static SScanner getInstance(char cKey)
    {
        return (SScanner)INSTANCES.get(cKey);
    }

    public static SScanner getInstance(String zName)
    {
        SScanner zSScanner;
        for (Iterator i = INSTANCES.keySet().iterator(); true == i.hasNext(); )
        {
            zSScanner = (SScanner)INSTANCES.get(i.next());
            if (zName.equals(zSScanner.getName()))
            {
                return zSScanner;
            }
        }

        return null;
    }

    public String toString()
    {
        return zName;
    }

    public char getKey()
    {
        return cKey;
    }

    public String getName()
    {
        return zName;
    }

    Object readResolve()
    {
        return getInstance(cKey);
    }

    public static SScanner[] values()
    {
        SScanner[] result = new SScanner[INSTANCES.size()];
        Iterator iter = INSTANCES.keySet().iterator();
        SScanner zSScanner;
        for (int i = 0; true == iter.hasNext(); i++)
        {
            zSScanner = (SScanner)INSTANCES.get(iter.next());
            result[i] = zSScanner;
        }

        return result;
    }
}
