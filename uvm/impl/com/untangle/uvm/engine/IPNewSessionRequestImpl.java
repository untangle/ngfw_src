/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.engine;

import java.net.InetAddress;

import com.untangle.uvm.node.PipelineEndpoints;
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

    public short protocol()
    {
        return argonRequest.protocol();
    }

    public InetAddress clientAddr()
    {
        return argonRequest.clientAddr();
    }

    public InetAddress serverAddr()
    {
        return argonRequest.serverAddr();
    }

    public int clientPort()
    {
        return argonRequest.clientPort();
    }

    public int serverPort()
    {
        return argonRequest.serverPort();
    }

    public int clientIntf()
    {
        return argonRequest.clientIntf();
    }

    public int serverIntf()
    {
        return argonRequest.serverIntf();
    }

    public PipelineEndpoints pipelineEndpoints()
    {
        return argonRequest.pipelineEndpoints();
    }

    public void clientAddr(InetAddress addr)
    {
        argonRequest.clientAddr(addr);
        modified = true;
    }

    public void serverAddr(InetAddress addr)
    {
        argonRequest.serverAddr(addr);
        modified = true;
    }

    public void clientPort(int port)
    {
        argonRequest.clientPort(port);
        modified = true;
    }

    public void serverPort(int port)
    {
        argonRequest.serverPort(port);
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
