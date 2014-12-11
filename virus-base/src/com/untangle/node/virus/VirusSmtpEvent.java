/**
 * $Id$
 */
package com.untangle.node.virus;

import java.util.List;
import java.util.LinkedList;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.node.smtp.SmtpMessageEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Log for SMTP Virus events.
 */
@SuppressWarnings("serial")
public class VirusSmtpEvent extends LogEvent
{
    private Long messageId;
    private SmtpMessageEvent messageInfo;
    private boolean clean;
    private String virusName;
    private String action;
    private String nodeName;

    public VirusSmtpEvent() { }

    public VirusSmtpEvent(SmtpMessageEvent messageInfo, boolean clean, String virusName, String action, String nodeName)
    {
        this.messageId = messageInfo.getMessageId();
        this.messageInfo = messageInfo;
        this.clean = clean;
        this.virusName = virusName;
        this.action = action;
        this.nodeName = nodeName;
    }

    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }

    public boolean getClean() { return clean; }
    public void setClean(boolean clean) { this.clean = clean; }

    public String getVirusName() { return virusName; }
    public void SetVirusName(String newValue) { this.virusName = newValue; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }

    @Override
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        return null;
    }

    @Override
    public List<java.sql.PreparedStatement> getDirectEventSqls( java.sql.Connection conn ) throws Exception
    {
        List<java.sql.PreparedStatement> sqlList = new LinkedList<java.sql.PreparedStatement>();
        String sql;
        int i=0;
        java.sql.PreparedStatement pstmt;
        
        sql = "UPDATE reports.mail_msgs " +
            "SET " +
            getNodeName().toLowerCase() + "_clean = ?, " + 
            getNodeName().toLowerCase() + "_name = ? " + 
            "WHERE " +
            "msg_id = ? " ;
        pstmt = conn.prepareStatement( sql );
        i=0;
        pstmt.setBoolean(++i, getClean());
        pstmt.setString(++i, getVirusName());
        pstmt.setLong(++i, getMessageId());
        sqlList.add(pstmt);
        
        sql = "UPDATE reports.mail_addrs " +
            "SET " +
            getNodeName().toLowerCase() + "_clean = ?, " + 
            getNodeName().toLowerCase() + "_name = ? " + 
            "WHERE " +
            "msg_id = ? " ;
        pstmt = conn.prepareStatement( sql );
        i=0;
        pstmt.setBoolean(++i, getClean());
        pstmt.setString(++i, getVirusName());
        pstmt.setLong(++i, getMessageId());
        sqlList.add(pstmt);

        return sqlList;
    }

    @Override
    public String toSummaryString()
    {
        String appName;
        switch ( getNodeName().toLowerCase() ) {
        case "virus_blocker_lite": appName = "Virus Blocker Lite"; break;
        case "virus_blocker": appName = "Virus Blocker"; break;
        default: appName = "Virus Blocker"; break;
        }

        String actionStr;
        if ( getClean() )
            actionStr = I18nUtil.marktr("scanned");
        else
            actionStr = I18nUtil.marktr("found virus") + " [" + getVirusName() + "]";

        String summary = appName + " " + action + " " + messageInfo.toSummaryString();
        return summary;
    }
    
}
