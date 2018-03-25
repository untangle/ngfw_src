/**
 * $Id$
 */
package com.untangle.app.virus_blocker;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.app.smtp.SmtpMessageEvent;
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
    private String appName;

    public VirusSmtpEvent() { }

    public VirusSmtpEvent(SmtpMessageEvent messageInfo, boolean clean, String virusName, String action, String appName)
    {
        this.messageId = messageInfo.getMessageId();
        this.messageInfo = messageInfo;
        this.clean = clean;
        this.virusName = virusName;
        this.action = action;
        this.appName = appName;
    }

    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }

    public boolean getClean() { return clean; }
    public void setClean(boolean clean) { this.clean = clean; }

    public String getVirusName() { return virusName; }
    public void SetVirusName(String newValue) { this.virusName = newValue; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }

    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql;
        int i=0;
        java.sql.PreparedStatement pstmt;
        
        sql = "UPDATE " + schemaPrefix() + "mail_msgs" + messageInfo.getPartitionTablePostfix() + " " +
            "SET " +
            getAppName().toLowerCase() + "_clean = ?, " + 
            getAppName().toLowerCase() + "_name = ? " + 
            "WHERE " +
            "msg_id = ? " ;
        pstmt = getStatementFromCache( sql, statementCache, conn );        
        i=0;
        pstmt.setBoolean(++i, getClean());
        pstmt.setString(++i, getVirusName());
        pstmt.setLong(++i, getMessageId());
        pstmt.addBatch();
        
        sql = "UPDATE " + schemaPrefix() + "mail_addrs" + messageInfo.getPartitionTablePostfix() + " " +
            "SET " +
            getAppName().toLowerCase() + "_clean = ?, " + 
            getAppName().toLowerCase() + "_name = ? " + 
            "WHERE " +
            "msg_id = ? " ;
        pstmt = getStatementFromCache( sql, statementCache, conn );        
        i=0;
        pstmt.setBoolean(++i, getClean());
        pstmt.setString(++i, getVirusName());
        pstmt.setLong(++i, getMessageId());
        pstmt.addBatch();

        return;
    }

    @Override
    public String toSummaryString()
    {
        String appName;
        switch ( getAppName().toLowerCase() ) {
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
