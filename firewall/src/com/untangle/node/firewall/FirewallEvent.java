/**
 * $Id$
 */
package com.untangle.node.firewall;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.util.I18nUtil;

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

    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql =
            "UPDATE " + schemaPrefix() + "sessions" + sessionEvent.getPartitionTablePostfix() + " " +
            "SET firewall_blocked = ?, " +
            "    firewall_flagged = ?, " + 
            "    firewall_rule_index = ? " + 
            "WHERE session_id = ? ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        
        
        int i=0;
        pstmt.setBoolean(++i, getBlocked());
        pstmt.setBoolean(++i, getFlagged());
        pstmt.setLong(++i, getRuleId());
        pstmt.setLong(++i, getSessionId());

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
            action = I18nUtil.marktr("password");
            
        String summary = "Firewall " + action + " " + sessionEvent.toSummaryString();
        return summary;
    }
}
