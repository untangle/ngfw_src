/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

// XXX convert to enum when we dump XDoclet

@SuppressWarnings("serial")
public class Mode implements Serializable
{

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
