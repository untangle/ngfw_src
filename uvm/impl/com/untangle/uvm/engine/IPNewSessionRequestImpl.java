/**
 * $Id$
 */
package com.untangle.uvm.engine;

import java.net.InetAddress;

import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.argon.ArgonIPNewSessionRequest;

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
    protected final ArgonIPNewSessionRequest argonRequest;

    protected IPNewSessionRequestImpl(Dispatcher disp, ArgonIPNewSessionRequest argonRequest)
    {
        this.pipelineConnector = disp.pipelineConnector();
        this.argonRequest = argonRequest;
    }

    public PipelineConnector pipelineConnector()
    {
        return pipelineConnector;
    }

    public long id()
    {
        return argonRequest.id();
    }

    public long getSessionId()
    {
        return argonRequest.id();
    }
    
    public short getProtocol()
    {
        return argonRequest.getProtocol();
    }

    public InetAddress getClientAddr()
    {
        return argonRequest.getClientAddr();
    }

    public InetAddress getServerAddr()
    {
        return argonRequest.getServerAddr();
    }

    public int getClientPort()
    {
        return argonRequest.getClientPort();
    }

    public int getServerPort()
    {
        return argonRequest.getServerPort();
    }

    public int getClientIntf()
    {
        return argonRequest.getClientIntf();
    }

    public int getServerIntf()
    {
        return argonRequest.getServerIntf();
    }

    public SessionEvent sessionEvent()
    {
        return argonRequest.sessionEvent();
    }

    public void setClientAddr( InetAddress addr )
    {
        argonRequest.setClientAddr(addr);
    }

    public void setServerAddr( InetAddress addr )
    {
        argonRequest.setServerAddr(addr);
    }

    public void setClientPort( int port )
    {
        argonRequest.setClientPort(port);
    }

    public void setServerPort( int port )
    {
        argonRequest.setServerPort(port);
    }

    public void rejectSilently()
    {
        argonRequest.rejectSilently();
    }

    public void endpoint()
    {
        argonRequest.endpoint();
    }

    public void rejectReturnUnreachable( byte code )
    {
        argonRequest.rejectReturnUnreachable(code);
    }

    public void release()
    {
        argonRequest.release();
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
        return this.argonRequest.sessionGlobalState().attach(key,ob);
    }

    public Object globalAttachment(String key)
    {
        return this.argonRequest.sessionGlobalState().attachment(key);
    }
    
    public byte state()
    {
        return argonRequest.state();
    }

    public InetAddress getNatFromHost()
    {
	return argonRequest.getNatFromHost();
    }

    public int getNatFromPort()
    {
        return argonRequest.getNatFromPort();
    }

    public InetAddress getNatToHost()
    {
        return argonRequest.getNatToHost();
    }

    public int getNatToPort()
    {
        return argonRequest.getNatToPort();
    }
}
