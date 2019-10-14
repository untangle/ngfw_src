/**
 * $Id$
 */
package com.untangle.app.threat_prevention;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.app.SessionEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Log event for ip reputation.
 */
@SuppressWarnings("serial")
public class ThreatPreventionEvent extends LogEvent
{
    private SessionEvent sessionEvent;
    private long    ruleId;
    private boolean blocked;
    private boolean flagged;
    private int     clientReputation;
    private int     clientCategories;
    private int     serverReputation;
    private int     serverCategories;

    public ThreatPreventionEvent() { }

    public ThreatPreventionEvent( SessionEvent sessionEvent, boolean blocked,  boolean flagged, int ruleId , int clientReputation, int clientCategories, int serverReputation, int serverCategories)
    {
        this.sessionEvent = sessionEvent;
        this.blocked = blocked;
        this.flagged = flagged;
        this.ruleId  = ruleId;
        this.clientReputation  = clientReputation;
        this.clientCategories  = clientCategories;
        this.serverReputation  = serverReputation;
        this.serverCategories  = serverCategories;
    }

    public boolean getBlocked() { return blocked; }
    public void setBlocked( boolean blocked ) { this.blocked = blocked; }

    public boolean getFlagged() { return flagged; }
    public void setFlagged( boolean flagged ) { this.flagged = flagged; }
    
    public long getRuleId() { return ruleId; }
    public void setRuleId( long ruleId ) { this.ruleId = ruleId; }

    public int getClientReputation() { return clientReputation; }
    public void setClientReputation( int reputation ) { this.clientReputation = reputation; }

    public int getClientCategories() { return clientCategories; }
    public void setClientCategories( int categories ) { this.clientCategories = categories; }

    public int getServerReputation() { return serverReputation; }
    public void setServerReputation( int reputation ) { this.serverReputation = reputation; }

    public int getServerCategories() { return serverCategories; }
    public void setServerCategories( int categories ) { this.serverCategories = categories; }

    public Long getSessionId() { return sessionEvent.getSessionId(); }
    public void setSessionId( Long sessionId ) { this.sessionEvent.setSessionId(sessionId); }

    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql =
            "UPDATE " + schemaPrefix() + "sessions" + sessionEvent.getPartitionTablePostfix() + " " +
            "SET threat_prevention_blocked = ?, " +
            "    threat_prevention_flagged = ?, " + 
            "    threat_prevention_rule_id = ?, " + 
            "    threat_prevention_client_reputation = ?, " + 
            "    threat_prevention_client_categories = ?, " + 
            "    threat_prevention_server_reputation = ?, " + 
            "    threat_prevention_server_categories = ? " + 
            "WHERE session_id = ? ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        
        
        int i=0;
        pstmt.setBoolean(++i, getBlocked());
        pstmt.setBoolean(++i, getFlagged());
        pstmt.setLong(++i, getRuleId());
        pstmt.setInt(++i, getClientReputation());
        pstmt.setInt(++i, getClientCategories());
        pstmt.setInt(++i, getServerReputation());
        pstmt.setInt(++i, getServerCategories());
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
            
        String summary = "Threat Prevention " + action + " " + sessionEvent.toSummaryString();
        return summary;
    }
}
