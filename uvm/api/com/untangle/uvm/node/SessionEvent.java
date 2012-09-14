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
    private long sessionId = -1L;
    private short protocol = 0;
    private int clientIntf = 0;
    private int serverIntf = 0;
    private InetAddress cClientAddr;
    private InetAddress sClientAddr;
    private InetAddress cServerAddr;
    private InetAddress sServerAddr;
    private int cClientPort = 0;
    private int sClientPort = 0;
    private int cServerPort = 0;
    private int sServerPort = 0;
    private Long policyId;
    private String username;
    private String hostname;
    
    public SessionEvent() { }

    /**
     * This constructor is called by ArgonHook
     * The other fields are completed later
     */
    public SessionEvent( long sessionId, SessionTuple clientSide, String username, String hostname )
    {
        super();
        this.sessionId = sessionId;
        protocol = clientSide.getProtocol();
        clientIntf = clientSide.getClientIntf();
        this.username = username;
        this.hostname = hostname;
    }

    public void completeEndpoints( SessionTuple clientSide, SessionTuple serverSide, long policyId )
    {
        cClientAddr = clientSide.getClientAddr();
        cClientPort = clientSide.getClientPort();
        cServerAddr = clientSide.getServerAddr();
        cServerPort = clientSide.getServerPort();
        sClientAddr = serverSide.getClientAddr();
        sClientPort = serverSide.getClientPort();
        sServerAddr = serverSide.getServerAddr();
        sServerPort = serverSide.getServerPort();
        serverIntf = serverSide.getServerIntf();
        this.policyId = policyId;
    }

    /**
     * Session id.
     *
     * @return the id of the session
     */
    public long getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(long sessionId)
    {
        this.sessionId = sessionId;
    }

    /**
     * Protocol.  Currently always either 6 (TCP) or 17 (UDP).
     *
     * @return the id of the session
     */
    public short getProtocol()
    {
        return protocol;
    }

    public void setProtocol(short protocol)
    {
        this.protocol = protocol;
    }

    /**
     * Client interface number (at client).  (0 outside, 1 inside)
     *
     * @return the number of the interface of the client
     */
    public int getClientIntf()
    {
        return clientIntf;
    }

    public void setClientIntf(int clientIntf)
    {
        this.clientIntf = clientIntf;
    }

    /**
     * Server interface number (at server).  (0 outside, 1 inside)
     *
     * @return the number of the interface of the server
     */
    public int getServerIntf()
    {
        return serverIntf;
    }

    public void setServerIntf(int serverIntf)
    {
        this.serverIntf = serverIntf;
    }

    /**
     * Client address, at the client side.
     *
     * @return the address of the client (as seen at client side of pipeline)
     */
    public InetAddress getCClientAddr()
    {
        return cClientAddr;
    }

    public void setCClientAddr(InetAddress cClientAddr)
    {
        this.cClientAddr = cClientAddr;
    }

    /**
     * Client address, at the server side.
     *
     * @return the address of the client (as seen at server side of pipeline)
     */
    public InetAddress getSClientAddr()
    {
        return sClientAddr;
    }

    public void setSClientAddr(InetAddress sClientAddr)
    {
        this.sClientAddr = sClientAddr;
    }

    /**
     * Server address, at the client side.
     *
     * @return the address of the server (as seen at client side of pipeline)
     */
    public InetAddress getCServerAddr()
    {
        return cServerAddr;
    }

    public void setCServerAddr(InetAddress cServerAddr)
    {
        this.cServerAddr = cServerAddr;
    }

    /**
     * Server address, at the server side.
     *
     * @return the address of the server (as seen at server side of pipeline)
     */
    public InetAddress getSServerAddr()
    {
        return sServerAddr;
    }

    public void setSServerAddr(InetAddress sServerAddr)
    {
        this.sServerAddr = sServerAddr;
    }

    /**
     * Client port, at the client side.
     *
     * @return the port of the client (as seen at client side of pipeline)
     */
    public int getCClientPort()
    {
        return cClientPort;
    }

    public void setCClientPort(int cClientPort)
    {
        this.cClientPort = cClientPort;
    }

    /**
     * Client port, at the server side.
     *
     * @return the port of the client (as seen at server side of pipeline)
     */
    public int getSClientPort()
    {
        return sClientPort;
    }

    public void setSClientPort(int sClientPort)
    {
        this.sClientPort = sClientPort;
    }

    /**
     * Server port, at the client side.
     *
     * @return the port of the server (as seen at client side of pipeline)
     */
    public int getCServerPort()
    {
        return cServerPort;
    }

    public void setCServerPort(int cServerPort)
    {
        this.cServerPort = cServerPort;
    }

    /**
     * Server port, at the server side.
     *
     * @return the port of the server (as seen at server side of pipeline)
     */
    public int getSServerPort()
    {
        return sServerPort;
    }

    public void setSServerPort(int sServerPort)
    {
        this.sServerPort = sServerPort;
    }

    /**
     * Policy that was applied for this pipeline.
     *
     * @return Policy for this pipeline
     */
    public Long getPolicyId()
    {
        return policyId;
    }

    public void setPolicyId(Long policyId)
    {
        this.policyId = policyId;
    }

    /**
     * The username associated with this session
     *
     * @return Username string for this session
     */
    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    /**
     * The hostname associated with this session
     *
     * @return Hostname string for this session
     */
    public String getHostname()
    {
        return hostname;
    }

    public void setHostname(String hostname)
    {
        this.hostname = hostname;
    }
    
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
        "(event_id, session_id, time_stamp, end_time, hname, uid, policy_id, c_client_addr, c_client_port, c_server_addr, c_server_port, s_client_addr, s_client_port, s_server_addr, s_server_port, client_intf, server_intf) " +
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
        //pstmt.setTimestamp(++i,timeStampPlusHours(24));
        pstmt.setTimestamp(++i,timeStampPlusMinutes(1));
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
            return getSessionId() == pe.getSessionId();
        } else {
            return false;
        }
    }

    public String toString()
    {
        String clientAddr = (getCClientAddr() != null ? getCClientAddr().getHostAddress() : "null");
        String serverAddr = (getSServerAddr() != null ? getSServerAddr().getHostAddress() : "null");
        String protocol  = getProtocolName();
        
        return "SessionEvent: [" + protocol + "] " + clientAddr + ":" + getCClientPort() + " -> " + serverAddr + ":" + getSServerPort();
    }
}
