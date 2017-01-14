/**
 * $Id$
 */
package com.untangle.node.virus_blocker;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Log for FTP virus events.
 */
@SuppressWarnings("serial")
public class VirusFtpEvent extends LogEvent
{
    private SessionEvent sessionEvent;
    private boolean clean;
    private String virusName;
    private String nodeName;
    private String uri;

    public VirusFtpEvent() { }

    public VirusFtpEvent(SessionEvent pe, boolean clean, String virusName, String nodeName, String uri)
    {
        this.sessionEvent = pe;
        this.nodeName = nodeName;
        this.clean = clean;
        this.virusName = virusName;
        this.uri = uri;
    }

    public SessionEvent getSessionEvent() { return sessionEvent; }
    public void setSessionEvent(SessionEvent sessionEvent) { this.sessionEvent = sessionEvent; }

    public boolean getClean() { return clean; }
    public void setClean(boolean clean) { this.clean = clean; }

    public String getVirusName() { return virusName; }
    public void SetVirusName(String newValue) { this.virusName = newValue; }
    
    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }

    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }
    
    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql = "INSERT INTO " + schemaPrefix() + "ftp_events" + getPartitionTablePostfix() + " " +
            "(time_stamp, session_id, client_intf, server_intf, " + "c_client_addr, c_server_addr, " + 
            "s_client_addr, s_server_addr, policy_id, username, " + 
            " hostname, uri, " + 
            getNodeName().toLowerCase() + "_clean, " + getNodeName().toLowerCase() + "_name "  +  ") values " + 
            "( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i = 0;
        pstmt.setTimestamp(++i, getTimeStamp());
        pstmt.setLong(++i, getSessionEvent().getSessionId());
        pstmt.setInt(++i, getSessionEvent().getClientIntf());
        pstmt.setInt(++i, getSessionEvent().getServerIntf());
        pstmt.setObject(++i, getSessionEvent().getCClientAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setObject(++i, getSessionEvent().getCServerAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setObject(++i, getSessionEvent().getSClientAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setObject(++i, getSessionEvent().getSServerAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setLong(++i, getSessionEvent().getPolicyId());
        pstmt.setString(++i, getSessionEvent().getUsername());
        pstmt.setString(++i, getSessionEvent().getHostname());
        pstmt.setString(++i, uri);
        pstmt.setBoolean(++i,getClean());
        pstmt.setString(++i, getVirusName());

        pstmt.addBatch();
        return;
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
            
        String summary = appName + " " + actionStr + " " + getUri();
        return summary;
    }
    
}
