/**
 * $Id$
 */
package com.untangle.uvm.node;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Type;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.node.SessionEvent;

/**
 * Used to record the Session stats at session end time.
 * PipelineStats and SessionEvent used to be the PiplineInfo
 * object.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="pl_stats", schema="events")
@SuppressWarnings("serial")
public class PipelineStats extends LogEvent
{
    private SessionEvent sessionEvent;

    private long c2pBytes = 0;
    private long p2sBytes = 0;
    private long s2pBytes = 0;
    private long p2cBytes = 0;

    private long c2pChunks = 0;
    private long p2sChunks = 0;
    private long s2pChunks = 0;
    private long p2cChunks = 0;

    private String uid;

    // constructors -----------------------------------------------------------

    public PipelineStats() { }

    public PipelineStats(SessionStats begin, SessionStats end, SessionEvent pe)
    {
        this.sessionEvent = pe;

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
     * Total bytes send from client to pipeline
     *
     * @return the number of bytes sent from the client into the pipeline
     */

    @Column(name="c2p_bytes", nullable=false)
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
     */
    @Column(name="s2p_bytes", nullable=false)
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
     */
    @Column(name="p2c_bytes", nullable=false)
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
     */
    @Column(name="p2s_bytes", nullable=false)
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
     */
    @Column(name="c2p_chunks", nullable=false)
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
     */
    @Column(name="s2p_chunks", nullable=false)
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
     */
    @Column(name="p2c_chunks", nullable=false)
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
     */
    @Column(name="p2s_chunks", nullable=false)
    public long getP2sChunks()
    {
        return p2sChunks;
    }

    public void setP2sChunks(long p2sChunks)
    {
        this.p2sChunks = p2sChunks;
    }

    @Column(name="session_id", nullable=false)
    public Long getSessionId()
    {
        return sessionEvent.getSessionId();
    }

    public void setSessionId( Long sessionId )
    {
        this.sessionEvent.setSessionId(sessionId);
    }

    @Transient
    public SessionEvent getSessionEvent()
    {
        return sessionEvent;
    }

    public void setSessionEvent(SessionEvent sessionEvent)
    {
        this.sessionEvent = sessionEvent;
    }


    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        getSessionEvent().appendSyslog(sb);

        sb.startSection("stats");
        sb.addField("raze-date", getTimeStamp());
        sb.addField("c2pBytes", c2pBytes);
        sb.addField("p2sBytes", p2sBytes);
        sb.addField("s2pBytes", s2pBytes);
        sb.addField("p2cBytes", p2cBytes);
        sb.addField("c2pChunks", c2pChunks);
        sb.addField("p2sChunks", p2sChunks);
        sb.addField("s2pChunks", s2pChunks);
        sb.addField("p2cChunks", p2cChunks);
        sb.addField("uid", uid);
    }

    // reuse default getSyslogId
    // reuse default getSyslogPriority
}
