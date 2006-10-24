/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

// XXX convert to enum when we dump XDoclet

public class Mode implements Serializable
{
    private static final long serialVersionUID = 8234167214876633324L;

    public static final Mode RELEASE = new Mode("RELEASE");
    public static final Mode NORMAL = new Mode("NORMAL");
    public static final Mode BUFFERED = new Mode("BUFFERED");
    // public static final Mode DOUBLE_ENDPOINT = new Mode("DOUBLE_ENDPOINT");
    // public static final Mode READ_ONLY = new Mode("READ_ONLY");

    private static final Map INSTANCES = new HashMap();

    static {
        INSTANCES.put(RELEASE.toString(), RELEASE);
        INSTANCES.put(NORMAL.toString(), NORMAL);
        INSTANCES.put(BUFFERED.toString(), BUFFERED);
        // INSTANCES.put(DOUBLE_ENDPOINT.toString(), DOUBLE_ENDPOINT);
        // INSTANCES.put(READ_ONLY.toString(), READ_ONLY);
    }

    private final String mode;

    public static Mode getInstance(String mode)
    {
        return (Mode)INSTANCES.get(mode);
    }

    private Mode(String mode)
    {
        this.mode = mode;
    }

    public String toString()
    {
        return mode;
    }

    Object readResolve()
    {
        return getInstance(mode);
    }
}
