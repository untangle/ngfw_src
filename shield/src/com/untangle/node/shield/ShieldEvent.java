/**
 * $Id: ShieldEvent.java 33317 2012-10-17 19:12:21Z dmorris $
 */
package com.untangle.node.shield;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.SessionEvent;

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

    private static String sql =
        "UPDATE reports.sessions " + 
        "SET shield_blocked = ? " +
        "WHERE session_id = ? ";

    @Override
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );

        int i=0;
        pstmt.setBoolean(++i, getBlocked());
        pstmt.setLong(++i, getSessionId());

        return pstmt;
    }
}
