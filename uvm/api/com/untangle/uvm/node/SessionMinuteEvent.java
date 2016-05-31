/**
 * $Id: SessionMinuteEvent.java,v 1.00 2016/05/30 19:57:23 dmorris Exp $
 */
package com.untangle.uvm.node;

import java.net.InetAddress;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * This event stores the activity of a session on a given minute
 */
@SuppressWarnings("serial")
public class SessionMinuteEvent extends LogEvent
{
    private final long sessionId;
    private final long c2sBytes;
    private final long s2cBytes;
    
    public SessionMinuteEvent( long sessionId, long c2sBytes, long s2cBytes )
    {
        this.sessionId = sessionId;
        this.c2sBytes = c2sBytes;
        this.s2cBytes = s2cBytes;
    }

    /**
     * Session id.
     */
    public long getSessionId() { return sessionId; }

    /**
     * The number of bytes sent by the client during this minute
     */
    public long getC2sBytes() { return c2sBytes; }

    /**
     * The number of bytes sent by the client during this minute
     */
    public long getS2cBytes() { return s2cBytes; }
    
    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql = "INSERT INTO reports.session_min" + getPartitionTablePostfix() + " " +
            "(time_stamp, session_id, c2s_bytes, s2c_bytes) " +
            "values " +
            "(?, ?, ?, ?); ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        
        
        int i=0;
        pstmt.setTimestamp(++i,getTimeStamp());
        pstmt.setLong(++i,getSessionId());
        pstmt.setLong(++i,getC2sBytes());
        pstmt.setLong(++i,getS2cBytes());
        pstmt.addBatch();
        return;
    }
    
    public boolean equals(Object o)
    {
        if (o instanceof SessionMinuteEvent) {
            SessionMinuteEvent pe = (SessionMinuteEvent)o;
            if (pe.getSessionId() != getSessionId() ||
                pe.getTimeStamp() != getTimeStamp() ||
                pe.getC2sBytes() != getC2sBytes() ||
                pe.getS2cBytes() != getS2cBytes())
                return false;
            return true;
        } else {
            return false;
        }
    }

    public String toString()
    {
        return "SessionMinuteEvent: [" + sessionId + "] c2sBytes:" + c2sBytes + " s2cBytes:" + s2cBytes;
    }

    @Override
    public String toSummaryString()
    {
        return "SessionMinuteEvent: [" + sessionId + "] c2sBytes:" + c2sBytes + " s2cBytes:" + s2cBytes;
    }
    
    public int hashCode()
    {
        return ((int)sessionId) + getTimeStamp().hashCode();
    }
}
