/**
 * $Id$
 */
package com.untangle.node.firewall;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.node.SessionEvent;

/**
 * Log event for the firewall.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="n_firewall_evt", schema="events")
@SuppressWarnings("serial")
public class FirewallEvent extends LogEvent implements Serializable
{
    private SessionEvent sessionEvent;
    private int     ruleIndex;
    private long    ruleId;
    private boolean wasBlocked;

    // Constructors
    public FirewallEvent() { }

    public FirewallEvent( SessionEvent sessionEvent, boolean wasBlocked, int ruleIndex )
    {
        this.sessionEvent = sessionEvent;
        this.wasBlocked = wasBlocked;
        this.ruleIndex  = ruleIndex;
    }

    /**
     * Whether or not the session was blocked.
     *
     * @return If the session was passed or blocked.
     */
    @Column(name="was_blocked", nullable=false)
    public boolean getWasBlocked()
    {
        return wasBlocked;
    }

    public void setWasBlocked( boolean wasBlocked )
    {
        this.wasBlocked = wasBlocked;
    }

    /**
     * Rule index, when this event was triggered.
     *
     * @return current rule index for the rule that triggered this event.
     */
    @Column(name="rule_index", nullable=false)
    public int getRuleIndex()
    {
        return ruleIndex;
    }

    public void setRuleIndex( int ruleIndex )
    {
        this.ruleIndex = ruleIndex;
    }

    /**
     * Rule ID, when this event was triggered.
     *
     * @return current rule ID for the rule that triggered this event.
     */
    @Column(name="rule_id", nullable=true)
    public long getRuleId()
    {
        return ruleId;
    }

    public void setRuleId( long ruleId )
    {
        this.ruleId = ruleId;
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
    
    // Syslog methods -----------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        getSessionEvent().appendSyslog(sb);

        sb.startSection("info");
        sb.addField("reason-rule#", getRuleIndex());
        sb.addField("blocked", getWasBlocked());
    }

    @Transient
    public String getSyslogId()
    {
        return ""; // XXX
    }

    @Transient
    public SyslogPriority getSyslogPriority()
    {
        // INFORMATIONAL = statistics or normal operation
        // WARNING = traffic altered
        return false == getWasBlocked() ? SyslogPriority.INFORMATIONAL : SyslogPriority.WARNING;
    }
}
