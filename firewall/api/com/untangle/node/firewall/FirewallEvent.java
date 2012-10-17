/**
 * $Id$
 */
package com.untangle.node.firewall;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.SessionEvent;

/**
 * Log event for the firewall.
 */
@SuppressWarnings("serial")
public class FirewallEvent extends LogEvent
{
    private SessionEvent sessionEvent;
    private long    ruleId;
    private boolean blocked;
    private boolean flagged;

    // Constructors
    public FirewallEvent() { }

    public FirewallEvent( SessionEvent sessionEvent, boolean blocked,  boolean flagged, int ruleId )
    {
        this.sessionEvent = sessionEvent;
        this.blocked = blocked;
        this.flagged = flagged;
        this.ruleId  = ruleId;
    }

    public boolean getBlocked() { return blocked; }
    public void setBlocked( boolean blocked ) { this.blocked = blocked; }

    public boolean getFlagged() { return flagged; }
    public void setFlagged( boolean flagged ) { this.flagged = flagged; }
    
    public long getRuleId() { return ruleId; }
    public void setRuleId( long ruleId ) { this.ruleId = ruleId; }

    public Long getSessionId() { return sessionEvent.getSessionId(); }
    public void setSessionId( Long sessionId ) { this.sessionEvent.setSessionId(sessionId); }

    //public SessionEvent getSessionEvent() { return sessionEvent; }
    //public void setSessionEvent(SessionEvent sessionEvent) { this.sessionEvent = sessionEvent; }
    
    private static String sql =
        "UPDATE reports.sessions " + 
        "SET firewall_blocked = ?, " +
        "    firewall_flagged = ?, " + 
        "    firewall_rule_index = ? " + 
        "WHERE session_id = ? ";

    @Override
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );

        int i=0;
        pstmt.setBoolean(++i, getBlocked());
        pstmt.setBoolean(++i, getFlagged());
        pstmt.setLong(++i, getRuleId());
        pstmt.setLong(++i, getSessionId());

        return pstmt;
    }
}
