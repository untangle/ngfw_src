/*
 * Copyright (c) 2003 - 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.tapi.impl;

import java.net.InetAddress;
import com.metavize.mvvm.tapi.IPNewSessionRequest;
import com.metavize.mvvm.tapi.MPipe;

abstract class IPNewSessionRequestImpl implements IPNewSessionRequest {

    protected MPipeImpl mPipe;

    protected volatile Object attachment = null;

    /**
     * The pipeline request that corresponds to this (transform) request.
     *
     */
    protected com.metavize.mvvm.argon.IPNewSessionRequest pRequest;

    protected IPNewSessionRequestImpl(Dispatcher disp, com.metavize.mvvm.argon.IPNewSessionRequest pRequest) {
        this.mPipe = disp.mPipe();
        this.pRequest = pRequest;
    }

    public MPipe mPipe() {
        return mPipe;
    }

    public int id() {
        return pRequest.id();
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

    public void clientAddr(InetAddress addr)
    {
        pRequest.clientAddr(addr);
    }

    public void serverAddr(InetAddress addr)
    {
        pRequest.serverAddr(addr);
    }

    public void clientPort(int port)
    {
        pRequest.clientPort(port);
    }

    public void serverPort(int port)
    {
        pRequest.serverPort(port);
    }

    public void serverIntf(byte intf)
    {
        pRequest.serverIntf(intf);
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

    public void release() {
        pRequest.release();
    }

    byte state() {
        return pRequest.state();
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
}
