/**
 * $Id$
 */
package com.untangle.node.spyware;

import com.untangle.uvm.node.SessionEvent;
import com.untangle.node.http.HttpRequestEvent;
import com.untangle.node.http.RequestLine;
import com.untangle.uvm.logging.LogEvent;

/**
 * Log event for a spyware hit.
 */
@SuppressWarnings("serial")
public class SpywareUrlEvent extends LogEvent
{
    private Long requestId;
    private RequestLine requestLine; // pipeline endpoints & location

    public SpywareUrlEvent() { }

    public SpywareUrlEvent(RequestLine requestLine)
    {
        this.requestId = requestLine.getRequestId();
        this.requestLine = requestLine;
    }

    public String getIdentification()
    {
        HttpRequestEvent hre = requestLine.getHttpRequestEvent();
        String host = null == hre ? getSessionEvent().getSServerAddr().toString() : hre.getHost();
        return "http://" + host + requestLine.getRequestUri().toString();
    }

    public Boolean getBlocked()
    {
        return true;
    }

    public SessionEvent getSessionEvent()
    {
        return requestLine.getSessionEvent();
    }

    public Long getRequestId() { return requestId; }
    public void setRequestId( Long requestId ) { this.requestId = requestId; }

    private static String sql =
        "UPDATE reports.http_events " +
        "SET " +
        "sw_blacklisted = ? " +
        "WHERE " +
        "request_id = ? ";

    @Override
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );
        
        int i = 0;
        pstmt.setBoolean(++i,getBlocked());
        pstmt.setLong(++i,getRequestId());
        
        return pstmt;
    }
}
