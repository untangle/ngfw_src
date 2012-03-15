/**
 * $Id$
 */
package com.untangle.node.protofilter;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.node.SessionEvent;

/**
 * Log event for a proto filter match.
 */
@SuppressWarnings("serial")
public class ProtoFilterLogEvent extends LogEvent
{
    private SessionEvent sessionEvent;
    private String protocol;
    private boolean blocked;

    // constructors -----------------------------------------------------------

    public ProtoFilterLogEvent() { }

    public ProtoFilterLogEvent(SessionEvent sessionEvent, String protocol, boolean blocked)
    {
        this.sessionEvent = sessionEvent;
        this.protocol = protocol;
        this.blocked = blocked;
    }

    // accessors --------------------------------------------------------------

    /**
     * The protocol, as determined by the protocol filter.
     *
     * @return the protocol name.
     */
    public String getProtocol()
    {
        return protocol;
    }

    public void setProtocol(String protocol)
    {
        this.protocol = protocol;
    }

    /**
     * Whether or not we blocked it.
     *
     * @return whether or not the session was blocked (closed)
     */
    public boolean getBlocked()
    {
        return blocked;
    }

    public void setBlocked(boolean blocked)
    {
        this.blocked = blocked;
    }

    public Long getSessionId()
    {
        return sessionEvent.getSessionId();
    }

    public void setSessionId( Long sessionId )
    {
        this.sessionEvent.setSessionId(sessionId);
    }

    @Override
    public boolean isDirectEvent()
    {
        return true;
    }

    @Override
    public String getDirectEventSql()
    {
        String sql =
            "UPDATE reports.sessions " + 
            "SET pf_protocol = '" + getProtocol() + "', " +
            "    pf_blocked = '" + getBlocked() + "' " + 
            "WHERE session_id = '" + sessionEvent.getSessionId() + "'";
        return sql;
    }

    public void appendSyslog(SyslogBuilder sb)
    {
        sessionEvent.appendSyslog(sb);

        sb.startSection("info");
        sb.addField("protocol", getProtocol());
        sb.addField("blocked", getBlocked());
    }

    public String getSyslogId()
    {
        return ""; // XXX
    }

    public SyslogPriority getSyslogPriority()
    {
        // WARNING = traffic altered
        // INFORMATIONAL = statistics or normal operation
        return true == getBlocked() ? SyslogPriority.WARNING : SyslogPriority.INFORMATIONAL;
    }
}
