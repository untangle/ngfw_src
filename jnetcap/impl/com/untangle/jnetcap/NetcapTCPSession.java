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


package com.untangle.jnetcap;

import java.net.InetAddress;

@SuppressWarnings("unused") //JNI
public class NetcapTCPSession extends NetcapSession
{
    private static final int FLAG_FD                    = 32;
    private static final int FLAG_ACKED                 = 33;

    public  static final int NON_LOCAL_BIND             = 1;
    
    //unused private static final int DEFAULT_SERVER_START_FLAGS    = 0;
    private static final int DEFAULT_SERVER_COMPLETE_FLAGS = NON_LOCAL_BIND;
    private static final int DEFAULT_CLIENT_COMPLETE_FLAGS = 0;
    private static final int DEFAULT_RESET_FLAGS           = 0;
    private static final int DEFAULT_LIBERATE_FLAGS        = 0;
    private static final int DEFAULT_DROP_FLAGS            = 0;
    private static final int DEFAULT_SEND_ICMP_FLAGS       = 0;
    private static final int DEFAULT_FORWARD_REJECT_FLAGS  = 0;
    
    public NetcapTCPSession( long id )
    {
        super( id, Netcap.IPPROTO_TCP );
    }
    
    /* Get whether or not the client has already ACKED the session */
    public boolean acked()
    {
        return ( getIntValue( FLAG_ACKED, pointer.value()) == 1 ) ? true : false;
    }

    /* ??? This is a dirty hack to work around the fact that you cannot overwrite *
     * the return value with a subclass, this is fixed in 1.5.0                   *
     * in 1.5, this would just read:
     * public TCPEndpoints clientSide() { return clientSide; }
     * public TCPEndpoints serverSide() { return serverSide; }
     * Perhaps this may be too confusing.
     */
    public TCPEndpoints tcpClientSide() { return (TCPEndpoints)clientSide; }
    public TCPEndpoints tcpServerSide() { return (TCPEndpoints)serverSide; }

    protected Endpoints makeEndpoints( boolean ifClient ) 
    {
        return new TCPSessionEndpoints( ifClient );
    }
    
    /**
     * Complete the connection to a client, this may throw an exception if
     * the connection is not completed succesfully.</p>
     * @param flags - Flags for the client complete.
     */
    public void clientComplete( int flags )
    {
        clientComplete( pointer.value(), flags );
    }

    /**
     * Complete the connection to a client with the default flags.
     * this will throw an exception if the connection is not completed 
     * succesfully.</p>
     * @param flags - Flags for the client complete.
     */
    public void clientComplete()
    {
        clientComplete( DEFAULT_CLIENT_COMPLETE_FLAGS );
    }

    /**
     * Reset the connection to the client.</p>
     * @param flags - Flags for the client reset.
     */
    public void clientReset( int flags )
    {
        clientReset( pointer.value(), flags );
    }

    /**
     * Reset the connection to the client with default flags.
     */
    public void clientReset()
    {
        clientReset( pointer.value(), DEFAULT_RESET_FLAGS );
    }

    /**
     * liberate the connection.
     */
    public void liberate()
    {
        liberate( pointer.value(), DEFAULT_LIBERATE_FLAGS );
    }

    /**
     * Drop the next few incoming client SYNs
     */
    public void clientDrop()
    {
        clientDrop( pointer.value(), DEFAULT_DROP_FLAGS );
    }

    /**
     * Send an ICMP message in response to incoming client SYNs
     * This will send the same response that was received by the server.  If an
     * icmp response was not received from the server, then a response is not sent
     * and all incoming SYNs are dropped.
     */
    public void clientSendIcmp()
    {
        clientSendIcmp( pointer.value(), DEFAULT_SEND_ICMP_FLAGS );
    }

    /**
     * Send an ICMP Destination unreachable message with the specified code 
     */
    public void clientSendIcmpDestUnreach( byte code )
    {
        clientSendIcmpDestUnreach( pointer.value(), DEFAULT_SEND_ICMP_FLAGS, code );
    }

    /**
     * Send whatever rejection type the server sent.
     */
    public void clientForwardReject()
    {
        clientForwardReject( pointer.value(), DEFAULT_FORWARD_REJECT_FLAGS );
    }
    
    /* Deprecated (intf must be specified before completion) (made non-public) */
    /* Complete the connection, every other function should call this after setup */
    private void   serverComplete( int flags )
    {
        serverComplete( pointer.value(), flags );
    }

    /* Deprecated (intf must be specified before completion) (made non-public) */
	private void   serverComplete()
    {
        serverComplete( DEFAULT_SERVER_COMPLETE_FLAGS );
    }

    /* Connect to the server with new settings on the traffic side */
    public void   serverComplete( InetAddress clientAddress, int clientPort,
                                  InetAddress serverAddress, int serverPort, int intf, int flags )
    {
        if ( clientAddress == null ) clientAddress = serverSide.client().host();
        if ( clientPort    == 0    ) clientPort    = serverSide.client().port();
        if ( serverAddress == null ) serverAddress = serverSide.server().host();
        if ( serverPort    == 0    ) serverPort    = serverSide.server().port();
        
        if ( setServerEndpoint( pointer.value(), Inet4AddressConverter.toLong( clientAddress ), clientPort,
                                Inet4AddressConverter.toLong( serverAddress ), serverPort, intf ) < 0 ) {
            Netcap.error( "Unable to modify the server endpoint" + pointer.value());
        }

        /* XXX If the destination is local, then you have to remap the connection, this
         * will be dealt with at a later date */
        
        serverComplete( flags );
    }
    
    public void   serverComplete( InetAddress clientAddress, int clientPort,
                                  InetAddress serverAddress, int serverPort, int intf ) 
    {
        serverComplete( clientAddress, clientPort, serverAddress, serverPort, intf, DEFAULT_SERVER_COMPLETE_FLAGS );
    }
    
    private static native int setServerEndpoint ( long sessionPointer, long clientAddress, int clientPort, long serverAddress, int serverPort, int intf );

    private static native void clientComplete           ( long sessionPointer, int flags );
    private static native void clientReset              ( long sessionPointer, int flags );
    private static native void liberate                 ( long sessionPointer, int flags );
    private static native void clientDrop               ( long sessionPointer, int flags );
    private static native void clientSendIcmp           ( long sessionPointer, int flags );
    private static native void clientForwardReject      ( long sessionPointer, int flags );
    private static native void clientSendIcmpDestUnreach( long sessionPointer, int flags, byte code );    

    private static native void serverComplete   ( long sessionPointer, int flags );

    private static native void close( long sessionPointer, boolean ifClient );
    
    private static native int read( long sessionPointer, boolean ifClient, byte[] data );
    private static native int write( long sessionPointer, boolean ifClient, byte[] data );

    /**
     * Set the blocking flag for one of the file descriptors.  This will throw an
     * error if it is unable to set the flag.
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
         * Set the blocking flag for one of the file descriptors.  This will throw an
         * error if it is unable to set the flag.
         */
        public void blocking( boolean mode ) {
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
