/**
 * $Id: ApplicationControlLogEvent.java 40254 2015-05-09 03:59:14Z dmorris $
 */
package com.untangle.node.application_control;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.util.I18nUtil;

@SuppressWarnings("serial")
public class ApplicationControlLogEvent extends LogEvent
{
    private SessionEvent sessionEvent;
    private String application = null;
    private String protochain = null;
    private String detail = null;
    private Integer confidence = null;
    private Integer state = null;
    private Integer ruleid = null;
    private boolean flagged = false;
    private boolean blocked = false;

    public ApplicationControlLogEvent()
    {
    }

    public ApplicationControlLogEvent(SessionEvent sessionEvent, ApplicationControlStatus status, ApplicationControlProtoRule rule)
    {
        this.sessionEvent = sessionEvent;
        this.application = status.application;
        this.protochain = status.protochain;
        this.detail = status.detail;
        this.confidence = status.confidence;
        this.state = status.state;
        this.ruleid = null;
        this.flagged = rule.getFlag();
        if (rule.getBlock() || rule.getTarpit())
            this.blocked = true;
        else
            this.blocked = false;
    }

    public ApplicationControlLogEvent(SessionEvent sessionEvent, ApplicationControlStatus status, Integer ruleid, boolean flagged, boolean blocked)
    {
        this.sessionEvent = sessionEvent;
        this.application = status.application;
        this.protochain = status.protochain;
        this.detail = status.detail;
        this.confidence = status.confidence;
        this.state = status.state;
        this.ruleid = ruleid;
        this.flagged = flagged;
        this.blocked = blocked;
    }

    public String getApplication() { return application; }
    public void setApplication(String application) { this.application = application; }

    public String getProtochain() { return protochain; }
    public void setProtochain(String protochain) { this.protochain = protochain; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public Integer getConfidence() { return confidence; }
    public void setConfidence(Integer confidence) { this.confidence = confidence; }

    public Integer getState() { return state; }
    public void setState(Integer state) { this.state = state; }

    public Integer getRuleId() { return ruleid; }
    public void setRuleId(Integer ruleid) { this.ruleid = ruleid; }

    public boolean getFlagged() { return flagged; }
    public void setFlagged(boolean flagged) { this.flagged = flagged; }

    public boolean getBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    public SessionEvent getSessionEvent() { return sessionEvent; }
    public void setSessionEvent(SessionEvent sessionEvent) { this.sessionEvent = sessionEvent; }

    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql = "UPDATE reports.sessions" + sessionEvent.getPartitionTablePostfix() + " " + "SET ";
        if (application != null)
            sql += " application_control_application = ?, ";
        if (detail != null)
            sql += " application_control_detail = ?, ";
        if (protochain != null)
            sql += " application_control_protochain = ?, ";
        if (confidence != null)
            sql += " application_control_confidence = ?, ";
        if (ruleid != null)
            sql += " application_control_ruleid = ?, ";
        sql += " application_control_flagged = ?, ";
        sql += " application_control_blocked = ? ";
        sql += " WHERE session_id = ? ";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i = 0;
        if (application != null)
            pstmt.setString(++i, getApplication());
        if (detail != null)
            pstmt.setString(++i, getDetail());
        if (protochain != null)
            pstmt.setString(++i, getProtochain());
        if (confidence != null)
            pstmt.setInt(++i, getConfidence());
        if (ruleid != null)
            pstmt.setInt(++i, getRuleId());
        pstmt.setBoolean(++i, getFlagged());
        pstmt.setBoolean(++i, getBlocked());
        pstmt.setLong(++i, sessionEvent.getSessionId());

        pstmt.addBatch();
        return;
    }

    public String toString()
    {
        String string = new String();
        SessionEvent pe = getSessionEvent();
        string += ("ApplicationControlLogEvent(");
        string += (" clientaddr:" + pe.getCClientAddr());
        string += (" clientport:" + pe.getCClientPort());
        string += (" serveraddr:" + pe.getCServerAddr());
        string += (" serverport:" + pe.getCServerPort());
        string += (" application:" + application);
        string += (" protochain:" + protochain);
        string += (" detail:" + detail);
        string += (" confidence:" + confidence);
        string += (" state:" + state);
        string += (" ruleid:" + ruleid);
        string += (" flagged:" + flagged);
        string += (" blocked:" + blocked);
        string += (")");
        return string;
    }

    @Override
    public String toSummaryString()
    {
        String summary = "Application Control " + I18nUtil.marktr("identified") + " " + sessionEvent.toSummaryString() + " " + I18nUtil.marktr("as") + getProtochain();
        return summary;
    }
}
