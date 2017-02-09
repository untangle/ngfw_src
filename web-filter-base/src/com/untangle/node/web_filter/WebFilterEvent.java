/**
 * $Id$
 */
package com.untangle.node.web_filter;

import com.untangle.node.http.RequestLine;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Log event for a web filter cation
 */
@SuppressWarnings("serial")
public class WebFilterEvent extends LogEvent
{
    private RequestLine requestLine;
    private Boolean blocked;
    private Boolean flagged;
    private Reason  reason;
    private String  category;
    private String  nodeName;
    
    public WebFilterEvent() { }

    public WebFilterEvent(RequestLine requestLine, Boolean blocked, Boolean flagged, Reason reason, String category, String nodeName)
    {
        this.requestLine = requestLine;
        this.blocked = blocked;
        this.flagged = flagged;
        this.reason = reason;
        this.category = category;
        this.nodeName = nodeName;
    }

    public Boolean getBlocked() { return blocked; }
    public void setBlocked( Boolean newValue ) { this.blocked = newValue; }

    public Boolean getFlagged() { return flagged; }
    public void setFlagged( Boolean newValue ) { this.flagged = newValue; }

    public Reason getReason() { return reason; }
    public void setReason( Reason newValue ) { this.reason = newValue; }

    public String getCategory() { return category; }
    public void setCategory( String newValue ) { this.category = newValue; }

    public String getNodeName() { return nodeName; }
    public void setNodeName(String newValue) { this.nodeName = newValue; }

    public RequestLine getRequestLine() { return requestLine; }
    public void setRequestLine(RequestLine newValue) { this.requestLine = newValue; }
    
    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql =
            "UPDATE " + schemaPrefix() + "http_events" + requestLine.getHttpRequestEvent().getPartitionTablePostfix() + " " +
            "SET " +
            _getDatabaseColumnNamePrefix() + "_blocked  = ?, " + 
            _getDatabaseColumnNamePrefix() + "_flagged  = ?, " +
            _getDatabaseColumnNamePrefix() + "_reason   = ?, " +
            _getDatabaseColumnNamePrefix() + "_category = ? " +
            "WHERE " +
            "request_id = ? ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i = 0;
        pstmt.setBoolean(++i, getBlocked());
        pstmt.setBoolean(++i, getFlagged());
        pstmt.setString(++i, ((getReason() == null) ? "" : Character.toString(getReason().getKey())));
        pstmt.setString(++i, getCategory());
        pstmt.setLong(++i, requestLine.getRequestId());

        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        String appName;
        switch ( getNodeName().toLowerCase() ) {
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
        String node = getNodeName().toLowerCase();

        if ("web-filter".equals(node))
            return "web_filter";
        if ("web-monitor".equals(node))
            return "web_filter"; // use the same DB columns as web filter
        if ("web_monitor".equals(node))
            return "web_filter"; // use the same DB columns as web filter

        return node;
    }
}
