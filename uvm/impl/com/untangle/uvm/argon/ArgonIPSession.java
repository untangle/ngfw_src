/**
 * $Id$
 */
package com.untangle.uvm.argon;

import java.net.InetAddress;

public abstract class ArgonIPSession extends ArgonSession
{
    protected final short protocol;
    protected final InetAddress clientAddr;
    protected final InetAddress serverAddr;
    protected final int clientPort;
    protected final int serverPort;
    protected final int clientIntf;
    protected final int serverIntf;

    public ArgonIPSession( ArgonIPNewSessionRequest request )
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
