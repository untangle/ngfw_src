/**
 * $Id$
 */
package com.untangle.node.virus;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.SessionEvent;

/**
 * Log for FTP virus events.
 */
@SuppressWarnings("serial")
public class VirusFtpEvent extends LogEvent
{
    private SessionEvent sessionEvent;
    private VirusScannerResult result;
    private String nodeName;
    private String uri;

    public VirusFtpEvent() { }

    public VirusFtpEvent(SessionEvent pe, VirusScannerResult result, String nodeName, String uri)
    {
        this.sessionEvent = pe;
        this.result = result;
        this.nodeName = nodeName;
        this.uri = uri;
    }

    /**
     * Get the session Id
     *
     * @return the the session Id
     */
    public Long getSessionId()
    {
        return sessionEvent.getSessionId();
    }

    public void setSessionId( Long sessionId )
    {
        this.sessionEvent.setSessionId(sessionId);
    }

    public SessionEvent getSessionEvent()
    {
        return sessionEvent;
    }

    public void setSessionEvent(SessionEvent sessionEvent)
    {
        this.sessionEvent = sessionEvent;
    }

    /**
     * Virus scan result.
     *
     * @return the scan result.
     */
    public VirusScannerResult getResult()
    {
        return result;
    }

    public void setResult(VirusScannerResult result)
    {
        this.result = result;
    }

    /**
     * Spam scanner node.
     *
     * @return the node
     */
    public String getNodeName()
    {
        return nodeName;
    }

    public void setNodeName(String nodeName)
    {
        this.nodeName = nodeName;
    }
    
    
    private static String sql = "INSERT INTO reports.ftp_events " + "(time_stamp, "
            + "session_id, client_intf, server_intf, " + "c_client_addr, c_server_addr, "
            + "s_client_addr, s_server_addr, policy_id, username, "
            + " hostname, uri, ";
    
    private static String sql_end = ") values "
            + "( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    @Override
    public java.sql.PreparedStatement getDirectEventSql(java.sql.Connection conn) throws Exception
    {
        java.sql.PreparedStatement pstmt = conn.prepareStatement(sql + getNodeName().toLowerCase() + "_clean, " + 
                getNodeName().toLowerCase() + "_name "  + sql_end);

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
        pstmt.setBoolean(++i,getResult().isClean());
        pstmt.setString(++i, getResult().getVirusName());

        return pstmt;
    }
}
