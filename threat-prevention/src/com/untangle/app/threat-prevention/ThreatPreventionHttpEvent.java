/**
 * $Id$
 */
package com.untangle.app.threat_prevention;

import com.untangle.app.http.RequestLine;
import com.untangle.uvm.app.SessionEvent;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Log event for a web filter cation
 */
@SuppressWarnings("serial")
public class ThreatPreventionHttpEvent extends LogEvent
{
    private RequestLine requestLine;
    private SessionEvent sessionEvent;
    private Boolean blocked;
    private Boolean flagged;
    private Integer ruleId = 0;
    private Integer reputation = 0;
    private Integer categories = 0;
    
    public ThreatPreventionHttpEvent() { }

    public ThreatPreventionHttpEvent(RequestLine requestLine, SessionEvent sessionEvent, Boolean blocked, Boolean flagged, Integer ruleId, int reputation, int categories)
    {
        this.requestLine = requestLine;
        this.sessionEvent = sessionEvent;
        this.blocked = blocked;
        this.flagged = flagged;
        this.ruleId = ruleId;
        this.reputation = reputation;
        this.categories = categories;
    }

    public Boolean getBlocked() { return blocked; }
    public void setBlocked( Boolean newValue ) { this.blocked = newValue; }

    public Boolean getFlagged() { return flagged; }
    public void setFlagged( Boolean newValue ) { this.flagged = newValue; }

    public Integer getRuleId() { return ruleId; }
    public void setRuleId( Integer newValue ) { this.ruleId = newValue; }

    public Integer getReputation() { return reputation; }
    public void setReputation( Integer newValue ) { this.reputation = newValue; }

    public Integer getCategories() { return categories; }
    public void setCategories( Integer newValue ) { this.categories = newValue; }

    public RequestLine getRequestLine() { return requestLine; }
    public void setRequestLine(RequestLine newValue) { this.requestLine = newValue; }

    public SessionEvent getSessionEvent() { return sessionEvent; }
    public void setSessionEvent(SessionEvent newValue) { this.sessionEvent = newValue; }
    
    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql =
            "UPDATE " + schemaPrefix() + "http_events" + requestLine.getHttpRequestEvent().getPartitionTablePostfix() + " " +
            "SET " +
            "threat_prevention_blocked  = ?, " + 
            "threat_prevention_flagged  = ?, " +
            "threat_prevention_rule_id  = ?, " +
            "threat_prevention_reputation = ?, " +
            "threat_prevention_categories = ? " +
            "WHERE " +
            "request_id = ? ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i = 0;
        pstmt.setBoolean(++i, getBlocked());
        pstmt.setBoolean(++i, getFlagged());
        pstmt.setInt(++i, getRuleId());
        pstmt.setInt(++i, getReputation());
        pstmt.setInt(++i, getCategories());
        pstmt.setLong(++i, requestLine.getRequestId());

        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        String actionStr;
        if ( getBlocked() )
            actionStr = I18nUtil.marktr("blocked");
        else if ( getFlagged() )
            actionStr = I18nUtil.marktr("flagged");
        else
            actionStr = I18nUtil.marktr("logged");

        String summary = "Threat Prevention " + actionStr + " " + requestLine.getUrl() + " (" + getReputation() + "," + getCategories() + ")";
        return summary;
    }

}
