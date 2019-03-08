/**
 * $Id$
 */
package com.untangle.jnetcap;

import java.net.InetAddress;

import org.apache.log4j.Logger;

/**
 * NetcapTCPSession
 */
@SuppressWarnings("unused") //JNI
public class NetcapTCPSession extends NetcapSession
{
    private static final Logger logger = Logger.getLogger( NetcapTCPSession.class );

    private static final int FLAG_SERVER_FD                    = 100;
    private static final int FLAG_CLIENT_FD                    = 101;

    /**
     * NetcapTCPSession is a TCP session instance
     * @param id
     */
    public NetcapTCPSession( long id )
    {
        super( id );
    }
    
    /**
     * clientSide
     * @return the client side TCPEndpoints
     */
    public TCPEndpoints clientSide()
    {
        return (TCPEndpoints)clientSide;
    }

    /**
     * serverSide
     * @return the server side TCPEndpoints
     */
    public TCPEndpoints serverSide()
    {
        return (TCPEndpoints)serverSide;
    }

    /**
     * makeEndpoints makes Endpoints object for this session
     * @param ifClient - true if is a client endpoints, false otherwise
     * @return endpoints
     */
    protected Endpoints makeEndpoints( boolean ifClient ) 
    {
        return new TCPSessionEndpoints( ifClient );
    }
    
    /**
     * Complete the connection to a client, this may throw an exception if
     * the connection is not completed succesfully.
     */
    public void clientComplete()
    {
        clientComplete( pointer.value() );
    }

    /**
     * Reset the connection to the client.
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
     * @param code
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
    
    /**
     * Deprecated (intf must be specified before completion) (made non-public) 
     * Complete the connection, every other function should call this after setup
     */
    private void   serverComplete( )
    {
        serverComplete( pointer.value() );
    }

    /**
     * Connect to the server with new settings on the traffic side
     * @param clientAddress 
     * @param clientPort 
     * @param serverAddress 
     * @param serverPort 
     * @param intf 
     */
    public void   serverComplete( InetAddress clientAddress, int clientPort, InetAddress serverAddress, int serverPort, int intf )
    {
        if ( clientAddress == null ) clientAddress = serverSide.client().host();
        if ( clientPort    == 0    ) clientPort    = serverSide.client().port();
        if ( serverAddress == null ) serverAddress = serverSide.server().host();
        if ( serverPort    == 0    ) serverPort    = serverSide.server().port();
        
        if ( setServerEndpoint( pointer.value(), Inet4AddressConverter.toLong( clientAddress ), clientPort, Inet4AddressConverter.toLong( serverAddress ), serverPort, intf ) < 0 ) {
            logger.error( "Unable to modify the server endpoint" + pointer.value());
        }

        serverComplete( );
    }
    
    /**
     * setServerEndpoint
     * @param sessionPointer 
     * @param clientAddress 
     * @param clientPort 
     * @param serverAddress 
     * @param serverPort 
     * @param intf 
     * @return 0 if success
     */
    private static native int setServerEndpoint ( long sessionPointer, long clientAddress, int clientPort, long serverAddress, int serverPort, int intf );

    /**
     * clientComplete
     * @param sessionPointer 
     */
    private static native void clientComplete           ( long sessionPointer );

    /**
     * clientReset
     * @param sessionPointer 
     */
    private static native void clientReset              ( long sessionPointer );

    /**
     * clientDrop
     * @param sessionPointer 
     */
    private static native void clientDrop               ( long sessionPointer );

    /**
     * clientSendIcmp
     * @param sessionPointer 
     */
    private static native void clientSendIcmp           ( long sessionPointer );

    /**
     * clientForwardReject
     * @param sessionPointer 
     */
    private static native void clientForwardReject      ( long sessionPointer );

    /**
     * clientSendIcmpDestUnreach
     * @param sessionPointer 
     * @param code 
     */
    private static native void clientSendIcmpDestUnreach( long sessionPointer, byte code );    

    /**
     * serverComplete
     * @param sessionPointer 
     */
    private static native void serverComplete           ( long sessionPointer );

    /**
     * close
     * @param sessionPointer 
     * @param ifClient 
     */
    private static native void close                    ( long sessionPointer, boolean ifClient );
    
    /**
     * read
     * @param sessionPointer 
     * @param ifClient 
     * @param data 
     * @return number of bytes
     */
    private static native int read( long sessionPointer, boolean ifClient, byte[] data );

    /**
     * write
     * @param sessionPointer 
     * @param ifClient 
     * @param data 
     * @return number of bytes
     */
    private static native int write( long sessionPointer, boolean ifClient, byte[] data );

    /**
     * Set the blocking mode for one of the file descriptors.  This
     * will throw an error if it fails.
     * @param sessionPointer 
     * @param ifClient 
     * @param mode 
     */
    private static native void blocking( long sessionPointer, boolean ifClient, boolean mode );

    /**
     * TCPSessionEndpoints
     */
    protected class TCPSessionEndpoints extends SessionEndpoints implements TCPEndpoints
    {
        /**
         * TCPSessionEndpoints
         * @param isClientSide - true if for the client side, false if for server side
         */
        public TCPSessionEndpoints( boolean isClientSide )
        {
            super( isClientSide );
        }
                
        /**
         * fd - gets the file descriptor
         * @return the FD
         */
        public int fd()
        {
            if ( isClientSide )
                return getIntValue( FLAG_CLIENT_FD, pointer.value());
            else
                return getIntValue( FLAG_SERVER_FD, pointer.value());
        }
        
        /**
         * Set the blocking mode for one of the file descriptors.
         * This will throw an Exception if it fails.
         * @param mode 
         */
        public void blocking( boolean mode )
        {
            NetcapTCPSession.blocking( pointer.value(), isClientSide, mode );
        }

        /**
         * read
         * @param data
         * @return number of bytes read
         */
        public int read( byte[] data )
        {
            return NetcapTCPSession.read( pointer.value(), isClientSide, data );
        }
        
        /**
         * write
         * @param data
         * @return number of bytes written
         */
        public int write( byte[] data )
        {
            return NetcapTCPSession.write( pointer.value(), isClientSide, data );
        }

        /**
         * write
         * @param data 
         * @return number of bytes written
         */
        public int write( String data )
        {
            return write( data.getBytes());
        }

        /**
         * Close a file descriptor associated with one of the sides.  This will
         * throw an error if it is unable to close the file desscriptor
         */
        public void close()
        {
            NetcapTCPSession.close( pointer.value(), isClientSide );
        }
    }
}
