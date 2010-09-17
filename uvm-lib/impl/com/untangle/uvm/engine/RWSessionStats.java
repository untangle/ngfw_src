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

package com.untangle.uvm.engine;

import java.util.Date;

import com.untangle.uvm.util.MetaEnv;
import com.untangle.uvm.vnet.SessionStats;

/**
 * <code>RWSessionStats</code> is the writable subclass of SessionStats used
 * internally by the uvm.
 * It is contained within a Session (never in a SessionDesc).
 *
 * @author <a href="mailto:jdi@untangle.com"></a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class RWSessionStats extends SessionStats
{
    public static boolean DoDetailedTimes = false;

    public RWSessionStats()
    {
        super();
        long now = MetaEnv.currentTimeMillis();
        creationDate = new Date(now);
        lastActivityDate = new Date(now);
        if (DoDetailedTimes)
            times = new long[MAX_TIME_INDEX];
    }

    protected void readData(int side, long bytes)
    {
        if (side == SessionImpl.CLIENT) {
            c2tChunks++;
            c2tBytes += bytes;
        } else {
            s2tChunks++;
            s2tBytes += bytes;
        }
        lastActivityDate.setTime(MetaEnv.currentTimeMillis());
    }

    protected void wroteData(int side, long bytes)
    {
        if (side == SessionImpl.SERVER) {
            t2sChunks++;
            t2sBytes += bytes;
        } else {
            t2cChunks++;
            t2cBytes += bytes;
        }
        lastActivityDate.setTime(MetaEnv.currentTimeMillis());
    }

    protected void stateChanged()
    {
        lastActivityDate.setTime(MetaEnv.currentTimeMillis());
    }

}
