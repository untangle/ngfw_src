/**
 * $Id: CaptureRuleEvent.java,v 1.00 2011/12/14 01:02:03 mahotz Exp $
 */

package com.untangle.node.capture;

import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.logging.LogEvent;

@SuppressWarnings("serial")
public class CaptureRuleEvent extends LogEvent
{
    private SessionEvent sessionEvent = null;
    private Integer ruleid = null;
    private boolean captured = false;

    public CaptureRuleEvent()
    {
    }

    public CaptureRuleEvent(SessionEvent sessionEvent, CaptureRule rule)
    {
        this.sessionEvent = sessionEvent;
        this.ruleid = rule.getId();
        this.captured = rule.getCapture();
    }

    public CaptureRuleEvent(SessionEvent sessionEvent, boolean captured)
    {
        this.sessionEvent = sessionEvent;
        this.captured = captured;
    }

    public SessionEvent getSessionEvent()
    {
        return sessionEvent;
    }

    public void setSessionEvent(SessionEvent sessionEvent)
    {
        this.sessionEvent = sessionEvent;
    }

    public boolean getCaptured()
    {
        return (captured);
    }

    public void setCaptured(boolean captured)
    {
        this.captured = captured;
    }

    public Integer getRuleId()
    {
        return (ruleid);
    }

    public void setRuleId(Integer ruleid)
    {
        this.ruleid = ruleid;
    }

    @Override
    public java.sql.PreparedStatement getDirectEventSql(java.sql.Connection conn) throws Exception
    {
        String sql = "UPDATE reports.sessions SET ";
        sql += " capture_rule_index = ?, ";
        sql += " capture_blocked = ? ";
        sql += " WHERE session_id = ? ";

        java.sql.PreparedStatement pstmt = conn.prepareStatement(sql);

        int i = 0;

        if (ruleid == null)
            pstmt.setInt(++i, 0);
        else
            pstmt.setInt(++i, getRuleId());
        pstmt.setBoolean(++i, getCaptured());
        pstmt.setLong(++i, sessionEvent.getSessionId());
        return pstmt;
    }

    public String toString()
    {
        String string = new String();
        SessionEvent pe = getSessionEvent();
        string += ("CaptureRuleEvent(");
        string += (" clientaddr:" + pe.getCClientAddr());
        string += (" clientport:" + pe.getCClientPort());
        string += (" serveraddr:" + pe.getCServerAddr());
        string += (" serverport:" + pe.getCServerPort());
        string += (" ruleid:" + ruleid);
        string += (" captured:" + captured);
        string += (")");
        return string;
    }
}
