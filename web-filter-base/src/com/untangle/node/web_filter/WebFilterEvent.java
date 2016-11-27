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
    public void setBlocked( Boolean blocked ) { this.blocked = blocked; }

    public Boolean getFlagged() { return flagged; }
    public void setFlagged( Boolean flagged ) { this.flagged = flagged; }

    public Reason getReason() { return reason; }
    public void setReason( Reason reason ) { this.reason = reason; }

    public String getCategory() { return category; }
    public void setCategory( String category ) { this.category = category; }

    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }

    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql =
            "UPDATE " + getSchemaPrefix() + "http_events" + requestLine.getHttpRequestEvent().getPartitionTablePostfix() + " " +
            "SET " +
            fixupNodeName() + "_blocked  = ?, " + 
            fixupNodeName() + "_flagged  = ?, " +
            fixupNodeName() + "_reason   = ?, " +
            fixupNodeName() + "_category = ? " +
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
        case "web_filter_lite": appName = "Web Filter Lite"; break;
        case "web_filter": appName = "Web Filter"; break;
        default: appName = "Web Filter"; break;
        }

        String actionStr;
        if ( getBlocked() )
            actionStr = I18nUtil.marktr("blocked");
        else
            actionStr = I18nUtil.marktr("passed");

        String summary = appName + " " + actionStr + " " + requestLine.getUrl() + " (" + getCategory() + " " + getReason() + ")";
        return summary;
    }

    private String fixupNodeName()
    {
        String node = getNodeName().toLowerCase();
        if ("web-filter-lite".equals(node))
            return "web_filter_lite";
        if ("web-filter".equals(node))
            return "web_filter";
        return node;
    }
}
