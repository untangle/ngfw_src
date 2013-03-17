/**
 * $Id$
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
    
    protected RouterSessionData( InetAddress oClientAddr, int oClientPort, InetAddress mClientAddr, int mClientPort,
                                 InetAddress oServerAddr, int oServerPort, InetAddress mServerAddr, int mServerPort )
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
        return (( originalClientPort != modifiedClientPort )  || !originalClientAddr.equals( modifiedClientAddr ));
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
        return (( originalServerPort != modifiedServerPort )  || !originalServerAddr.equals( modifiedServerAddr ));
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
