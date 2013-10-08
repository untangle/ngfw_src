/**
 * $Id: SocketQueueListener.java 34435 2013-04-01 19:49:54Z dmorris $
 */
package com.untangle.jvector;

public interface SocketQueueListener
{
    public void event( IncomingSocketQueue in );

    public void event( OutgoingSocketQueue out );

    /**
     * This isn't really used because a shutdown on an incoming socket queue will
     * send a crumb
     */
    public void shutdownEvent( IncomingSocketQueue in );

    /**
     * This is useful for events that must "go backwards" such as a reset read
     * from a TCPSink
     */
    public void shutdownEvent( OutgoingSocketQueue out );
}
