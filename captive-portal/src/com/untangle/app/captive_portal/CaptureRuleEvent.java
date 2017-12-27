/**
 * $Id$
 */

package com.untangle.app.captive_portal;

import com.untangle.uvm.app.SessionEvent;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * This is the implementation of a Capture Rule Event used when logging capture
 * events to the database.
 * 
 * @author mahotz
 * 
 */

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

// THIS IS FOR ECLIPSE - @formatter:off
    
    public SessionEvent getSessionEvent() { return sessionEvent; }
    public void setSessionEvent(SessionEvent sessionEvent) { this.sessionEvent = sessionEvent; }

    public boolean getCaptured() { return captured; }
    public void setCaptured(boolean captured) { this.captured = captured; }

    public Integer getRuleId() { return ruleid; }
    public void setRuleId(Integer ruleid) { this.ruleid = ruleid; }

// THIS IS FOR ECLIPSE - @formatter:on

    @Override
    public void compileStatements(java.sql.Connection conn, java.util.Map<String, java.sql.PreparedStatement> statementCache) throws Exception
    {
        String sql = "UPDATE " + schemaPrefix() + "sessions" + sessionEvent.getPartitionTablePostfix() + " " + " SET " + " captive_portal_rule_index = ?, " + " captive_portal_blocked = ? " + " WHERE session_id = ? ";

        java.sql.PreparedStatement pstmt = getStatementFromCache(sql, statementCache, conn);

        int i = 0;

        if (ruleid == null) pstmt.setInt(++i, 0);
        else pstmt.setInt(++i, getRuleId());
        pstmt.setBoolean(++i, getCaptured());
        pstmt.setLong(++i, sessionEvent.getSessionId());

        pstmt.addBatch();
        return;
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
        if (getCaptured()) action = I18nUtil.marktr("captured");
        else action = I18nUtil.marktr("passed");

        String summary = "Captive Portal " + action + " " + sessionEvent.toSummaryString();
        return summary;
    }
}
