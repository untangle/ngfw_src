/**
 * $Id$
 */
package com.untangle.node.webfilter;

import com.untangle.node.http.RequestLine;
import com.untangle.uvm.logging.LogEvent;

/**
 * Log event for a web filter cation
 */
@SuppressWarnings("serial")
public class WebFilterEvent extends LogEvent
{
    private Long requestId;
    private Boolean blocked;
    private Boolean flagged;
    private Reason  reason;
    private String  category;
    private String  nodeName;

    public WebFilterEvent() { }

    public WebFilterEvent(RequestLine requestLine, Boolean blocked, Boolean flagged, Reason reason, String category, String nodeName)
    {
        this.requestId = requestLine.getRequestId();
        this.blocked = blocked;
        this.flagged = flagged;
        this.reason = reason;
        this.category = category;
        this.nodeName = nodeName;
    }

    public Long getRequestId() { return requestId; }
    public void setRequestId( Long requestId ) { this.requestId = requestId; }

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
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        String sql =
            "UPDATE reports.http_events " +
            "SET " +
            getNodeName().toLowerCase() + "_blocked  = ?, " + 
            getNodeName().toLowerCase() + "_flagged  = ?, " +
            getNodeName().toLowerCase() + "_reason   = ?, " +
            getNodeName().toLowerCase() + "_category = ? " +
            "WHERE " +
            "request_id = ? ";
        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );

        int i = 0;
        pstmt.setBoolean(++i, getBlocked());
        pstmt.setBoolean(++i, getFlagged());
        pstmt.setString(++i, ((getReason() == null) ? "" : Character.toString(getReason().getKey())));
        pstmt.setString(++i, getCategory());
        pstmt.setLong(++i, getRequestId());
        return pstmt;
    }
}
