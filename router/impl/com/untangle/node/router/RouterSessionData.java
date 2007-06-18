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
package com.untangle.node.router;

import java.net.InetAddress;

import java.util.List;
import java.util.LinkedList;

class RouterSessionData
{
    private final InetAddress originalClientAddr;
    private final int         originalClientPort;

    private final InetAddress modifiedClientAddr;
    private final int         modifiedClientPort;

    private final InetAddress originalServerAddr;
    private final int         originalServerPort;

    private final InetAddress modifiedServerAddr;
    private final int         modifiedServerPort;

    private final List<SessionRedirect> redirectList = new LinkedList<SessionRedirect>();
    
    protected RouterSessionData( InetAddress oClientAddr, int oClientPort, 
                              InetAddress mClientAddr, int mClientPort,
                              InetAddress oServerAddr, int oServerPort,
                              InetAddress mServerAddr, int mServerPort )
    {
        originalClientAddr = oClientAddr;
        originalClientPort = oClientPort;

        modifiedClientAddr = mClientAddr;        
        modifiedClientPort = mClientPort;

        originalServerAddr = oServerAddr;
        originalServerPort = oServerPort;
        
        modifiedServerAddr = mServerAddr;
        modifiedServerPort = mServerPort;
    }

    boolean isClientRedirect()
    {
        return (( originalClientPort != modifiedClientPort )  ||
                !originalClientAddr.equals( modifiedClientAddr ));
    }

    InetAddress originalClientAddr()
    {
        return originalClientAddr;
    }

    int originalClientPort()
    {
        return originalClientPort;
    }

    InetAddress modifiedClientAddr()
    {
        return modifiedClientAddr;
    }

    int modifiedClientPort()
    {
        return modifiedClientPort;
    }


    boolean isServerRedirect()
    {
        return (( originalServerPort != modifiedServerPort )  ||
                !originalServerAddr.equals( modifiedServerAddr ));
    }

    InetAddress originalServerAddr()
    {
        return originalServerAddr;
    }

    int originalServerPort()
    {
        return originalServerPort;
    }

    InetAddress modifiedServerAddr()
    {
        return modifiedServerAddr;
    }

    int modifiedServerPort()
    {
        return modifiedServerPort;
    }
    
    List<SessionRedirect> redirectList()
    {
        return redirectList;
    }
    
    void addRedirect( SessionRedirect sessionRedirect ) {
        redirectList.add( sessionRedirect );
    }

    public String toString()
    {
        return "RouterSessionData| [" + 
            originalClientAddr + ":" + originalClientPort + " -> " + 
            originalServerAddr + ":" + originalServerPort + "] -> [" + 
            modifiedClientAddr + ":" + modifiedClientPort + " -> " + 
            modifiedServerAddr + ":" + modifiedServerPort + "]";
    }
}
