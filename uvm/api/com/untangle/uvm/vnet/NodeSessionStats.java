/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.io.Serializable;
import java.util.Date;

/**
 * <code>NodeSessionStats</code> records statistics for a node session.
 */
@SuppressWarnings("serial")
public class NodeSessionStats implements Serializable
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

    public NodeSessionStats()
    {
        long now = System.currentTimeMillis();
        creationDate = new Date(now);
        lastActivityDate = new Date(now);
    }

    public NodeSessionStats(NodeSessionStats oldStats)
    {
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
    public long c2tBytes()
    {
        return c2tBytes;
    }

    /**
     * <code>t2sBytes</code> gives the count of bytes transferred from the node to the server.
     * This may not be the same as <code>c2tBytes</code> if the node is changing the data.
     *
     * @return a <code>long</code> giving the number of bytes transferred from the node to the server.
     */
    public long t2sBytes()
    {
        return t2sBytes;
    }

    /**
     * <code>s2tBytes</code> gives the count of bytes transferred from the server to the node.
     * This may not be the same as <code>t2cBytes</code> if the node is changing the data.
     *
     * @return a <code>long</code> giving the number of bytes transferred from the server to the node.
     */
    public long s2tBytes()
    {
        return s2tBytes;
    }

    /**
     * <code>t2cBytes</code> gives the count of bytes transferred from the node to the client.
     * This may not be the same as <code>s2tBytes</code> if the node is changing the data.
     *
     * @return a <code>long</code> giving the number of bytes transferred from the node to the client.
     */
    public long t2cBytes()
    {
        return t2cBytes;
    }

    // Chunks for tcp, packets for udp (icmp)
    public long c2tChunks()
    {
        return c2tChunks;
    }

    public long t2sChunks()
    {
        return t2sChunks;
    }

    public long s2tChunks()
    {
        return s2tChunks;
    }

    public long t2cChunks()
    {
        return t2cChunks;
    }

    /**
     * <code>creationDate</code> gives the time of the session's creation.  This is defined
     * as the time this node received the new-session event (not the request).
     *
     * @return a <code>Date</code> giving the time of the session's creation
     */
    public Date creationDate()
    {
        return creationDate;
    }

    /**
     * <code>lastActivityDate</code> gives the time of the last activity on the session.
     * Activity is any bytes, creation, or close/shutdown.
     *
     * @return a <code>Date</code> giving the time of the last activity on this session
     */
    public Date lastActivityDate()
    {
        return lastActivityDate;
    }

    public void readData(int side, long bytes)
    {
        if (side == NodeSession.CLIENT) {
            c2tChunks++;
            c2tBytes += bytes;
        } else {
            s2tChunks++;
            s2tBytes += bytes;
        }
        lastActivityDate.setTime(System.currentTimeMillis());
    }

    public void wroteData(int side, long bytes)
    {
        if (side == NodeSession.SERVER) {
            t2sChunks++;
            t2sBytes += bytes;
        } else {
            t2cChunks++;
            t2cBytes += bytes;
        }
        lastActivityDate.setTime(System.currentTimeMillis());
    }

    public void stateChanged()
    {
        lastActivityDate.setTime(System.currentTimeMillis());
    }

}
