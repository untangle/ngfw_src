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
import com.untangle.uvm.vnet.MPipe;

/**
 * Abstract base class for IP new session request implementations
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
abstract class IPNewSessionRequestImpl implements IPNewSessionRequest {

    protected final MPipeImpl mPipe;

    protected volatile Object attachment = null;

    protected boolean needsFinalization = true;
    protected boolean modified = false;

    /**
     * The pipeline request that corresponds to this (node) request.
     *
     */
    protected final com.untangle.uvm.argon.IPNewSessionRequest pRequest;

    protected IPNewSessionRequestImpl(Dispatcher disp, com.untangle.uvm.argon.IPNewSessionRequest pRequest)
    {
        this.mPipe = disp.mPipe();
        this.pRequest = pRequest;
    }

    public MPipe mPipe()
    {
        return mPipe;
    }

    public int id()
    {
        return pRequest.id();
    }

    public short protocol()
    {
        return pRequest.protocol();
    }

    public InetAddress clientAddr()
    {
        return pRequest.clientAddr();
    }

    public InetAddress serverAddr()
    {
        return pRequest.serverAddr();
    }

    public int clientPort()
    {
        return pRequest.clientPort();
    }

    public int serverPort()
    {
        return pRequest.serverPort();
    }

    public byte clientIntf()
    {
        return pRequest.clientIntf();
    }

    public byte serverIntf()
    {
        return pRequest.serverIntf();
    }

    public PipelineEndpoints pipelineEndpoints()
    {
        return pRequest.pipelineEndpoints();
    }

    public void clientAddr(InetAddress addr)
    {
        pRequest.clientAddr(addr);
        modified = true;
    }

    public void serverAddr(InetAddress addr)
    {
        pRequest.serverAddr(addr);
        modified = true;
    }

    public void clientPort(int port)
    {
        pRequest.clientPort(port);
        modified = true;
    }

    public void serverPort(int port)
    {
        pRequest.serverPort(port);
        modified = true;
    }

    public void rejectSilently(boolean needsFinalization)
    {
        pRequest.rejectSilently();
        this.needsFinalization = needsFinalization;
    }

    public void rejectSilently()
    {
        rejectSilently(false);
    }

    public void endpoint()
    {
        pRequest.endpoint();
    }

    public void rejectReturnUnreachable(byte code, boolean needsFinalization)
    {
        pRequest.rejectReturnUnreachable(code);
        this.needsFinalization = needsFinalization;
    }

    public void rejectReturnUnreachable(byte code)
    {
        rejectReturnUnreachable(code, false);
    }

    public void release(boolean needsFinalization)
    {
        this.needsFinalization = needsFinalization;
        pRequest.release();
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

    public byte state()
    {
        return pRequest.state();
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
	return pRequest.getNatFromHost();
    }

    public int getNatFromPort()
    {
        return pRequest.getNatFromPort();
    }

    public InetAddress getNatToHost()
    {
        return pRequest.getNatToHost();
    }

    public int getNatToPort()
    {
        return pRequest.getNatToPort();
    }
}
