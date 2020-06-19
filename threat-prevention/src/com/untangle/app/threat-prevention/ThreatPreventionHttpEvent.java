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
    private ThreatPreventionReason  reason;
    private Integer ruleId = 0;
    private Integer clientReputation = 0;
    private Integer clientCategories = 0;
    private Integer serverReputation = 0;
    private Integer serverCategories = 0;
    
    public ThreatPreventionHttpEvent() { }

    public ThreatPreventionHttpEvent(RequestLine requestLine, SessionEvent sessionEvent, Boolean blocked, Boolean flagged, ThreatPreventionReason reason, Integer ruleId, int clientReputation, int clientCategories, int serverReputation, int serverCategories)
    {
        this.requestLine = requestLine;
        this.sessionEvent = sessionEvent;
        this.blocked = blocked;
        this.flagged = flagged;
        this.reason = reason;
        this.ruleId = ruleId;
        this.clientReputation = clientReputation;
        this.clientCategories = clientCategories;
        this.serverReputation = serverReputation;
        this.serverCategories = serverCategories;
    }

    public Boolean getBlocked() { return blocked; }
    public void setBlocked( Boolean newValue ) { this.blocked = newValue; }

    public Boolean getFlagged() { return flagged; }
    public void setFlagged( Boolean newValue ) { this.flagged = newValue; }

    public ThreatPreventionReason getReason() { return reason; }
    public void setReason( ThreatPreventionReason reason ) { this.reason = reason; }

    public Integer getRuleId() { return ruleId; }
    public void setRuleId( Integer newValue ) { this.ruleId = newValue; }

    public Integer getClientReputation() { return clientReputation; }
    public void setClientReputation( Integer newValue ) { this.clientReputation = newValue; }

    public Integer getClientCategories() { return clientCategories; }
    public void setClientCategories( Integer newValue ) { this.clientCategories = newValue; }

    public Integer getServerReputation() { return serverReputation; }
    public void setServerReputation( Integer newValue ) { this.serverReputation = newValue; }

    public Integer getServerCategories() { return serverCategories; }
    public void setServerCategories( Integer newValue ) { this.serverCategories = newValue; }

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
            "threat_prevention_reason = ?, " + 
            "threat_prevention_rule_id  = ?, " +
            "threat_prevention_client_reputation = ?, " +
            "threat_prevention_client_categories = ?, " +
            "threat_prevention_server_reputation = ?, " +
            "threat_prevention_server_categories = ? " +
            "WHERE " +
            "request_id = ? ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i = 0;
        pstmt.setBoolean(++i, getBlocked());
        pstmt.setBoolean(++i, getFlagged());
        pstmt.setString(++i, ((getReason() == null) ? "" : Character.toString(getReason().getKey())));
        pstmt.setInt(++i, getRuleId());
        pstmt.setInt(++i, getClientReputation());
        pstmt.setInt(++i, getClientCategories());
        pstmt.setInt(++i, getServerReputation());
        pstmt.setInt(++i, getServerCategories());
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

        String summary = "Threat Prevention " + actionStr + " " + requestLine.getUrl() + " (" + getClientReputation() + "," + getClientCategories() + getServerReputation() + "," + getServerCategories() + "," +")";
        return summary;
    }

}
