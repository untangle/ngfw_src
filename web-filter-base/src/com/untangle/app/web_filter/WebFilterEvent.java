/**
 * $Id$
 */
package com.untangle.app.web_filter;

import com.untangle.app.http.RequestLine;
import com.untangle.uvm.app.SessionEvent;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Log event for a web filter cation
 */
@SuppressWarnings("serial")
public class WebFilterEvent extends LogEvent
{
    private RequestLine requestLine;
    private SessionEvent sessionEvent;
    private Boolean blocked;
    private Boolean flagged;
    private Reason  reason;
    private Integer categoryId = 0;
    private Integer ruleId = 0;
    // We keep category around for events
    private String category;
    private String  appName;
    
    public WebFilterEvent() { }

    public WebFilterEvent(RequestLine requestLine, SessionEvent sessionEvent, Boolean blocked, Boolean flagged, Reason reason, Integer categoryId, Integer ruleId, String category, String appName)
    {
        this.requestLine = requestLine;
        this.sessionEvent = sessionEvent;
        this.blocked = blocked;
        this.flagged = flagged;
        this.reason = reason;
        this.categoryId = categoryId;
        this.ruleId = ruleId;
        this.category = category;
        this.appName = appName;
    }

    public Boolean getBlocked() { return blocked; }
    public void setBlocked( Boolean newValue ) { this.blocked = newValue; }

    public Boolean getFlagged() { return flagged; }
    public void setFlagged( Boolean newValue ) { this.flagged = newValue; }

    public Reason getReason() { return reason; }
    public void setReason( Reason newValue ) { this.reason = newValue; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId( Integer newValue ) { this.categoryId = newValue; }

    public Integer getRuleId() { return ruleId; }
    public void setRuleId( Integer newValue ) { this.ruleId = newValue; }

    public String getCategory() { return category; }
    public void setCategory( String newValue ) { this.category = newValue; }

    public String getAppName() { return appName; }
    public void setAppName(String newValue) { this.appName = newValue; }

    public RequestLine getRequestLine() { return requestLine; }
    public void setRequestLine(RequestLine newValue) { this.requestLine = newValue; }

    public SessionEvent getSessionEvent() { return sessionEvent; }
    public void setSessionEvent(SessionEvent newValue) { this.sessionEvent = newValue; }
    
    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String columns = "";
        if( getBlocked() != null) columns += _getDatabaseColumnNamePrefix() + "_blocked  = ?";
        if( getFlagged() != null) columns += ( columns.length() > 0 ? "," : "" ) + _getDatabaseColumnNamePrefix() + "_flagged  = ?";
        columns += ( columns.length() > 0 ? "," : "" ) + _getDatabaseColumnNamePrefix() + "_reason   = ?";
        if( getCategoryId() != null) columns += ( columns.length() > 0 ? "," : "" ) + _getDatabaseColumnNamePrefix() + "_category_id = ?";
        if( getRuleId() != null) columns += ( columns.length() > 0 ? "," : "" ) +  _getDatabaseColumnNamePrefix() + "_rule_id = ?";

        String sql =
            "UPDATE " + schemaPrefix() + "http_events" + requestLine.getHttpRequestEvent().getPartitionTablePostfix() + " " +
            "SET " +
            columns +
            " WHERE " +
            "request_id = ? ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i = 0;
        if(getBlocked() != null) pstmt.setBoolean(++i, getBlocked());
        if(getFlagged() != null ) pstmt.setBoolean(++i, getFlagged());
        pstmt.setString(++i, ((getReason() == null) ? "" : Character.toString(getReason().getKey())));
        if(getCategoryId() != null) pstmt.setInt(++i, getCategoryId());
        if(getRuleId() != null) pstmt.setInt(++i, getRuleId());
        pstmt.setLong(++i, requestLine.getRequestId());

        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        String appName;
        switch ( getAppName().toLowerCase() ) {
        case "web_filter": appName = "Web Filter"; break;
        case "web_monitor": appName = "Web Monitor"; break;
        default: appName = "Web Filter"; break;
        }

        String actionStr;
        if ( getBlocked() )
            actionStr = I18nUtil.marktr("blocked");
        else if ( getFlagged() )
            actionStr = I18nUtil.marktr("flagged");
        else
            actionStr = I18nUtil.marktr("logged");

        String summary = appName + " " + actionStr + " " + requestLine.getUrl() + " (" + getCategory() + ")";
        return summary;
    }

    private String _getDatabaseColumnNamePrefix()
    {
        String app = getAppName().toLowerCase();

        if ("web-filter".equals(app))
            return "web_filter";
        if ("web-monitor".equals(app))
            return "web_filter"; // use the same DB columns as web filter
        if ("web_monitor".equals(app))
            return "web_filter"; // use the same DB columns as web filter

        return app;
    }
}
