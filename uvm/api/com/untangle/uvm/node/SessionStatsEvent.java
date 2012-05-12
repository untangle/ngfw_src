/**
 * $Id$
 */
package com.untangle.uvm.node;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.Date;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.SessionEvent;

/**
 * Used to record the Session stats at session end time.
 * SessionStatsEvent and SessionEvent used to be the PiplineInfo
 * object.
 */
@SuppressWarnings("serial")
public class SessionStatsEvent extends LogEvent
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

    public SessionStatsEvent() { }

    public SessionStatsEvent( SessionEvent pe )
    {
        this.sessionEvent = pe;
    }

    // accessors --------------------------------------------------------------

    /**
     * Total bytes send from client to pipeline
     */
    public long getC2pBytes() { return c2pBytes; }
    public void setC2pBytes( long c2pBytes ) { this.c2pBytes = c2pBytes; }

    /**
     * Total bytes send from server to pipeline
     */
    public long getS2pBytes() { return s2pBytes; }
    public void setS2pBytes( long s2pBytes ) { this.s2pBytes = s2pBytes; }

    /**
     * Total bytes send from pipeline to client
     */
    public long getP2cBytes() { return p2cBytes; }
    public void setP2cBytes( long p2cBytes ) { this.p2cBytes = p2cBytes; }

    /**
     * Total bytes send from pipeline to server
     */
    public long getP2sBytes() { return p2sBytes; }
    public void setP2sBytes( long p2sBytes ) { this.p2sBytes = p2sBytes; }

    /**
     * Total chunks send from client to pipeline
     */
    public long getC2pChunks() { return c2pChunks; }
    public void setC2pChunks( long c2pChunks ) { this.c2pChunks = c2pChunks; }

    /**
     * Total chunks send from server to pipeline
     */
    public long getS2pChunks() { return s2pChunks; }
    public void setS2pChunks( long s2pChunks ) { this.s2pChunks = s2pChunks; }

    /**
     * Total chunks send from pipeline to client
     */
    public long getP2cChunks() { return p2cChunks; }
    public void setP2cChunks( long p2cChunks ) { this.p2cChunks = p2cChunks; }

    /**
     * Total chunks send from pipeline to server
     */
    public long getP2sChunks() { return p2sChunks; }
    public void setP2sChunks( long p2sChunks ) { this.p2sChunks = p2sChunks; }

    public Long getSessionId() { return sessionEvent.getSessionId(); }
    public void setSessionId( Long sessionId ) { this.sessionEvent.setSessionId(sessionId); }

    public SessionEvent getSessionEvent() { return sessionEvent; }
    public void setSessionEvent(SessionEvent sessionEvent) { this.sessionEvent = sessionEvent; }

    private static String sql = "UPDATE reports.sessions " +
        "SET " +
        " c2p_bytes = ?, " +
        " s2p_bytes = ?, " +
        " p2c_bytes = ?, " + 
        " p2s_bytes = ? " + 
        " WHERE " + 
        " session_id = ?";

    @Override
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );
        
        int i = 0;
        pstmt.setLong(++i,getC2pBytes());
        pstmt.setLong(++i,getS2pBytes());
        pstmt.setLong(++i,getP2cBytes());
        pstmt.setLong(++i,getP2sBytes());
        pstmt.setLong(++i,getSessionId());
        
        return pstmt;
    }
}
