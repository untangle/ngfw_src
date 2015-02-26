/**
 * $Id$
 */
package com.untangle.node.ips;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.util.I18nUtil;

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
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        String sql =
            "UPDATE reports.sessions" + sessionEvent.getPartitionTablePostfix() + " " +
            "SET " +
            " ips_blocked = ?, " + 
            " ips_ruleid = ?, " + 
            " ips_description = ? " + 
            "WHERE session_id = ? ";

        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );

        int i=0;
        pstmt.setBoolean(++i, getBlocked());
        pstmt.setInt(++i, getRule().getSid());
        pstmt.setString(++i, getRule().getDescription());
        pstmt.setLong(++i, sessionEvent.getSessionId());

        return pstmt;
    }

    @Override
    public String toSummaryString()
    {
        String action;
        if ( getBlocked() )
            action = I18nUtil.marktr("blocked");
        else
            action = I18nUtil.marktr("detected");
        String summary = "Intrusion Prevention" + " " + action + " \"" + getMessage() + "\" " + sessionEvent.toSummaryString();
        return summary;
    }

}
