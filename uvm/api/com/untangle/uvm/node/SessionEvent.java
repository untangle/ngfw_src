/**
 * $Id$
 */
package com.untangle.uvm.node;

import java.net.InetAddress;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.policy.Policy;

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
    private Policy policy;
    private String username;
    private String hostname;
    

    public SessionEvent() { }

    public SessionEvent( IPSessionDesc clientSide, IPSessionDesc serverSide, Policy policy, String username, String hostname )
    {
        super();
        
        this.sessionId = clientSide.id();
        protocol  = clientSide.protocol();

        cClientAddr = clientSide.clientAddr();
        cClientPort = clientSide.clientPort();
        cServerAddr = clientSide.serverAddr();
        cServerPort = clientSide.serverPort();

        sClientAddr = serverSide.clientAddr();
        sClientPort = serverSide.clientPort();
        sServerAddr = serverSide.serverAddr();
        sServerPort = serverSide.serverPort();

        clientIntf = clientSide.clientIntf();
        serverIntf = serverSide.serverIntf();

        this.username = username;
        this.hostname = hostname;
        this.policy = policy;
    }

    /**
     *  This constructor is called by ArgonHook, just to get an object which is
     * filled in later.
     */
    public SessionEvent( IPSessionDesc clientSide, String username, String hostname )
    {
        super();
        this.sessionId = clientSide.id();
        protocol = clientSide.protocol();
        clientIntf = clientSide.clientIntf();
        this.username = username;
        this.hostname = hostname;
    }

    public void completeEndpoints( IPSessionDesc clientSide, IPSessionDesc serverSide, Policy policy )
    {
        cClientAddr = clientSide.clientAddr();
        cClientPort = clientSide.clientPort();
        cServerAddr = clientSide.serverAddr();
        cServerPort = clientSide.serverPort();
        sClientAddr = serverSide.clientAddr();
        sClientPort = serverSide.clientPort();
        sServerAddr = serverSide.serverAddr();
        sServerPort = serverSide.serverPort();
        serverIntf = serverSide.serverIntf();
        this.policy = policy;
    }

    /**
     * Session id.
     *
     * @return the id of the session
     */
    public Long getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(Long sessionId)
    {
        this.sessionId = sessionId;
    }

    /**
     * Protocol.  Currently always either 6 (TCP) or 17 (UDP).
     *
     * @return the id of the session
     */
    public Short getProtocol()
    {
        return protocol;
    }

    public void setProtocol(Short protocol)
    {
        this.protocol = protocol;
    }

    /**
     * Client interface number (at client).  (0 outside, 1 inside)
     *
     * @return the number of the interface of the client
     */
    public Integer getClientIntf()
    {
        return clientIntf;
    }

    public void setClientIntf(Integer clientIntf)
    {
        this.clientIntf = clientIntf;
    }

    /**
     * Server interface number (at server).  (0 outside, 1 inside)
     *
     * @return the number of the interface of the server
     */
    public Integer getServerIntf()
    {
        return serverIntf;
    }

    public void setServerIntf(Integer serverIntf)
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
    public Integer getCClientPort()
    {
        return cClientPort;
    }

    public void setCClientPort(Integer cClientPort)
    {
        this.cClientPort = cClientPort;
    }

    /**
     * Client port, at the server side.
     *
     * @return the port of the client (as seen at server side of pipeline)
     */
    public Integer getSClientPort()
    {
        return sClientPort;
    }

    public void setSClientPort(Integer sClientPort)
    {
        this.sClientPort = sClientPort;
    }

    /**
     * Server port, at the client side.
     *
     * @return the port of the server (as seen at client side of pipeline)
     */
    public Integer getCServerPort()
    {
        return cServerPort;
    }

    public void setCServerPort(Integer cServerPort)
    {
        this.cServerPort = cServerPort;
    }

    /**
     * Server port, at the server side.
     *
     * @return the port of the server (as seen at server side of pipeline)
     */
    public Integer getSServerPort()
    {
        return sServerPort;
    }

    public void setSServerPort(Integer sServerPort)
    {
        this.sServerPort = sServerPort;
    }

    /**
     * Policy that was applied for this pipeline.
     *
     * @return Policy for this pipeline
     */
    public Policy getPolicy()
    {
        return policy;
    }

    public void setPolicy(Policy policy)
    {
        this.policy = policy;
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
        case SessionEndpoints.PROTO_TCP: return "TCP";
        case SessionEndpoints.PROTO_UDP: return "UDP";
        default: return "unknown";
        }
    }
    
    @Override
    public boolean isDirectEvent()
    {
        return true;
    }

    @Override
    public String getDirectEventSql()
    {
        String sql = "INSERT INTO reports.sessions " +
            "(event_id, session_id, time_stamp, end_time, hname, uid, policy_id, c_client_addr, c_client_port, c_server_addr, c_server_port, s_client_addr, s_client_port, s_server_addr, s_server_port, client_intf, server_intf) " +
            "values " +
            "( " +
            getSessionId() + "," +
            getSessionId() + "," +
            "timestamp '" + new java.sql.Timestamp(getTimeStamp().getTime()) + "'" + "," +
            "timestamp '" + new java.sql.Timestamp(getTimeStamp().getTime()) + "'" + " + interval '1 days'," +
            "'" + (getHostname() == null ? "" : getHostname()) + "'" + "," + 
            "'" + (getUsername() == null ? "" : getUsername()) + "'" + "," +
            getPolicy().getId() + "," +
            "'" + getCClientAddr().getHostAddress() + "'" + "," +
            getCClientPort() + "," +
            "'" + getCServerAddr().getHostAddress() + "'" + "," +
            getCServerPort() + "," +
            "'" + getSClientAddr().getHostAddress() + "'" + "," +
            getSClientPort() + "," +
            "'" + getSServerAddr().getHostAddress() + "'" + "," +
            getSServerPort() + "," +
            getClientIntf() + "," +
            getServerIntf() + ")" +
            ";";

            return sql;
    }
    
    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        sb.startSection("endpoints");

        sb.addField("create-date", getTimeStamp());
        sb.addField("session-id", getSessionId());
        sb.addField("protocol", getProtocolName());
        sb.addField("policy", (( policy == null ) ? "<none>" : policy.getName()));
        
        sb.addField("client-addr", cClientAddr); //Client address, at the client side.
        sb.addField("client-port", cClientPort); //Client port, at the client side.
        sb.addField("server-addr", sServerAddr); //Server address, at the server side.
        sb.addField("server-port", sServerPort); //Server port, at the server side.
    }

    // reuse default getSyslogId
    // reuse default getSyslogPriority

    // Object methods ---------------------------------------------------------

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
        
        return "[" + protocol + "] " + clientAddr + ":" + clientPort + " -> " + serverAddr + ":" + serverPort;
    }
    public int hashCode()
    {
        return getSessionId().hashCode();
    }
}
