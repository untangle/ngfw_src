/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.engine;

import java.net.InetAddress;

import com.untangle.mvvm.tran.PipelineEndpoints;
import com.untangle.mvvm.tapi.IPNewSessionRequest;
import com.untangle.mvvm.tapi.MPipe;

abstract class IPNewSessionRequestImpl implements IPNewSessionRequest {

    protected MPipeImpl mPipe;

    protected volatile Object attachment = null;

    protected boolean needsFinalization = true;
    protected boolean modified = false;
    protected boolean isInbound;

    /**
     * The pipeline request that corresponds to this (transform) request.
     *
     */
    protected com.untangle.mvvm.argon.IPNewSessionRequest pRequest;

    protected IPNewSessionRequestImpl(Dispatcher disp,
                                      com.untangle.mvvm.argon.IPNewSessionRequest pRequest,
                                      boolean isInbound) {
        this.mPipe = disp.mPipe();
        this.pRequest = pRequest;
        this.isInbound = isInbound;
    }

    public MPipe mPipe() {
        return mPipe;
    }

    public int id() {
        return pRequest.id();
    }

    public boolean isInbound() {
        return isInbound;
    }

    public boolean isOutbound() {
        return !isInbound;
    }

    public short protocol() {
        return pRequest.protocol();
    }

    public InetAddress clientAddr() {
        return pRequest.clientAddr();
    }

    public InetAddress serverAddr() {
        return pRequest.serverAddr();
    }

    public int clientPort() {
        return pRequest.clientPort();
    }

    public int serverPort() {
        return pRequest.serverPort();
    }

    public byte clientIntf() {
        return pRequest.clientIntf();
    }

    public byte serverIntf() {
        return pRequest.serverIntf();
    }

    public byte originalServerIntf() {
        return pRequest.originalServerIntf();
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

    public void serverIntf(byte intf)
    {
        pRequest.serverIntf(intf);
        modified = true;
    }

    public void rejectSilently()
    {
        pRequest.rejectSilently();
    }

    public void endpoint()
    {
        pRequest.endpoint();
    }

    public void rejectReturnUnreachable(byte code)
    {
        pRequest.rejectReturnUnreachable(code);
    }

    public void release(boolean needsFinalization)
    {
        this.needsFinalization = needsFinalization;
        pRequest.release();
    }

    public void release() {
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

    byte state() {
        return pRequest.state();
    }

    boolean needsFinalization()
    {
        return needsFinalization;
    }

    boolean modified()
    {
        return modified;
    }
}
