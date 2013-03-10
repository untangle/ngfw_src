/**
 * $Id$
 */
package com.untangle.uvm.node;

import java.net.InetAddress;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.SessionTuple;

/**
 * Used to record the Session endpoints at session end time.
 * SessionStatsEvent and SessionEvent used to be the PiplineInfo
 * object.
 */
@SuppressWarnings("serial")
public class SessionEvent extends LogEvent
{
    private Long sessionId = -1L;
    private Short protocol;
    private Integer clientIntf;
    private Integer serverIntf;
    private InetAddress cClientAddr;
    private InetAddress sClientAddr;
    private InetAddress cServerAddr;
    private InetAddress sServerAddr;
    private Integer cClientPort;
    private Integer sClientPort;
    private Integer cServerPort;
    private Integer sServerPort;
    private Long policyId;
    private String username;
    private String hostname;
    
    public SessionEvent()
    {
        super();
    }

    /**
     * Session id.
     */
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    /**
     * Protocol.  Currently always either 6 (TCP) or 17 (UDP).
     */
    public Short getProtocol() { return protocol; }
    public void setProtocol(Short protocol) { this.protocol = protocol; }

    /**
     * Client interface number (at client).
     */
    public Integer getClientIntf() { return clientIntf; }
    public void setClientIntf(Integer clientIntf) { this.clientIntf = clientIntf; }

    /**
     * Server interface number (at server).
     */
    public Integer getServerIntf() { return serverIntf; }
    public void setServerIntf(Integer serverIntf) { this.serverIntf = serverIntf; }

    /**
     * Client address, at the client side.
     */
    public InetAddress getCClientAddr() { return cClientAddr; }
    public void setCClientAddr(InetAddress cClientAddr) { this.cClientAddr = cClientAddr; }

    /**
     * Client address, at the server side.
     */
    public InetAddress getSClientAddr() { return sClientAddr; }
    public void setSClientAddr(InetAddress sClientAddr) { this.sClientAddr = sClientAddr; }

    /**
     * Server address, at the client side.
     */
    public InetAddress getCServerAddr() { return cServerAddr; }
    public void setCServerAddr(InetAddress cServerAddr) { this.cServerAddr = cServerAddr; }

    /**
     * Server address, at the server side.
     */
    public InetAddress getSServerAddr() { return sServerAddr; }
    public void setSServerAddr(InetAddress sServerAddr) { this.sServerAddr = sServerAddr; }

    /**
     * Client port, at the client side.
     */
    public Integer getCClientPort() { return cClientPort; }
    public void setCClientPort(Integer cClientPort) { this.cClientPort = cClientPort; }

    /**
     * Client port, at the server side.
     */
    public Integer getSClientPort() { return sClientPort; }
    public void setSClientPort(Integer sClientPort) { this.sClientPort = sClientPort; }

    /**
     * Server port, at the client side.
     */
    public Integer getCServerPort() { return cServerPort; }
    public void setCServerPort(Integer cServerPort) { this.cServerPort = cServerPort; }

    /**
     * Server port, at the server side.
     */
    public Integer getSServerPort() { return sServerPort; }
    public void setSServerPort(Integer sServerPort) { this.sServerPort = sServerPort; }

    /**
     * Policy that was applied for this pipeline.
     */
    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long policyId) { this.policyId = policyId; }

    /**
     * The username associated with this session
     */
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    /**
     * The hostname associated with this session
     */
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    
    /* This doesn't really belong here */
    public String getProtocolName()
    {
        switch (protocol) {
        case SessionTuple.PROTO_TCP: return "TCP";
        case SessionTuple.PROTO_UDP: return "UDP";
        default: return "unknown";
        }
    }
    
    private static String sql = "INSERT INTO reports.sessions " +
        "(event_id, session_id, time_stamp, end_time, hostname, username, policy_id, c_client_addr, c_client_port, c_server_addr, c_server_port, s_client_addr, s_client_port, s_server_addr, s_server_port, client_intf, server_intf) " +
        "values " +
        "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?); ";

    @Override
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );

        int i=0;
        pstmt.setLong(++i,getSessionId());
        pstmt.setLong(++i,getSessionId());
        pstmt.setTimestamp(++i,getTimeStamp());
        pstmt.setTimestamp(++i,timeStampPlusMinutes(1)); // default end_time
        pstmt.setString(++i, getHostname());
        pstmt.setString(++i, getUsername());
        pstmt.setLong(++i, (getPolicyId() == null ? 0 : getPolicyId() ));
        pstmt.setObject(++i, getCClientAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, getCClientPort());
        pstmt.setObject(++i, getCServerAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, getCServerPort());
        pstmt.setObject(++i, getSClientAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, getSClientPort());
        pstmt.setObject(++i, getSServerAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, getSServerPort());
        pstmt.setInt(++i, getClientIntf());
        pstmt.setInt(++i, getServerIntf());

        return pstmt;
    }
    
    public boolean equals(Object o)
    {
        if (o instanceof SessionEvent) {
            SessionEvent pe = (SessionEvent)o;
            return getSessionId().equals(pe.getSessionId());
        } else {
            return false;
        }
    }

    public String toString()
    {
        String clientAddr = (getCClientAddr() != null ? getCClientAddr().getHostAddress() : "null");
        String serverAddr = (getSServerAddr() != null ? getSServerAddr().getHostAddress() : "null");
        String clientPort = (getCClientPort() != null ? getCClientPort().toString() : "null");
        String serverPort = (getSServerPort() != null ? getSServerPort().toString() : "null");
        String protocol  = getProtocolName();
        
        return "SessionEvent: [" + protocol + "] " + clientAddr + ":" + clientPort + " -> " + serverAddr + ":" + serverPort;
    }

    public int hashCode()
    {
        return getSessionId().hashCode();
    }
}
