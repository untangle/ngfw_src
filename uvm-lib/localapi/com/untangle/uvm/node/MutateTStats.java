/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.node;

import com.untangle.uvm.vnet.MPipe;
import com.untangle.uvm.vnet.Session;
import com.untangle.uvm.util.MetaEnv;

/**
 * <code>MutateTStats</code> is a helper class that allows VNet to modify NodeStats
 * (which are normally read-only).
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
public final class MutateTStats {

    // Duplicates IPSessionState. XX
    public static final int CLIENT_TO_SERVER = 1;
    public static final int SERVER_TO_CLIENT = 2;

    private static NodeStats safeStats(Session sess) {
        try {
            // Node may not be running any more
            return sess.mPipe().node().getStats();
        } catch (IllegalStateException x) {
            return null;
        }
    }

    private static NodeStats safeStats(MPipe pipe) {
        try {
            // Node may not be running any more
            return pipe.node().getStats();
        } catch (IllegalStateException x) {
            return null;
        }
    }

    public static void readData(int direction, Session sess, long bytes) {
        NodeStats stats = safeStats(sess);
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
        NodeStats stats = safeStats(sess);
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
        NodeStats stats = safeStats(sess);
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
        NodeStats stats = safeStats(sess);
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
        NodeStats stats = safeStats(mPipe);
        if (stats == null)
            return;
        synchronized(stats) {
            stats.tcpSessionCount++;
            stats.tcpSessionTotal++;
            stats.lastActivityDate.setTime(MetaEnv.currentTimeMillis());
        }
    }

    public static void removeTCPSession(MPipe mPipe) {
        NodeStats stats = safeStats(mPipe);
        if (stats == null)
            return;
        synchronized(stats) {
            stats.tcpSessionCount--;
            // Last activity date already set by state change
        }
    }

    public static void addUDPSession(MPipe mPipe) {
        NodeStats stats = safeStats(mPipe);
        if (stats == null)
            return;
        synchronized(stats) {
            stats.udpSessionCount++;
            stats.udpSessionTotal++;
            stats.lastActivityDate.setTime(MetaEnv.currentTimeMillis());
        }
    }

    public static void removeUDPSession(MPipe mPipe) {
        NodeStats stats = safeStats(mPipe);
        if (stats == null)
            return;
        synchronized(stats) {
            stats.udpSessionCount--;
            // Last activity date already set by state change
        }
    }

    public static void requestTCPSession(MPipe mPipe) {
        NodeStats stats = safeStats(mPipe);
        if (stats == null)
            return;
        synchronized(stats) {
            stats.tcpSessionRequestTotal++;
            stats.lastActivityDate.setTime(MetaEnv.currentTimeMillis());
        }
    }

    public static void requestUDPSession(MPipe mPipe) {
        NodeStats stats = safeStats(mPipe);
        if (stats == null)
            return;
        synchronized(stats) {
            stats.udpSessionRequestTotal++;
            stats.lastActivityDate.setTime(MetaEnv.currentTimeMillis());
        }
    }

    public static void sessionStateChanged(MPipe mPipe) {
        NodeStats stats = safeStats(mPipe);
        if (stats == null)
            return;
        synchronized(stats) {
            stats.lastActivityDate.setTime(MetaEnv.currentTimeMillis());
        }
    }

    // Horrifically used by portal XXXXXXXXXXXXXXXXXXXXXXXXXXXXX
    public static NodeStats unholyMatrimony(NodeStats tstats, NodeStats mstats)
    {
        NodeStats result = new NodeStats();

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
