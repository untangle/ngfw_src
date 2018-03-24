/**
 * $Id$
 */
package com.untangle.app.bandwidth_control;

import java.io.Serializable;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.app.SessionEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Prioritize event for the bandwidth control.  
 */
@SuppressWarnings("serial")
public class PrioritizeEvent extends LogEvent implements Serializable
{
    private SessionEvent sessionEvent;
    private int priority;
    private int ruleId;

    public PrioritizeEvent() { }

    public PrioritizeEvent(SessionEvent sessionEvent, int priority, int ruleId)
    {
        this.sessionEvent = sessionEvent;
        this.priority = priority;
        this.ruleId = ruleId;
    }
    
    public int getPriority() { return priority; }
    public void setPriority( int newValue ) { this.priority = newValue; }

    public int getRuleId() { return ruleId; }
    public void setRuleId( int newValue ) { this.ruleId = newValue; }
    
    public SessionEvent getSessionEvent() { return sessionEvent; }
    public void setSessionEvent( SessionEvent newValue ) { this.sessionEvent = newValue; }

    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql =
            "UPDATE " + schemaPrefix() + "sessions" + sessionEvent.getPartitionTablePostfix() + " " +
            "SET bandwidth_control_priority = ?, " +
            "    bandwidth_control_rule = ? " + 
            "WHERE session_id = ? ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i=0;
        pstmt.setInt(++i, getPriority());
        pstmt.setInt(++i, getRuleId());
        pstmt.setLong(++i, sessionEvent.getSessionId());

        pstmt.addBatch();
        return;
    }

    @Override
    public String toSummaryString()
    {
        String priorityStr;
        switch ( getPriority() ) {
        case 1: priorityStr = "Very High"; break;
        case 2: priorityStr = "High"; break;
        case 3: priorityStr = "Medium"; break;
        case 4: priorityStr = "Low"; break;
        case 5: priorityStr = "Limited"; break;
        case 6: priorityStr = "Limited More"; break;
        case 7: priorityStr = "Limited Severely"; break;
        default: priorityStr = "unknown"; break;
        }

        String summary = "Bandwidth Control " + I18nUtil.marktr("rule") + " " + getRuleId() + " " + I18nUtil.marktr("prioritized") + " " + sessionEvent.toSummaryString() + " " + I18nUtil.marktr("to") + " " + priorityStr;
        return summary;
    }
}
