/**
 * $Id$
 */
package com.untangle.jnetcap;

import java.net.InetAddress;

import org.apache.log4j.Logger;

@SuppressWarnings("unused") //JNI
public class NetcapTCPSession extends NetcapSession
{
    private static final Logger logger = Logger.getLogger( NetcapTCPSession.class );

    private static final int FLAG_FD                    = 32;

    public NetcapTCPSession( long id )
    {
        super( id, Netcap.IPPROTO_TCP );
    }
    
    public TCPEndpoints clientSide() { return (TCPEndpoints)clientSide; }
    public TCPEndpoints serverSide() { return (TCPEndpoints)serverSide; }

    protected Endpoints makeEndpoints( boolean ifClient ) 
    {
        return new TCPSessionEndpoints( ifClient );
    }
    
    /**
     * Complete the connection to a client, this may throw an exception if
     * the connection is not completed succesfully.</p>
     * @param flags - Flags for the client complete.
     */
    public void clientComplete()
    {
        clientComplete( pointer.value() );
    }

    /**
     * Reset the connection to the client.</p>
     * @param flags - Flags for the client reset.
     */
    public void clientReset()
    {
        clientReset( pointer.value() );
    }

    /**
     * Drop the next few incoming client SYNs
     */
    public void clientDrop()
    {
        clientDrop( pointer.value() );
    }

    /**
     * Send an ICMP message in response to incoming client SYNs
     * This will send the same response that was received by the server.  If an
     * icmp response was not received from the server, then a response is not sent
     * and all incoming SYNs are dropped.
     */
    public void clientSendIcmp()
    {
        clientSendIcmp( pointer.value() );
    }

    /**
     * Send an ICMP Destination unreachable message with the specified code 
     */
    public void clientSendIcmpDestUnreach( byte code )
    {
        clientSendIcmpDestUnreach( pointer.value(), code );
    }

    /**
     * Send whatever rejection type the server sent.
     */
    public void clientForwardReject()
    {
        clientForwardReject( pointer.value() );
    }
    
    /* Deprecated (intf must be specified before completion) (made non-public) */
    /* Complete the connection, every other function should call this after setup */
    private void   serverComplete( )
    {
        serverComplete( pointer.value() );
    }

    /* Connect to the server with new settings on the traffic side */
    public void   serverComplete( InetAddress clientAddress, int clientPort, InetAddress serverAddress, int serverPort, int intf )
    {
        if ( clientAddress == null ) clientAddress = serverSide.client().host();
        if ( clientPort    == 0    ) clientPort    = serverSide.client().port();
        if ( serverAddress == null ) serverAddress = serverSide.server().host();
        if ( serverPort    == 0    ) serverPort    = serverSide.server().port();
        
        if ( setServerEndpoint( pointer.value(), Inet4AddressConverter.toLong( clientAddress ), clientPort, Inet4AddressConverter.toLong( serverAddress ), serverPort, intf ) < 0 ) {
            logger.error( "Unable to modify the server endpoint" + pointer.value());
        }

        /* XXX If the destination is local, then you have to remap the connection, this
         * will be dealt with at a later date */
        
        serverComplete( );
    }
    
    private static native int setServerEndpoint ( long sessionPointer, long clientAddress, int clientPort, long serverAddress, int serverPort, int intf );

    private static native void clientComplete           ( long sessionPointer );
    private static native void clientReset              ( long sessionPointer );
    private static native void clientDrop               ( long sessionPointer );
    private static native void clientSendIcmp           ( long sessionPointer );
    private static native void clientForwardReject      ( long sessionPointer );
    private static native void clientSendIcmpDestUnreach( long sessionPointer, byte code );    
    private static native void serverComplete           ( long sessionPointer );
    private static native void close                    ( long sessionPointer, boolean ifClient );
    
    private static native int read( long sessionPointer, boolean ifClient, byte[] data );
    private static native int write( long sessionPointer, boolean ifClient, byte[] data );

    /**
     * Set the blocking mode for one of the file descriptors.  This will throw an error if it fails.
     */
    private static native void blocking( long sessionPointer, boolean ifClient, boolean mode );

    protected class TCPSessionEndpoints extends SessionEndpoints implements TCPEndpoints
    {
        public TCPSessionEndpoints( boolean ifClientSide )
        {
            super( ifClientSide );
        }
                
        public int fd() { return getIntValue( buildMask( FLAG_FD ), pointer.value()); }
        
        /**
         * Set the blocking mode for one of the file descriptors.  This will throw an if it fails.
         */
        public void blocking( boolean mode )
        {
            NetcapTCPSession.blocking( pointer.value(), ifClientSide, mode );
        }

        public int read( byte[] data )
        {
            return NetcapTCPSession.read( pointer.value(), ifClientSide, data );
        }
        
        public int write( byte[] data )
        {
            return NetcapTCPSession.write( pointer.value(), ifClientSide, data );
        }

        public int write( String data )
        {
            return write( data.getBytes());
        }

        /**
         * Close a file descriptor associated with one of the sides.  This will
         * throw an error if it is unable to close the file desscriptor */
        public void close()
        {
            NetcapTCPSession.close( pointer.value(), ifClientSide );
        }
        
        public int buildMask( int type )
        {
            return ( ifClientSide ? FLAG_IF_CLIENT_MASK : 0 ) | type;
        }
    }

    static 
    {
        Netcap.load();
    }
}
