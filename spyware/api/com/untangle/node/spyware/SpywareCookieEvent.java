/**
 * $Id$
 */
package com.untangle.node.spyware;

import com.untangle.uvm.node.SessionEvent;
import com.untangle.node.http.RequestLine;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;

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

    @Override
    public String getDirectEventSql()
    {
        String sql =
            "UPDATE reports.n_http_events " +
            "SET " +
            "sw_cookie_ident  = " + "'" + getIdentification() + "'" + " " +
            "WHERE " +
            "request_id = " + getRequestId() +
            ";";
        return sql;
    }

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        getSessionEvent().appendSyslog(sb);

        sb.startSection("info");
        sb.addField("info", getIdentification());
        sb.addField("blocked", getBlocked());
    }

    // use SpywareEvent getSyslogId and getSyslogPriority
}
