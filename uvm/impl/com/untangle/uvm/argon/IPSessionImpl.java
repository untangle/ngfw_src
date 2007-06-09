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

package com.untangle.uvm.argon;

import java.net.InetAddress;

public abstract class IPSessionImpl extends SessionImpl implements IPSession 
{
    protected final short protocol;
    protected final InetAddress clientAddr;
    protected final InetAddress serverAddr;
    protected final int clientPort;
    protected final int serverPort;
    protected final byte clientIntf;
    protected final byte serverIntf;

    public IPSessionImpl( IPNewSessionRequest request )
    {
        super( request, request.state() == IPNewSessionRequest.REQUESTED || request.state() == IPNewSessionRequest.ENDPOINTED );

        protocol      = request.protocol();
        clientAddr    = request.clientAddr();
        clientPort    = request.clientPort();
        clientIntf    = request.clientIntf();

        serverPort    = request.serverPort();
        serverAddr    = request.serverAddr();
        serverIntf    = request.serverIntf();
    }

    /* IPSessionDesc */
    /** This should be abstract and reference the sub functions. */
    public short protocol()
    {
        return protocol;
    }

    public InetAddress clientAddr() 
    {
        return clientAddr;
    }
    
    public InetAddress serverAddr()
    {
        return serverAddr;
    }

    public int clientPort()
    {
        return clientPort;
    }
    
    public int serverPort()
    {
        return serverPort;
    }

    public byte clientIntf()
    {     
        return clientIntf;
    }
    
    public byte serverIntf()
    {
        return serverIntf;
    }
    
    /* IPSession */
    public void release()
    {
        /* Maybe someday */
    }

    public void scheduleTimer( long delay ) throws IllegalArgumentException
    {
        /* XX need some implementation */
    }

    public void cancelTimer()
    {
        /* Possible, unless just using the vectoring */
    }
}
