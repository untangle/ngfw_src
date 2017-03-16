/**
 * $Id$
 */
package com.untangle.app.application_control_lite;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.app.SessionEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Log event for a proto filter match.
 */
@SuppressWarnings("serial")
public class ApplicationControlLiteEvent extends LogEvent
{
    private SessionEvent sessionEvent;
    private String protocol;
    private boolean blocked;

    public ApplicationControlLiteEvent() { }

    public ApplicationControlLiteEvent(SessionEvent sessionEvent, String protocol, boolean blocked)
    {
        this.sessionEvent = sessionEvent;
        this.protocol = protocol;
        this.blocked = blocked;
    }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public boolean getBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    public Long getSessionId()
    {
        return sessionEvent.getSessionId();
    }

    public void setSessionId( Long sessionId )
    {
        this.sessionEvent.setSessionId(sessionId);
    }

    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql =
            "UPDATE " + schemaPrefix() + "sessions" + sessionEvent.getPartitionTablePostfix() + " " +
            "SET application_control_lite_protocol = ?, " + 
            "    application_control_lite_blocked = ? " +
            "WHERE session_id = ? ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i=0;
        pstmt.setString(++i, getProtocol());
        pstmt.setBoolean(++i, getBlocked());
        pstmt.setLong(++i, sessionEvent.getSessionId());

        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        String action;
        if ( getBlocked() )
            action = I18nUtil.marktr("blocked");
        else
            action = I18nUtil.marktr("identified");
            
        String summary = "Application Control Lite" + " " + action + " " + sessionEvent.toSummaryString() + " " + getProtocol();
        return summary;
    }

}
