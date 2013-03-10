/**
 * $Id$
 */
package com.untangle.node.spyware;

import com.untangle.uvm.node.SessionEvent;
import com.untangle.node.http.RequestLine;
import com.untangle.uvm.logging.LogEvent;

/**
 * Log event for a spyware hit.
 */
@SuppressWarnings("serial")
public class SpywareCookieEvent extends LogEvent
{
    private Long requestId;
    private String identification;
    private RequestLine requestLine; // pipeline endpoints & location

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public SpywareCookieEvent() { }

    public SpywareCookieEvent(RequestLine requestLine, String identification)
    {
        this.requestId = requestLine.getRequestId();
        this.identification = identification;
        this.requestLine = requestLine;
    }

    // SpywareEvent methods ---------------------------------------------------

    public Boolean getBlocked()
    {
        return true;
    }

    public SessionEvent getSessionEvent()
    {
        return requestLine.getSessionEvent();
    }

    // accessors --------------------------------------------------------------

    /**
     * Request line for this HTTP response pair.
     */
    public Long getRequestId() { return requestId; }
    public void setRequestId( Long requestId ) { this.requestId = requestId; }

    /**
     * The identification (name of IP address range matched)
     */
    public String getIdentification() { return identification; }
    public void setIdentification( String identification ) { this.identification = identification; }

    private static String sql =
        "UPDATE reports.http_events " +
        "SET " +
        "spyware_cookie_ident = ? " +
        "WHERE " +
        "request_id = ? ";

    @Override
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );
        
        int i = 0;
        pstmt.setString(++i,getIdentification());
        pstmt.setLong(++i,getRequestId());
        
        return pstmt;
    }
}
