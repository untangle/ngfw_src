/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: VScanner.java,v 1.5 2005/03/16 04:00:04 cng Exp $
 */

package com.metavize.tran.email;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// XXX convert to enum when we dump XDoclet

public class VScanner implements Serializable
{
    private static final long serialVersionUID = 3717843529434500320L;

    private static final Map INSTANCES = new HashMap();

    public static final VScanner FPROTAV = new VScanner('F', "F-Prot Antivirus");
    public static final VScanner SOPHOSAV = new VScanner('S', "Sophos Anti-Virus");
    public static final VScanner HAURIAV = new VScanner('H', "Hauri ViRobot");
    public static final VScanner CLAMAV = new VScanner('C', "ClamAV");
    public static final VScanner NOAV = new VScanner('N', "no anti-virus");

    static {
        INSTANCES.put(FPROTAV.getKey(), FPROTAV);
        INSTANCES.put(SOPHOSAV.getKey(), SOPHOSAV);
        INSTANCES.put(HAURIAV.getKey(), HAURIAV);
        INSTANCES.put(CLAMAV.getKey(), CLAMAV);
        INSTANCES.put(NOAV.getKey(), NOAV);
    }

    private char cKey;
    private String zName;

    private VScanner(char cKey, String zName)
    {
        this.cKey = cKey;
        this.zName = zName;
    }

    public static VScanner getInstance(char cKey)
    {
        return (VScanner)INSTANCES.get(cKey);
    }

    public static VScanner getInstance(String zName)
    {
        VScanner zVScanner;
        for (Iterator i = INSTANCES.keySet().iterator(); true == i.hasNext(); )
        {
            zVScanner = (VScanner)INSTANCES.get(i.next());
            if (zName.equals(zVScanner.getName()))
            {
                return zVScanner;
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

    public static VScanner[] values()
    {
        VScanner[] result = new VScanner[INSTANCES.size()];
        Iterator iter = INSTANCES.keySet().iterator();
        VScanner zVScanner;
        for (int i = 0; true == iter.hasNext(); i++)
        {
            zVScanner = (VScanner)INSTANCES.get(iter.next());
            result[i] = zVScanner;
        }

        return result;
    }
}
