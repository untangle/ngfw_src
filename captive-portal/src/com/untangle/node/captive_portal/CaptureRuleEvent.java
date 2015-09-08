/**
 * $Id$
 */

package com.untangle.node.captive_portal;

import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;

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

    public SessionEvent getSessionEvent() { return sessionEvent; }
    public void setSessionEvent(SessionEvent sessionEvent) { this.sessionEvent = sessionEvent; }

    public boolean getCaptured() { return captured; }
    public void setCaptured(boolean captured) { this.captured = captured; }

    public Integer getRuleId() { return ruleid; }
    public void setRuleId(Integer ruleid) { this.ruleid = ruleid; }

    @Override
    public java.sql.PreparedStatement getDirectEventSql(java.sql.Connection conn) throws Exception
    {
        String sql = "UPDATE reports.sessions" + sessionEvent.getPartitionTablePostfix() + " " +
            " SET " + 
            " captive_portal_rule_index = ?, " + 
            " captive_portal_blocked = ? " + 
            " WHERE session_id = ? ";

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

    @Override
    public String toSummaryString()
    {
        String action;
        if ( getCaptured() )
            action = I18nUtil.marktr("captured");
        else
            action = I18nUtil.marktr("passed");

        String summary = "Captive Portal " + action + " " + sessionEvent.toSummaryString();
        return summary;
    }

}
