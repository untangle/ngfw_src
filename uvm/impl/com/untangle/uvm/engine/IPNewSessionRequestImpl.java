/**
 * $Id$
 */
package com.untangle.uvm.engine;

import java.net.InetAddress;

import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.ArgonConnector;
import com.untangle.uvm.argon.ArgonIPNewSessionRequest;

/**
 * Abstract base class for IP new session request implementations
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
abstract class IPNewSessionRequestImpl implements IPNewSessionRequest
{

    protected final ArgonConnectorImpl argonConnector;

    protected volatile Object attachment = null;

    protected boolean needsFinalization = true;
    protected boolean modified = false;

    /**
     * The pipeline request that corresponds to this (node) request.
     *
     */
    protected final ArgonIPNewSessionRequest argonRequest;

    protected IPNewSessionRequestImpl(Dispatcher disp, ArgonIPNewSessionRequest argonRequest)
    {
        this.argonConnector = disp.argonConnector();
        this.argonRequest = argonRequest;
    }

    public ArgonConnector argonConnector()
    {
        return argonConnector;
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

    public void getClientAddr(InetAddress addr)
    {
        argonRequest.getClientAddr(addr);
        modified = true;
    }

    public void getServerAddr(InetAddress addr)
    {
        argonRequest.getServerAddr(addr);
        modified = true;
    }

    public void getClientPort(int port)
    {
        argonRequest.getClientPort(port);
        modified = true;
    }

    public void getServerPort(int port)
    {
        argonRequest.getServerPort(port);
        modified = true;
    }

    public void rejectSilently(boolean needsFinalization)
    {
        argonRequest.rejectSilently();
        this.needsFinalization = needsFinalization;
    }

    public void rejectSilently()
    {
        rejectSilently(false);
    }

    public void endpoint()
    {
        argonRequest.endpoint();
    }

    public void rejectReturnUnreachable(byte code, boolean needsFinalization)
    {
        argonRequest.rejectReturnUnreachable(code);
        this.needsFinalization = needsFinalization;
    }

    public void rejectReturnUnreachable(byte code)
    {
        rejectReturnUnreachable(code, false);
    }

    public void release(boolean needsFinalization)
    {
        this.needsFinalization = needsFinalization;
        argonRequest.release();
    }

    public void release()
    {
        release(false);
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

    public boolean needsFinalization()
    {
        return needsFinalization;
    }

    public boolean modified()
    {
        return modified;
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
