/**
 * $Id$
 */
package com.untangle.node.ips;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.node.SessionEvent;

/**
 * Log event for a blocked request.
 *
 * @author <a href="mailto:nchilders@untangle.com">Nick Childers</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="n_ips_evt", schema="events")
@SuppressWarnings("serial")
public class IpsLogEvent extends LogEvent
{
    private SessionEvent sessionEvent;
    private String classification;
    private String message;
    private boolean blocked;
    private int ruleSid;

    // constructors -----------------------------------------------------------

    public IpsLogEvent() { }

    public IpsLogEvent(SessionEvent sessionEvent, int ruleSid, String classification, String message, boolean blocked)
    {
        this.sessionEvent = sessionEvent;
        this.ruleSid = ruleSid;
        this.classification = classification;
        this.message = message;
        this.blocked = blocked;
    }

    // accessors --------------------------------------------------------------

    /**
     * SID of the rule that fired.
     */
    @Column(name="rule_sid", nullable=false)
    public int getRuleSid()
    {
        return this.ruleSid;
    }

    public void setRuleSid(int ruleSid)
    {
        this.ruleSid = ruleSid;
    }

    /**
     * Classification of signature that generated this event.
     *
     * @return the classification
     */
    public String getClassification()
    {
        return classification;
    }

    public void setClassification(String classification)
    {
        this.classification = classification;
    }

    /**
     * Message of signature that generated this event.
     *
     * @return the message
     */
    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    /**
     * Was it blocked.
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
        sb.addField("snort-id", ruleSid);
        sb.addField("blocked", blocked);
        sb.addField("message", message);
    }

    @Transient
    public String getSyslogId()
    {
        return "Log";
    }

    @Transient
    public SyslogPriority getSyslogPriority()
    {
        // NOTICE = ips event logged
        // WARNING = traffic altered
        return false == blocked ? SyslogPriority.NOTICE : SyslogPriority.WARNING;
    }

    // Object methods ---------------------------------------------------------

    public String toString() {
        return "IpsLogEvent id: " + getId() + " Message: " + message;
    }
}
