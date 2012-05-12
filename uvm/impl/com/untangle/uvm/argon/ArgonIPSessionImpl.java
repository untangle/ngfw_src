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

package com.untangle.uvm.argon;

import java.net.InetAddress;

public abstract class ArgonIPSessionImpl extends ArgonSessionImpl implements ArgonIPSession 
{
    protected final short protocol;
    protected final InetAddress clientAddr;
    protected final InetAddress serverAddr;
    protected final int clientPort;
    protected final int serverPort;
    protected final int clientIntf;
    protected final int serverIntf;

    public ArgonIPSessionImpl( ArgonIPNewSessionRequest request )
    {
        super( request, request.state() == ArgonIPNewSessionRequest.REQUESTED || request.state() == ArgonIPNewSessionRequest.ENDPOINTED );

        protocol      = request.getProtocol();
        clientAddr    = request.getClientAddr();
        clientPort    = request.getClientPort();
        clientIntf    = request.getClientIntf();

        serverPort    = request.getServerPort();
        serverAddr    = request.getServerAddr();
        serverIntf    = request.getServerIntf();
    }

    /** This should be abstract and reference the sub functions. */
    public short getProtocol()
    {
        return protocol;
    }

    public InetAddress getClientAddr() 
    {
        return clientAddr;
    }
    
    public InetAddress getServerAddr()
    {
        return serverAddr;
    }

    public int getClientPort()
    {
        return clientPort;
    }
    
    public int getServerPort()
    {
        return serverPort;
    }

    public int getClientIntf()
    {     
        return clientIntf;
    }
    
    public int getServerIntf()
    {
        return serverIntf;
    }
    
    /* NodeIPSession */
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
