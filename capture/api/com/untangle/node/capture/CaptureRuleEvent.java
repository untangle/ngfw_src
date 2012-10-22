/**
 * $Id: CaptureRuleEvent.java,v 1.00 2011/12/14 01:02:03 mahotz Exp $
 */

package com.untangle.node.capture;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.SessionEvent;

@SuppressWarnings("serial")
public class CaptureRuleEvent extends LogEvent
{
    private SessionEvent sessionEvent;
    private Integer ruleid = null;
    private boolean blocked = false;

    public CaptureRuleEvent() { }

    public CaptureRuleEvent( SessionEvent sessionEvent, CaptureRule rule )
    {
        this.sessionEvent = sessionEvent;
        this.ruleid = rule.getId();
        this.blocked = rule.getBlock();
    }

    public Integer getRuleId() { return(ruleid); }
    public void setRuleId( Integer ruleid ) { this.ruleid = ruleid; }

    public boolean getBlocked() { return(blocked); }
    public void setBlocked( boolean blocked ) { this.blocked = blocked; }

    public SessionEvent getSessionEvent() { return sessionEvent; }
    public void setSessionEvent( SessionEvent sessionEvent ) { this.sessionEvent = sessionEvent; }

    @Override
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        String sql =
            "UPDATE reports.sessions " +
            "SET ";

        if (ruleid != null)
            sql += " capture_rule_index = ?, ";

        sql += " capture_blocked = ? ";
        sql += " WHERE session_id = ? ";

        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );

        int i=0;

        if (ruleid != null)
            pstmt.setInt(++i, getRuleId());

        pstmt.setBoolean(++i, getBlocked());
        pstmt.setLong(++i, sessionEvent.getSessionId());
        return pstmt;
    }

    public String toString()
    {
        String string = new String();
        SessionEvent pe = getSessionEvent();
        string+=("CaptureRuleEvent(");
        string+=(" clientaddr:" + pe.getCClientAddr());
        string+=(" clientport:" + pe.getCClientPort());
        string+=(" serveraddr:" + pe.getCServerAddr());
        string+=(" serverport:" + pe.getCServerPort());
        string+=(" ruleid:" + ruleid);
        string+=(" blocked:" + blocked);
        string+=(")");
        return string;
    }
}
