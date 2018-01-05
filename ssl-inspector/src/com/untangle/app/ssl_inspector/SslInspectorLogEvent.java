/**
 * $Id: SslInspectorLogEvent.java 40254 2015-05-09 03:59:14Z dmorris $
 */

package com.untangle.app.ssl_inspector;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.app.SessionEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Class to manage the details for our log events
 * 
 * @author mahotz
 * 
 */
@SuppressWarnings("serial")
public class SslInspectorLogEvent extends LogEvent
{
    private SessionEvent sessionEvent;
    private Integer ruleid;
    private String status;
    private String detail;

    public SslInspectorLogEvent()
    {
    }

    public SslInspectorLogEvent(SessionEvent sessionEvent, Integer ruleid, String status, String detail)
    {
        this.sessionEvent = sessionEvent;
        this.ruleid = ruleid;
        this.status = status;
        this.detail = detail;
    }

    // THIS IS FOR ECLIPSE - @formatter:off

    public SessionEvent getSessionEvent() { return sessionEvent; }
    public void setSessionEvent(SessionEvent sessionEvent) { this.sessionEvent = sessionEvent; }

    public Integer getRuleId() { return ruleid; }
    public void setRuleId(Integer ruleid) { this.ruleid = ruleid; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    // THIS IS FOR ECLIPSE - @formatter:on

    @Override
    public void compileStatements(java.sql.Connection conn, java.util.Map<String, java.sql.PreparedStatement> statementCache) throws Exception
    {
        String sql = "UPDATE " + schemaPrefix() + "sessions" + sessionEvent.getPartitionTablePostfix() + " " + "SET ";
        if (ruleid != null) sql += " ssl_inspector_ruleid = ?, ";
        sql += " ssl_inspector_status = ?, ";
        sql += " ssl_inspector_detail = ? ";
        sql += " WHERE session_id = ? ";

        java.sql.PreparedStatement pstmt = getStatementFromCache(sql, statementCache, conn);

        int i = 0;
        if (ruleid != null) pstmt.setInt(++i, getRuleId());
        pstmt.setString(++i, getStatus());
        pstmt.setString(++i, getDetail());
        pstmt.setLong(++i, sessionEvent.getSessionId());

        pstmt.addBatch();
        return;
    }

    public String toString()
    {
        String string = new String();
        string += ("SslInspectorLogEvent(");
        string += (" clientaddr:" + sessionEvent.getCClientAddr());
        string += (" clientport:" + sessionEvent.getCClientPort());
        string += (" serveraddr:" + sessionEvent.getSServerAddr());
        string += (" serverport:" + sessionEvent.getSServerPort());
        string += (" ruleid:" + ruleid);
        string += (" status:" + status);
        string += (" detail:" + detail);
        string += (")");
        return string;
    }

    @Override
    public String toSummaryString()
    {
        String action;
        switch (getStatus())
        {
        case "INSPECTED":
            action = I18nUtil.marktr("inspected");
            break;
        case "IGNORED":
            action = I18nUtil.marktr("ignored");
            break;
        case "BLOCKED":
            action = I18nUtil.marktr("blocked");
            break;
        case "UNTRUSTED":
            action = I18nUtil.marktr("disallowed (untrusted)");
            break;
        case "ABANDONED":
            action = I18nUtil.marktr("abandoned");
            break;
        default:
            action = I18nUtil.marktr("unknown");
            break;
        }

        String summary = "SSL Inspector " + action + " " + sessionEvent.toSummaryString();
        return summary;
    }
}
