/**
 * $Id$
 */
package com.untangle.node.protofilter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.node.SessionEvent;

/**
 * Log event for a proto filter match.
 *
 * @author
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="n_protofilter_evt", schema="events")
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
    @Column(nullable=false)
    public boolean isBlocked()
    {
        return blocked;
    }

    public void setBlocked(boolean blocked)
    {
        this.blocked = blocked;
    }

    @Column(name="session_id", nullable=false)
    public Long getSessionId()
    {
        return sessionEvent.getSessionId();
    }

    public void setSessionId( Long sessionId )
    {
        this.sessionEvent.setSessionId(sessionId);
    }

    @Transient
    public SessionEvent getSessionEvent()
    {
        return sessionEvent;
    }

    public void setSessionEvent(SessionEvent sessionEvent)
    {
        this.sessionEvent = sessionEvent;
    }

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        getSessionEvent().appendSyslog(sb);

        sb.startSection("info");
        sb.addField("protocol", getProtocol());
        sb.addField("blocked", isBlocked());
    }

    @Transient
    public String getSyslogId()
    {
        return ""; // XXX
    }

    @Transient
    public SyslogPriority getSyslogPriority()
    {
        // WARNING = traffic altered
        // INFORMATIONAL = statistics or normal operation
        return true == isBlocked() ? SyslogPriority.WARNING : SyslogPriority.INFORMATIONAL;
    }
}
