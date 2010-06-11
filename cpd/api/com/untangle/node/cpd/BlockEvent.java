package com.untangle.node.cpd;

import java.io.Serializable;
import java.net.InetAddress;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.node.SessionEndpoints;

/**
 * Block event for the captive portal.  This is for each session that 
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
@Table(name="n_cpd_block_evt", schema="events")
public class BlockEvent extends LogEvent implements Serializable
{

    private short protocol;

    private byte clientIntf;

    private InetAddress clientAddress;
    private InetAddress serverAddress;

    private int clientPort;
    private int serverPort;

    // constructors -----------------------------------------------------------
 // Constructors
    public BlockEvent()
    {
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
     * Protocol.  Currently always either 6 (TCP) or 17 (UDP).
     *
     * @return the id of the session
     */
    @Column(name="proto", nullable=false)
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
    @Column(name="client_intf", nullable=false)
    public byte getClientIntf()
    {
        return clientIntf;
    }

    public void setClientIntf(byte clientIntf)
    {
        this.clientIntf = clientIntf;
    }

    /**
     * Client address, at the client side.
     *
     * @return the address of the client (as seen at client side)
     */
    @Column(name="client_address")
    @Type(type="com.untangle.uvm.type.InetAddressUserType")
    public InetAddress getClientAddress()
    {
        return clientAddress;
    }

    public void setClientAddress(InetAddress newValue)
    {
        this.clientAddress = newValue;
    }

    /**
     * Server address, at the client side.
     *
     * @return the address of the server (as seen at client side)
     */
    @Column(name="server_address")
    @Type(type="com.untangle.uvm.type.InetAddressUserType")
    public InetAddress getServerAddress()
    {
        return serverAddress;
    }

    public void setServerAddress(InetAddress newValue)
    {
        this.serverAddress = newValue;
    }

    /**
     * Client port, at the client side.
     *
     * @return the port of the client (as seen at client side)
     */
    @Column(name="client_port", nullable=false)
    public int getClientPort()
    {
        return clientPort;
    }

    public void setClientPort(int newValue)
    {
        this.clientPort= newValue;
    }

    /**
     * Server port, at the client side.
     *
     * @return the port of the server (as seen at client side of pipeline)
     */
    @Column(name="server_port", nullable=false)
    public int getServerPort()
    {
        return serverPort;
    }

    public void setServerPort(int newValue)
    {
        this.serverPort = newValue;
    }

	// Syslog methods -----------------------------------------------------
	public void appendSyslog(SyslogBuilder sb)
	{
        sb.startSection("endpoints");
        sb.addField("create-date", getTimeStamp());
        sb.addField("protocol", getProtocol());
        sb.addField("client-iface", getClientIntf());
        //Client address, at the client side.
        sb.addField("client-addr", clientAddress);
        //Client port, at the client side.
        sb.addField("client-port", clientPort);
        //Server address, at the client side.
        sb.addField("server-addr", serverAddress);
        //Server port, at the client side.
        sb.addField("server-port", serverPort);
		sb.startSection("info");
	}

	@Transient
	public String getSyslogId()
	{
		return "cpd-block";
	}

	@Transient
	public SyslogPriority getSyslogPriority()
	{
		// INFORMATIONAL = statistics or normal operation
		// WARNING = traffic altered
		return SyslogPriority.INFORMATIONAL;
	}
}
