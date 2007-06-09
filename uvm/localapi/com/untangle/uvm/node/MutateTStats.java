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

package com.untangle.mvvm.tran;

import com.untangle.mvvm.tapi.MPipe;
import com.untangle.mvvm.tapi.Session;
import com.untangle.mvvm.util.MetaEnv;

/**
 * <code>MutateTStats</code> is a helper class that allows the Smith to modify TransformStats
 * (which are normally read-only).
 *
 * @author <a href="mailto:jdi@slab.ninthwave.com">John Irwin</a>
 * @version 1.0
 */
public final class MutateTStats {

    // Duplicates IPSessionState. XX
    public static final int CLIENT_TO_SERVER = 1;
    public static final int SERVER_TO_CLIENT = 2;

    private static TransformStats safeStats(Session sess) {
        try {
            // Transform may not be running any more
            return sess.mPipe().transform().getStats();
        } catch (IllegalStateException x) {
            return null;
        }
    }

    private static TransformStats safeStats(MPipe pipe) {
        try {
            // Transform may not be running any more
            return pipe.transform().getStats();
        } catch (IllegalStateException x) {
            return null;
        }
    }

    public static void readData(int direction, Session sess, long bytes) {
        TransformStats stats = safeStats(sess);
        if (stats == null)
            return;
        synchronized(stats) {
            if (direction == CLIENT_TO_SERVER) {
                stats.c2tChunks++;
                stats.c2tBytes += bytes;
            } else {
                stats.s2tChunks++;
                stats.s2tBytes += bytes;
            }
            stats.lastActivityDate.setTime(MetaEnv.currentTimeMillis());
        }
    }

    public static void rereadData(int direction, Session sess, long bytes) {
        TransformStats stats = safeStats(sess);
        if (stats == null)
            return;
        synchronized(stats) {
            if (direction == CLIENT_TO_SERVER) {
                stats.c2tBytes += bytes;
            } else {
                stats.s2tBytes += bytes;
            }
            stats.lastActivityDate.setTime(MetaEnv.currentTimeMillis());
        }
    }

    public static void wroteData(int direction, Session sess, long bytes) {
        TransformStats stats = safeStats(sess);
        if (stats == null)
            return;
        synchronized(stats) {
            if (direction == CLIENT_TO_SERVER) {
                stats.t2sChunks++;
                stats.t2sBytes += bytes;
            } else {
                stats.t2cChunks++;
                stats.t2cBytes += bytes;
            }
            stats.lastActivityDate.setTime(MetaEnv.currentTimeMillis());
        }
    }

    public static void rewroteData(int direction, Session sess, long bytes)
    {
        TransformStats stats = safeStats(sess);
        if (stats == null)
            return;
        synchronized(stats) {
            if (direction == CLIENT_TO_SERVER) {
                stats.t2sBytes += bytes;
            } else {
                stats.t2cBytes += bytes;
            }
            stats.lastActivityDate.setTime(MetaEnv.currentTimeMillis());
        }
    }

    public static void addTCPSession(MPipe mPipe) {
        TransformStats stats = safeStats(mPipe);
        if (stats == null)
            return;
        synchronized(stats) {
            stats.tcpSessionCount++;
            stats.tcpSessionTotal++;
            stats.lastActivityDate.setTime(MetaEnv.currentTimeMillis());
        }
    }

    public static void removeTCPSession(MPipe mPipe) {
        TransformStats stats = safeStats(mPipe);
        if (stats == null)
            return;
        synchronized(stats) {
            stats.tcpSessionCount--;
            // Last activity date already set by state change
        }
    }

    public static void addUDPSession(MPipe mPipe) {
        TransformStats stats = safeStats(mPipe);
        if (stats == null)
            return;
        synchronized(stats) {
            stats.udpSessionCount++;
            stats.udpSessionTotal++;
            stats.lastActivityDate.setTime(MetaEnv.currentTimeMillis());
        }
    }

    public static void removeUDPSession(MPipe mPipe) {
        TransformStats stats = safeStats(mPipe);
        if (stats == null)
            return;
        synchronized(stats) {
            stats.udpSessionCount--;
            // Last activity date already set by state change
        }
    }

    public static void requestTCPSession(MPipe mPipe) {
        TransformStats stats = safeStats(mPipe);
        if (stats == null)
            return;
        synchronized(stats) {
            stats.tcpSessionRequestTotal++;
            stats.lastActivityDate.setTime(MetaEnv.currentTimeMillis());
        }
    }

    public static void requestUDPSession(MPipe mPipe) {
        TransformStats stats = safeStats(mPipe);
        if (stats == null)
            return;
        synchronized(stats) {
            stats.udpSessionRequestTotal++;
            stats.lastActivityDate.setTime(MetaEnv.currentTimeMillis());
        }
    }

    public static void sessionStateChanged(MPipe mPipe) {
        TransformStats stats = safeStats(mPipe);
        if (stats == null)
            return;
        synchronized(stats) {
            stats.lastActivityDate.setTime(MetaEnv.currentTimeMillis());
        }
    }

    // Horrifically used by portal XXXXXXXXXXXXXXXXXXXXXXXXXXXXX
    public static TransformStats unholyMatrimony(TransformStats tstats, TransformStats mstats)
    {
        TransformStats result = new TransformStats();

        // Byte and Chunk counts always zero so just ignore

        result.udpSessionCount = mstats.udpSessionCount;
        result.tcpSessionCount = mstats.tcpSessionCount;
        result.udpSessionTotal = mstats.udpSessionTotal;
        result.tcpSessionTotal = mstats.tcpSessionTotal;
        result.udpSessionRequestTotal = mstats.udpSessionRequestTotal;
        result.tcpSessionRequestTotal = mstats.tcpSessionRequestTotal;
        result.startDate = tstats.startDate;
        result.lastConfigureDate = tstats.lastConfigureDate;
        result.lastActivityDate = tstats.lastActivityDate;
        for (int i = 0; i < 16; i++)
            result.incrementCount(i, mstats.getCount(i));
        return result;
    }
}
