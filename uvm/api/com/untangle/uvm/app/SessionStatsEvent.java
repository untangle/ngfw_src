/**
 * $Id$
 */
package com.untangle.uvm.app;

import java.sql.Timestamp;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.app.SessionEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Used to record the Session stats at session end time.
 * SessionStatsEvent and SessionEvent used to be the PiplineInfo
 * object.
 */
@SuppressWarnings("serial")
public class SessionStatsEvent extends LogEvent
{
    private long sessionId;
    private SessionEvent sessionEvent;
    private long sessionStartTime = 0;
    
    private long c2pBytes = 0;
    private long p2sBytes = 0;
    private long s2pBytes = 0;
    private long p2cBytes = 0;

    private long c2pChunks = 0;
    private long p2sChunks = 0;
    private long s2pChunks = 0;
    private long p2cChunks = 0;

    private long endTime = 0;

    public SessionStatsEvent() { }

    public SessionStatsEvent( long sessionId, long sessionStartTime )
    {
        this.sessionId = sessionId;
        this.sessionStartTime = sessionStartTime;
    }

    public SessionStatsEvent( SessionEvent sessionEvent )
    {
        this.sessionEvent = sessionEvent;
        this.sessionId = sessionEvent.getSessionId();
    }
    
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
     * Time the session ended ( 0 means no value )
     */
    public long getEndTime() { return this.endTime; }
    public void setEndTime( long newValue ) { this.endTime = newValue; }

    /**
     * The Session ID
     */
    public Long getSessionId() { return sessionId; }
    public void setSessionId( Long sessionId ) { this.sessionId = sessionId; }

    /**
     * The Session Event
     */
    public SessionEvent getSessionEvent() { return this.sessionEvent; }
    public void setSessionEvent( SessionEvent newValue ) { this.sessionEvent = newValue; }

    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql = "UPDATE " + schemaPrefix() + "sessions" + getPostfix() + " " +
            "SET " +
            ( endTime != 0 ? " end_time = ?, " : "" ) +
            " c2p_bytes = ?, " +
            " s2p_bytes = ?, " +
            " p2c_bytes = ?, " + 
            " p2s_bytes = ? " + 
            " WHERE " + 
            " session_id = ?";
        
        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        
        
        int i = 0;
        if ( endTime != 0 ) pstmt.setTimestamp(++i,new Timestamp(getEndTime()));
        pstmt.setLong(++i,getC2pBytes());
        pstmt.setLong(++i,getS2pBytes());
        pstmt.setLong(++i,getP2cBytes());
        pstmt.setLong(++i,getP2sBytes());
        pstmt.setLong(++i,getSessionId());
        
        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        String summary = I18nUtil.marktr("Session Stats") + " " + 
            I18nUtil.marktr("client-side") + "-" + I18nUtil.marktr("from-client bytes") + ": " + getC2pBytes() + ", " +
            I18nUtil.marktr("client-side") + "-" + I18nUtil.marktr("to-client bytes") + ": " + getP2cBytes() + ", " +
            I18nUtil.marktr("server-side") + "-" + I18nUtil.marktr("from-server bytes") + ": " + getS2pBytes() + ", " +
            I18nUtil.marktr("server-side") + "-" + I18nUtil.marktr("to-server bytes") + ": " + getP2sBytes();

        return summary;
    }

    private String getPostfix()
    {
        if ( sessionEvent != null )
            return sessionEvent.getPartitionTablePostfix();

        if ( sessionStartTime != 0 )
            return getPartitionTablePostfix( new java.sql.Timestamp ( this.sessionStartTime ) );

        logger.warn("Unknown start time in event: " + this);
        return getPartitionTablePostfix( new java.sql.Timestamp ( System.currentTimeMillis() ) );
    }
}
