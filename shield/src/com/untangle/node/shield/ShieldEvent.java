/**
 * $Id$
 */
package com.untangle.node.shield;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Log event for the shield.
 */
@SuppressWarnings("serial")
public class ShieldEvent extends LogEvent
{
    private SessionEvent sessionEvent;
    private boolean blocked;

    // Constructors
    public ShieldEvent() { }

    public ShieldEvent( SessionEvent sessionEvent, boolean blocked )
    {
        this.sessionEvent = sessionEvent;
        this.blocked = blocked;
    }

    public boolean getBlocked() { return blocked; }
    public void setBlocked( boolean blocked ) { this.blocked = blocked; }

    public Long getSessionId() { return sessionEvent.getSessionId(); }
    public void setSessionId( Long sessionId ) { this.sessionEvent.setSessionId(sessionId); }

    @Override
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        String sql =
            "UPDATE reports.sessions" + sessionEvent.getPartitionTablePostfix() + " " +
            "SET shield_blocked = ? " +
            "WHERE session_id = ? ";

        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );

        int i=0;
        pstmt.setBoolean(++i, getBlocked());
        pstmt.setLong(++i, getSessionId());

        return pstmt;
    }

    @Override
    public String toSummaryString()
    {
        String action;
        if ( getBlocked() )
            action = I18nUtil.marktr("blocked");
        else
            action = I18nUtil.marktr("passed");

        String summary = I18nUtil.marktr("The shield") + " " + action + " " + sessionEvent.toSummaryString();
        return summary;
    }
}
