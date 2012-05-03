/**
 * $Id$
 */
package com.untangle.node.ips;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.node.SessionEvent;

/**
 * Log event for a blocked request.
 */
@SuppressWarnings("serial")
public class IpsLogEvent extends LogEvent
{
    private SessionEvent sessionEvent;
    private String classification;
    private String message;
    private boolean blocked;
    private IpsRule rule;

    // constructors -----------------------------------------------------------

    public IpsLogEvent() { }

    public IpsLogEvent(SessionEvent sessionEvent, IpsRule rule, String classification, String message, boolean blocked)
    {
        this.sessionEvent = sessionEvent;
        this.rule = rule;
        this.classification = classification;
        this.message = message;
        this.blocked = blocked;
    }

    // accessors --------------------------------------------------------------

    /**
     * the rule that fired this event.
     */
    public IpsRule getRule() { return this.rule; }
    public void setRule( IpsRule rule ) { this.rule = rule; }

    /**
     * Classification of signature that generated this event.
     */
    public String getClassification() { return classification; }
    public void setClassification( String classification ) { this.classification = classification; }

    /**
     * Message of signature that generated this event.
     */
    public String getMessage() { return message; }
    public void setMessage( String message ) { this.message = message; }

    /**
     * Was it blocked.
     */
    public boolean getBlocked() { return blocked; }
    public void setBlocked( boolean blocked ) { this.blocked = blocked; }

    public Long getSessionId() { return sessionEvent.getSessionId(); }
    public void setSessionId( Long sessionId ) { this.sessionEvent.setSessionId(sessionId); }

    public SessionEvent getSessionEvent() { return sessionEvent; }
    public void setSessionEvent(SessionEvent sessionEvent) { this.sessionEvent = sessionEvent; }

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
            "SET " +
            " ips_blocked = '" + getBlocked() + "', " +
            " ips_description = '" + getRule().getDescription() + "' " +
            "WHERE session_id = '" + sessionEvent.getSessionId() + "'";
        return sql;
    }

    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        getSessionEvent().appendSyslog(sb);

        sb.startSection("info");
        sb.addField("snort-id", rule.getSid());
        sb.addField("blocked", blocked);
        sb.addField("message", message);
    }

    public String getSyslogId()
    {
        return "Log";
    }

    public SyslogPriority getSyslogPriority()
    {
        // NOTICE = ips event logged
        // WARNING = traffic altered
        return false == blocked ? SyslogPriority.NOTICE : SyslogPriority.WARNING;
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return "IpsLogEvent id: " + getId() + " Message: " + message;
    }
}
