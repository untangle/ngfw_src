/**
 * $Id$
 */
package com.untangle.node.firewall;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.SessionEvent;

/**
 * Log event for the firewall.
 */
@SuppressWarnings("serial")
public class FirewallEvent extends LogEvent
{
    private SessionEvent sessionEvent;
    private long    ruleId;
    private boolean wasBlocked;

    // Constructors
    public FirewallEvent() { }

    public FirewallEvent( SessionEvent sessionEvent, boolean wasBlocked, int ruleId )
    {
        this.sessionEvent = sessionEvent;
        this.wasBlocked = wasBlocked;
        this.ruleId  = ruleId;
    }

    /**
     * Whether or not the session was blocked.
     *
     * @return If the session was passed or blocked.
     */
    public boolean getWasBlocked()
    {
        return wasBlocked;
    }

    public void setWasBlocked( boolean wasBlocked )
    {
        this.wasBlocked = wasBlocked;
    }

    /**
     * Rule ID, when this event was triggered.
     *
     * @return current rule ID for the rule that triggered this event.
     */
    public long getRuleId()
    {
        return ruleId;
    }

    public void setRuleId( long ruleId )
    {
        this.ruleId = ruleId;
    }

    public Long getSessionId()
    {
        return sessionEvent.getSessionId();
    }

    public void setSessionId( Long sessionId )
    {
        this.sessionEvent.setSessionId(sessionId);
    }

    public SessionEvent getSessionEvent()
    {
        return sessionEvent;
    }

    public void setSessionEvent(SessionEvent sessionEvent)
    {
        this.sessionEvent = sessionEvent;
    }
    
    @Override
    public String getDirectEventSql()
    {
        String sql =
            "UPDATE reports.sessions " + 
            "SET firewall_was_blocked = '" + getWasBlocked() + "', " +
            "    firewall_rule_index = '" + getRuleId() + "' " + 
            "WHERE session_id = '" + sessionEvent.getSessionId() + "'";
        return sql;
    }
}
