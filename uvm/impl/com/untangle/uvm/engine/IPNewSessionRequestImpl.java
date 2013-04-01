/**
 * $Id$
 */
package com.untangle.uvm.engine;

import java.net.InetAddress;

import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.netcap.NetcapIPNewSessionRequest;

/**
 * Abstract base class for IP new session request implementations
 */
abstract class IPNewSessionRequestImpl implements IPNewSessionRequest
{

    protected final PipelineConnectorImpl pipelineConnector;

    protected volatile Object attachment = null;

    /**
     * The pipeline request that corresponds to this (node) request.
     *
     */
    protected final NetcapIPNewSessionRequest netcapRequest;

    protected IPNewSessionRequestImpl(Dispatcher disp, NetcapIPNewSessionRequest netcapRequest)
    {
        this.pipelineConnector = disp.pipelineConnector();
        this.netcapRequest = netcapRequest;
    }

    public PipelineConnector pipelineConnector()
    {
        return pipelineConnector;
    }

    public long id()
    {
        return netcapRequest.id();
    }

    public long getSessionId()
    {
        return netcapRequest.id();
    }
    
    public short getProtocol()
    {
        return netcapRequest.getProtocol();
    }

    public InetAddress getClientAddr()
    {
        return netcapRequest.getClientAddr();
    }

    public InetAddress getServerAddr()
    {
        return netcapRequest.getServerAddr();
    }

    public int getClientPort()
    {
        return netcapRequest.getClientPort();
    }

    public int getServerPort()
    {
        return netcapRequest.getServerPort();
    }

    public int getClientIntf()
    {
        return netcapRequest.getClientIntf();
    }

    public int getServerIntf()
    {
        return netcapRequest.getServerIntf();
    }

    public SessionEvent sessionEvent()
    {
        return netcapRequest.sessionEvent();
    }

    public void setClientAddr( InetAddress addr )
    {
        netcapRequest.setClientAddr(addr);
    }

    public void setServerAddr( InetAddress addr )
    {
        netcapRequest.setServerAddr(addr);
    }

    public void setClientPort( int port )
    {
        netcapRequest.setClientPort(port);
    }

    public void setServerPort( int port )
    {
        netcapRequest.setServerPort(port);
    }

    public void rejectSilently()
    {
        netcapRequest.rejectSilently();
    }

    public void endpoint()
    {
        netcapRequest.endpoint();
    }

    public void rejectReturnUnreachable( byte code )
    {
        netcapRequest.rejectReturnUnreachable(code);
    }

    public void release()
    {
        netcapRequest.release();
    }

    public Object attach(Object ob)
    {
        Object oldOb = attachment;
        attachment = ob;
        return oldOb;
    }

    public Object attachment()
    {
        return attachment;
    }

    public Object globalAttach(String key, Object ob)
    {
        return this.netcapRequest.sessionGlobalState().attach(key,ob);
    }

    public Object globalAttachment(String key)
    {
        return this.netcapRequest.sessionGlobalState().attachment(key);
    }
    
    public byte state()
    {
        return netcapRequest.state();
    }

    public InetAddress getNatFromHost()
    {
	return netcapRequest.getNatFromHost();
    }

    public int getNatFromPort()
    {
        return netcapRequest.getNatFromPort();
    }

    public InetAddress getNatToHost()
    {
        return netcapRequest.getNatToHost();
    }

    public int getNatToPort()
    {
        return netcapRequest.getNatToPort();
    }
}
