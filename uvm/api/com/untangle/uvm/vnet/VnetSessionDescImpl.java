/**
 * $Id: VnetSessionDescImpl.java,v 1.00 2012/01/13 13:12:19 dmorris Exp $
 */
package com.untangle.uvm.vnet;

import java.net.InetAddress;

import org.json.JSONBean;

import com.untangle.uvm.vnet.VnetSessionDesc;

/**
 * Client side Vnet Session Description.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
@JSONBean.Marker
@SuppressWarnings("serial")
public class VnetSessionDescImpl implements VnetSessionDesc
{
    protected long id;

    protected String user;

    protected SessionStats stats;

    protected final byte clientState;
    protected final byte serverState;

    protected final short protocol;

    protected final int clientIntf;
    protected final int serverIntf;

    protected final InetAddress clientAddr;
    protected final InetAddress serverAddr;

    protected final int clientPort;
    protected final int serverPort;

    public VnetSessionDescImpl(long id, short protocol, SessionStats stats,
                               byte clientState, byte serverState,
                               int clientIntf, int serverIntf,
                               InetAddress clientAddr, InetAddress serverAddr,
                               int clientPort, int serverPort)
    {
        this.id = id;
        this.stats = stats;
        this.protocol = protocol;
        this.clientState = clientState;
        this.serverState = serverState;
        this.clientIntf = clientIntf;
        this.serverIntf = serverIntf;
        this.clientAddr = clientAddr;
        this.serverAddr = serverAddr;
        this.clientPort = clientPort;
        this.serverPort = serverPort;
    }

    @JSONBean.Getter
    public long id()
    {
        return id;
    }

    @JSONBean.Getter
    public String user()
    {
        return user;
    }

    @JSONBean.Getter
    public SessionStats stats()
    {
        return stats;
    }

    /**
     * Number of bytes received from the client.
     */
    @JSONBean.Getter
    public long c2tBytes()
    {
        return this.stats.c2tBytes();
    }

    /**
     * Number of bytes transmitted to the server.
     */
    @JSONBean.Getter
    public long t2sBytes()
    {
        return this.stats.t2sBytes();
    }

    /**
     * Number of bytes received from the server.
     */
    @JSONBean.Getter
    public long s2tBytes()
    {
        return this.stats.s2tBytes();
    }
    
    /**
     * Number of bytes transmitted to the client.
     */
    @JSONBean.Getter
    public long t2cBytes()
    {
        return this.stats.t2cBytes();
    }

    /**
     * Number of chunks received from the client.
     */
    @JSONBean.Getter
    public long c2tChunks()
    {
        return this.stats.c2tChunks();
    }

    /**
     * Number of chunks transmitted to the server.
     */
    @JSONBean.Getter
    public long t2sChunks()
    {
        return this.stats.t2sChunks();
    }

    /**
     * Number of chunks received from the server.
     */
    @JSONBean.Getter
    public long s2tChunks()
    {
        return this.stats.s2tChunks();
    }

    /**
     * Number of chunks transmitted to the client.
     */
    @JSONBean.Getter
    public long t2cChunks()
    {
        return this.stats.t2cChunks();
    }


    @JSONBean.Getter()
    public short protocol()
    {
        return protocol;
    }

    @JSONBean.Getter()
    public int clientIntf()
    {
        return clientIntf;
    }

    @JSONBean.Getter()
    public int serverIntf()
    {
        return serverIntf;
    }

    @JSONBean.Getter()
    public byte clientState()
    {
        return clientState;
    }

    @JSONBean.Getter()
    public byte serverState()
    {
        return serverState;
    }

    @JSONBean.Getter()
    public InetAddress clientAddr()
    {
        return clientAddr;
    }

    @JSONBean.Getter()
    public InetAddress serverAddr()
    {
        return serverAddr;
    }

    @JSONBean.Getter()
    public int clientPort()
    {
        return clientPort;
    }

    @JSONBean.Getter()
    public int serverPort()
    {
        return serverPort;
    }
}
