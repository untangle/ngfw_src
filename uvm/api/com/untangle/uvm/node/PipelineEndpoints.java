/**
 * $Id$
 */
package com.untangle.uvm.node;

import java.net.InetAddress;

import javax.persistence.Id;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.GeneratedValue;

import org.hibernate.annotations.Type;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.policy.Policy;

/**
 * Used to record the Session endpoints at session end time.
 * PipelineStats and PipelineEndpoints used to be the PiplineInfo
 * object.
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="pl_endp", schema="events")
@SuppressWarnings("serial")
public class PipelineEndpoints extends LogEvent
{
    private Integer sessionId = -1;

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
    
    // constructors -----------------------------------------------------------

    public PipelineEndpoints() { }

    public PipelineEndpoints( IPSessionDesc clientSide, IPSessionDesc serverSide, Policy policy, String username )
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
        this.policy = policy;
    }

    // This one is called by ArgonHook, just to get an object which is
    // filled in later.
    public PipelineEndpoints( IPSessionDesc clientSide, String username )
    {
        super();

        this.sessionId = clientSide.id();
        protocol = clientSide.protocol();
        clientIntf = clientSide.clientIntf();
        this.username = username;

        // Don't fill in anything that can change later.
    }

    // business methods -------------------------------------------------------

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

    /* This doesn't really belong here */
    @Transient
    public String getProtocolName()
    {
        switch (protocol) {
        case SessionEndpoints.PROTO_TCP: return "TCP";
        case SessionEndpoints.PROTO_UDP: return "UDP";
        default: return "unknown";
        }
    }

    // accessors --------------------------------------------------------------

    /**
     * Session id.
     *
     * @return the id of the session
     */
    @Column(name="session_id", nullable=false)
    public Integer getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(Integer sessionId)
    {
        this.sessionId = sessionId;
    }

    /**
     * Protocol.  Currently always either 6 (TCP) or 17 (UDP).
     *
     * @return the id of the session
     */
    @Column(name="proto", nullable=false)
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
    @Column(name="client_intf", nullable=false)
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
    @Column(name="server_intf", nullable=false)
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
    @Column(name="c_client_addr")
    @Type(type="com.untangle.uvm.type.InetAddressUserType")
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
    @Column(name="s_client_addr")
    @Type(type="com.untangle.uvm.type.InetAddressUserType")
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
    @Column(name="c_server_addr")
    @Type(type="com.untangle.uvm.type.InetAddressUserType")
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
    @Column(name="s_server_addr")
    @Type(type="com.untangle.uvm.type.InetAddressUserType")
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
    @Column(name="c_client_port", nullable=false)
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
    @Column(name="s_client_port", nullable=false)
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
    @Column(name="c_server_port", nullable=false)
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
    @Column(name="s_server_port", nullable=false)
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
    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="policy_id")
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
    @Column(name="username")
    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }
    
    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
        sb.startSection("endpoints");
        sb.addField("create-date", getTimeStamp());
        sb.addField("session-id", getSessionId());
        sb.addField("protocol", getProtocolName());

        sb.addField("policy", (( policy == null ) ? "<none>" : policy.getName()));

        //Client address, at the client side.
        sb.addField("client-addr", cClientAddr);
        //Client port, at the client side.
        sb.addField("client-port", cClientPort);
        //Server address, at the client side.
        sb.addField("server-addr", cServerAddr);
        //Server port, at the client side.
        sb.addField("server-port", cServerPort);

        //Client address, at the server side.
        sb.addField("client-addr", sClientAddr);
        //Client port, at the server side.
        sb.addField("client-port", sClientPort);
        //Server address, at the server side.
        sb.addField("server-addr", sServerAddr);
        //Server port, at the server side.
        sb.addField("server-port", sServerPort);
    }

    // reuse default getSyslogId
    // reuse default getSyslogPriority

    // Object methods ---------------------------------------------------------

    public boolean equals(Object o)
    {
        if (o instanceof PipelineEndpoints) {
            PipelineEndpoints pe = (PipelineEndpoints)o;
            return getSessionId().equals(pe.getSessionId());
        } else {
            return false;
        }
    }

    public int hashCode()
    {
        return getSessionId().hashCode();
    }
}
