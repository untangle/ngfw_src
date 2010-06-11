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

package com.untangle.uvm.vnet;

import java.io.Serializable;
import java.util.Date;

import org.json.JSONBean;

/**
 * <code>SessionStats</code> records vital statistics for a live session.
 * It is contained within a Session (or a SessionDesc when used by the GUI).
 *
 * XXX if this is for the client, it should probably be moved to
 * com.untangle.uvm.node.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
@JSONBean.Marker
public class SessionStats implements Serializable
{

    protected long c2tBytes = 0;
    protected long t2sBytes = 0;
    protected long s2tBytes = 0;
    protected long t2cBytes = 0;

    protected long c2tChunks = 0;
    protected long t2sChunks = 0;
    protected long s2tChunks = 0;
    protected long t2cChunks = 0;

    protected Date creationDate;
    protected Date lastActivityDate;

    public static final int MIN_TIME_INDEX = 1;
    public static final int NEW_SESSION_RECEIVED = 1;
    public static final int DISPATCH_REQUEST = 2;
    public static final int REQUEST_HANDLED = 3;
    public static final int MADE_SESSION = 4;
    public static final int OPENED_CLIENT_CHANNEL = 5;
    public static final int OPENED_SERVER_CHANNEL = 6;
    public static final int DISPATCH_NEW = 7;
    public static final int NEW_HANDLED = 8;
    public static final int FINISH_NEW = 9;
    public static final int FIRST_READABLE_CLIENT = 10;
    public static final int FIRST_READABLE_SERVER = 11;
    public static final int FIRST_BYTE_READ_FROM_CLIENT = 12;
    public static final int FIRST_BYTE_READ_FROM_SERVER = 13;
    public static final int FIRST_BYTE_WROTE_TO_CLIENT = 14;
    public static final int FIRST_BYTE_WROTE_TO_SERVER = 15;
    public static final int FINAL_CLOSE = 16;
    public static final int MAX_TIME_INDEX = 17;
    protected long[] times;

    public static final String[] TimeNames;
    static {
        TimeNames = new String[MAX_TIME_INDEX];
        TimeNames[NEW_SESSION_RECEIVED] = "new session received";
        TimeNames[DISPATCH_REQUEST] = "dispatch request";
        TimeNames[REQUEST_HANDLED] = "request handled";
        TimeNames[MADE_SESSION] = "made session";
        TimeNames[OPENED_CLIENT_CHANNEL] = "opened client channel";
        TimeNames[OPENED_SERVER_CHANNEL] = "opened server channel";
        TimeNames[DISPATCH_NEW] = "dispatch new";
        TimeNames[NEW_HANDLED] = "new handled";
        TimeNames[FINISH_NEW] = "finish new";
        TimeNames[FIRST_READABLE_CLIENT] = "first readable client";
        TimeNames[FIRST_READABLE_SERVER] = "first readable server";
        TimeNames[FIRST_BYTE_READ_FROM_CLIENT] = "first byte read from client";
        TimeNames[FIRST_BYTE_READ_FROM_SERVER] = "first byte read from server";
        TimeNames[FIRST_BYTE_WROTE_TO_CLIENT] = "first byte wrote to client";
        TimeNames[FIRST_BYTE_WROTE_TO_SERVER] = "first byte wrote to server";
        TimeNames[FINAL_CLOSE] = "final close";
    }

    protected SessionStats() {
    }

    public SessionStats(SessionStats oldStats) {
        this.c2tBytes = oldStats.c2tBytes;
        this.t2sBytes = oldStats.t2sBytes;
        this.s2tBytes = oldStats.s2tBytes;
        this.t2cBytes = oldStats.t2cBytes;
        this.c2tChunks = oldStats.c2tChunks;
        this.t2sChunks = oldStats.t2sChunks;
        this.s2tChunks = oldStats.s2tChunks;
        this.t2cChunks = oldStats.t2cChunks;
        this.creationDate = oldStats.creationDate;
        this.lastActivityDate = oldStats.lastActivityDate;
    }

    /**
     * <code>c2tBytes</code> gives the count of bytes transferred from the client to the node.
     * This may not be the same as <code>t2sBytes</code> if the node is changing the data.
     *
     * @return a <code>long</code> giving the number of bytes transferred from the client to the node.
     */
    @JSONBean.Getter
    public long c2tBytes() {
        return c2tBytes;
    }

    /**
     * <code>t2sBytes</code> gives the count of bytes transferred from the node to the server.
     * This may not be the same as <code>c2tBytes</code> if the node is changing the data.
     *
     * @return a <code>long</code> giving the number of bytes transferred from the node to the server.
     */
    @JSONBean.Getter
    public long t2sBytes() {
        return t2sBytes;
    }

    /**
     * <code>s2tBytes</code> gives the count of bytes transferred from the server to the node.
     * This may not be the same as <code>t2cBytes</code> if the node is changing the data.
     *
     * @return a <code>long</code> giving the number of bytes transferred from the server to the node.
     */
    @JSONBean.Getter
    public long s2tBytes() {
        return s2tBytes;
    }

    /**
     * <code>t2cBytes</code> gives the count of bytes transferred from the node to the client.
     * This may not be the same as <code>s2tBytes</code> if the node is changing the data.
     *
     * @return a <code>long</code> giving the number of bytes transferred from the node to the client.
     */
    @JSONBean.Getter
    public long t2cBytes() {
        return t2cBytes;
    }

    // Chunks for tcp, packets for udp (icmp)
    @JSONBean.Getter
    public long c2tChunks() {
        return c2tChunks;
    }

    @JSONBean.Getter
    public long t2sChunks() {
        return t2sChunks;
    }

    @JSONBean.Getter
    public long s2tChunks() {
        return s2tChunks;
    }

    @JSONBean.Getter
    public long t2cChunks() {
        return t2cChunks;
    }

    /**
     * <code>creationDate</code> gives the time of the session's creation.  This is defined
     * as the time this node received the new-session event (not the request).
     *
     * @return a <code>Date</code> giving the time of the session's creation
     */
    @JSONBean.Getter
    public Date creationDate() {
        return creationDate;
    }

    /**
     * <code>lastActivityDate</code> gives the time of the last activity on the session.
     * Activity is any bytes, creation, or close/shutdown.
     *
     * @return a <code>Date</code> giving the time of the last activity on this session
     */
    @JSONBean.Getter
    public Date lastActivityDate() {
        return lastActivityDate;
    }

    @JSONBean.Getter
    public long[] times() {
        return times;
    }
}
