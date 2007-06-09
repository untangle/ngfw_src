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

package com.untangle.uvm.engine;

import java.util.Date;

import com.untangle.uvm.tapi.*;
import com.untangle.uvm.util.MetaEnv;

/**
 * <code>RWSessionStats</code> is the writable subclass of SessionStats used
 * internally by the uvm.
 * It is contained within a Session (never in a SessionDesc).
 *
 * @author <a href="mailto:jdi@untangle.com"></a>
 * @version 1.0
 */
class RWSessionStats extends SessionStats {

    // Make this a config param. XXX
    public static boolean DoDetailedTimes = false;

    public RWSessionStats() {
        super();
        long now = MetaEnv.currentTimeMillis();
        creationDate = new Date(now);
        lastActivityDate = new Date(now);
        if (DoDetailedTimes)
            times = new long[MAX_TIME_INDEX];
    }

    protected void readData(int side, long bytes) {
        if (side == SessionImpl.CLIENT) {
            c2tChunks++;
            c2tBytes += bytes;
        } else {
            s2tChunks++;
            s2tBytes += bytes;
        }
        lastActivityDate.setTime(MetaEnv.currentTimeMillis());
    }

    protected void wroteData(int side, long bytes) {
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
