/**
 * $Id: SessionNat.java 33103 2012-09-25 23:46:26Z dmorris $
 */
package com.untangle.uvm.node;

import java.net.InetAddress;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.SessionTuple;

/**
 * This event updates the addresses and intf attributes
 * of a session after NAT (or port forward) or some other modification by an application
 * has taken place
 */
@SuppressWarnings("serial")
public class SessionNatEvent extends LogEvent
{
    private Long sessionId = -1L;

    private Integer serverIntf;

    private InetAddress sClientAddr;
    private InetAddress sServerAddr;
    private Integer sClientPort;
    private Integer sServerPort;
    
    public SessionNatEvent( Long sessionId, Integer serverIntf, InetAddress sClientAddr, Integer sClientPort, InetAddress sServerAddr, Integer sServerPort )
    {
        super();
        this.sessionId = sessionId;
        this.serverIntf = serverIntf;
        this.sClientAddr = sClientAddr;
        this.sClientPort = sClientPort;
        this.sServerAddr = sServerAddr;
        this.sServerPort = sServerPort;
    }

    /**
     * Session id.
     */
    public Long getSessionId() { return sessionId; }
    public void setSessionId( Long sessionId ) { this.sessionId = sessionId; }

    /**
     * Server interface index (at server).
     */
    public Integer getServerIntf() { return serverIntf; }
    public void setServerIntf(Integer serverIntf) { this.serverIntf = serverIntf; }

    /**
     * Client address, at the server side.
     */
    public InetAddress getSClientAddr() { return sClientAddr; }
    public void setSClientAddr(InetAddress sClientAddr) { this.sClientAddr = sClientAddr; }

    /**
     * Server address, at the server side.
     */
    public InetAddress getSServerAddr() { return sServerAddr; }
    public void setSServerAddr(InetAddress sServerAddr) { this.sServerAddr = sServerAddr; }

    /**
     * Client port, at the server side.
     */
    public Integer getSClientPort() { return sClientPort; }
    public void setSClientPort(Integer sClientPort) { this.sClientPort = sClientPort; }

    /**
     * Server port, at the server side.
     */
    public Integer getSServerPort() { return sServerPort; }
    public void setSServerPort(Integer sServerPort) { this.sServerPort = sServerPort; }

    private static String sql = "UPDATE reports.sessions " +
        "SET server_intf = ?, " +
        "    s_client_addr = ?, " +
        "    s_client_port = ?, " +
        "    s_server_addr = ?, " +
        "    s_server_port = ? " +
        "WHERE session_id = ? ";
        
    @Override
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );

        int i=0;
        pstmt.setInt(++i, getServerIntf());
        pstmt.setObject(++i, getSClientAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, getSClientPort());
        pstmt.setObject(++i, getSServerAddr().getHostAddress(), java.sql.Types.OTHER);
        pstmt.setInt(++i, getSServerPort());
        pstmt.setLong(++i,getSessionId());

        return pstmt;
    }
    
    public boolean equals(Object o)
    {
        if (o instanceof SessionNatEvent) {
            SessionNatEvent pe = (SessionNatEvent)o;
            return getSessionId().equals(pe.getSessionId());
        } else {
            return false;
        }
    }

    public String toString()
    {
        String clientAddr = (getSClientAddr() != null ? getSClientAddr().getHostAddress() : "null");
        String serverAddr = (getSServerAddr() != null ? getSServerAddr().getHostAddress() : "null");
        String clientPort = (getSClientPort() != null ? getSClientPort().toString() : "null");
        String serverPort = (getSServerPort() != null ? getSServerPort().toString() : "null");
        
        return "SessionNatEvent " + clientAddr + ":" + clientPort + " -> " + serverAddr + ":" + serverPort;
    }

    public int hashCode()
    {
        return getSessionId().hashCode();
    }
}
