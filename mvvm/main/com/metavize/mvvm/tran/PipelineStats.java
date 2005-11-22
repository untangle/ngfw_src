/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.tran;

import java.util.Date;

import com.metavize.mvvm.argon.IPSessionDesc;
import com.metavize.mvvm.logging.PipelineEvent;
import com.metavize.mvvm.logging.SyslogBuilder;
import com.metavize.mvvm.logging.SyslogPriority;

/**
 * Used to record the Session stats at session end time.
 * PipelineStats and PipelineEndpoints used to be the PiplineInfo
 * object.
 *
 * @author <a href="mailto:jdi@metavize.com">John Irwin</a>
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="PL_STATS"
 * mutable="false"
 */
public class PipelineStats extends PipelineEvent
{
    private static final long serialVersionUID = 2479594766473917892L;

    private Date razeDate;

    private long c2pBytes = 0;
    private long p2sBytes = 0;
    private long s2pBytes = 0;
    private long p2cBytes = 0;

    private long c2pChunks = 0;
    private long p2sChunks = 0;
    private long s2pChunks = 0;
    private long p2cChunks = 0;

    // constructors -----------------------------------------------------------

    public PipelineStats() { }

    public PipelineStats(IPSessionDesc begin, IPSessionDesc end, PipelineEndpoints pe)
    {
        super(pe);

        razeDate = new Date();

        c2pBytes = begin.c2tBytes();
        p2cBytes = begin.t2cBytes();
        c2pChunks = begin.c2tChunks();
        p2cChunks = begin.t2cChunks();

        p2sBytes = end.t2sBytes();
        s2pBytes = end.s2tBytes();
        p2sChunks = end.t2sChunks();
        s2pChunks = end.s2tChunks();
    }

    // accessors --------------------------------------------------------------

    /**
     * Time the session ended
     *
     * @return the time the session ended
     * @hibernate.property
     * column="RAZE_DATE"
     */
    public Date getRazeDate()
    {
        return razeDate;
    }

    public void setRazeDate(Date razeDate)
    {
        this.razeDate = razeDate;
    }

    /**
     * Total bytes send from client to pipeline
     *
     * @return the number of bytes sent from the client into the pipeline
     * @hibernate.property
     * column="C2P_BYTES"
     */
    public long getC2pBytes()
    {
        return c2pBytes;
    }

    public void setC2pBytes(long c2pBytes)
    {
        this.c2pBytes = c2pBytes;
    }

    /**
     * Total bytes send from server to pipeline
     *
     * @return the number of bytes sent from the server into the pipeline
     * @hibernate.property
     * column="S2P_BYTES"
     */
    public long getS2pBytes()
    {
        return s2pBytes;
    }

    public void setS2pBytes(long s2pBytes)
    {
        this.s2pBytes = s2pBytes;
    }

    /**
     * Total bytes send from pipeline to client
     *
     * @return the number of bytes sent from the pipeline to the client
     * @hibernate.property
     * column="P2C_BYTES"
     */
    public long getP2cBytes()
    {
        return p2cBytes;
    }

    public void setP2cBytes(long p2cBytes)
    {
        this.p2cBytes = p2cBytes;
    }

    /**
     * Total bytes send from pipeline to server
     *
     * @return the number of bytes sent from the pipeline to the server
     * @hibernate.property
     * column="P2S_BYTES"
     */
    public long getP2sBytes()
    {
        return p2sBytes;
    }

    public void setP2sBytes(long p2sBytes)
    {
        this.p2sBytes = p2sBytes;
    }

    /**
     * Total chunks send from client to pipeline
     *
     * @return the number of chunks sent from the client into the pipeline
     * @hibernate.property
     * column="C2P_CHUNKS"
     */
    public long getC2pChunks()
    {
        return c2pChunks;
    }

    public void setC2pChunks(long c2pChunks)
    {
        this.c2pChunks = c2pChunks;
    }

    /**
     * Total chunks send from server to pipeline
     *
     * @return the number of chunks sent from the server into the pipeline
     * @hibernate.property
     * column="S2P_CHUNKS"
     */
    public long getS2pChunks()
    {
        return s2pChunks;
    }

    public void setS2pChunks(long s2pChunks)
    {
        this.s2pChunks = s2pChunks;
    }

    /**
     * Total chunks send from pipeline to client
     *
     * @return the number of chunks sent from the pipeline to the client
     * @hibernate.property
     * column="P2C_CHUNKS"
     */
    public long getP2cChunks()
    {
        return p2cChunks;
    }

    public void setP2cChunks(long p2cChunks)
    {
        this.p2cChunks = p2cChunks;
    }

    /**
     * Total chunks send from pipeline to server
     *
     * @return the number of chunks sent from the pipeline to the server
     * @hibernate.property
     * column="P2S_CHUNKS"
     */
    public long getP2sChunks()
    {
        return p2sChunks;
    }

    public void setP2sChunks(long p2sChunks)
    {
        this.p2sChunks = p2sChunks;
    }

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        getPipelineEndpoints().appendSyslog(sb);

        sb.startSection("stats");

        sb.addField("c2pBytes", c2pBytes);
        sb.addField("p2sBytes", p2sBytes);
        sb.addField("s2pBytes", s2pBytes);
        sb.addField("p2cBytes", p2cBytes);

        sb.addField("c2pChunks", c2pChunks);
        sb.addField("p2sChunks", c2pChunks);
        sb.addField("s2pChunks", c2pChunks);
        sb.addField("p2cChunks", c2pChunks);
    }

    public SyslogPriority getSyslogPrioritiy()
    {
        return SyslogPriority.DEBUG;
    }
}
