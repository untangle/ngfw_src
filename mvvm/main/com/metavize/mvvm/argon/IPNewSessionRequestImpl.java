/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: IPNewSessionRequestImpl.java,v 1.13 2005/02/07 08:37:27 rbscott Exp $
 */

package com.metavize.mvvm.argon;

import java.net.InetAddress;
import java.net.Inet4Address;

import com.metavize.jnetcap.NetcapSession;
import com.metavize.jnetcap.Endpoints;
import com.metavize.jnetcap.Endpoint;

import com.metavize.jvector.IncomingSocketQueue;
import com.metavize.jvector.OutgoingSocketQueue;

import org.apache.log4j.Logger;

public abstract class IPNewSessionRequestImpl extends NewSessionRequestImpl implements IPNewSessionRequest
{
    protected InetAddress clientAddr;
    protected int clientPort;
    protected byte clientIntf;
    
    protected InetAddress serverAddr;
    protected int serverPort;
    protected byte serverIntf;

    protected byte state = REQUESTED;

    /* This is used to distinguish between REJECTED and REJECTED with code */
    protected byte code  = REJECTED;

    /* Debugging */
    private static final Logger logger = Logger.getLogger( IPNewSessionRequestImpl.class );

    /* Two ways to create an IPNewSessionRequest:
     * A. Pass in the netcap session and get the parameters from there.
     */
    public IPNewSessionRequestImpl( SessionGlobalState sessionGlobalState, ArgonAgent agent )
    {
        super( sessionGlobalState, agent );
        
        /* Everything comes from the client side */
        Endpoint client = sessionGlobalState.netcapSession().clientSide().client();
        Endpoint server = sessionGlobalState.netcapSession().clientSide().server();

        /* Get the server and client from the client end of the endpoint from the netcap session */
        clientAddr = client.host();
        clientPort = client.port();
        clientIntf = IntfConverter.toArgon( client.interfaceId());
        
        serverAddr = server.host();
        serverPort = server.port();

        /** Set the interface to unknown */
        /* ??? A little dirty hack to just flip the interfaces
         * How are we going to setup the interface */
        if ( clientIntf == IntfConverter.INSIDE )
            serverIntf = IntfConverter.OUTSIDE;
        else if ( clientIntf == IntfConverter.OUTSIDE )
            serverIntf = IntfConverter.INSIDE;
        else {
            logger.warn( "Unknown client interface: " + clientIntf );
            serverIntf = IntfConverter.UNKNOWN_INTERFACE;
        }
    }


    /* Two ways to create an IPNewSessionRequest:
     * B. Pass in the previous request and get the parameters from there
     */
    public IPNewSessionRequestImpl( IPSession session, ArgonAgent agent )
    {
        super( session.sessionGlobalState(), agent);
  
        /* Get the server and client from the previous request */
        clientAddr = session.clientAddr();
        clientPort = session.clientPort();
        clientIntf = session.clientIntf();

        serverAddr = session.serverAddr();
        serverPort = session.serverPort();
        serverIntf = session.serverIntf();
    }

    public short protocol()
    {
        return sessionGlobalState.protocol();
    }

    public InetAddress clientAddr()
    {
        return clientAddr;
    }
    
    public void clientAddr( InetAddress addr )
    {
        clientAddr = addr;
    }

    public int clientPort()
    {
        return clientPort;
    }

    public void clientPort( int port )
    {
        clientPort = port;
    }

    public byte clientIntf()
    {
        return clientIntf;
    }

    public InetAddress serverAddr()
    {
        return serverAddr;
    }

    public void serverAddr( InetAddress addr )
    {
        serverAddr = addr;
    }

    public int serverPort()
    {
        return serverPort;
    }

    public void serverPort( int port )
    {
        serverPort = port;
    }

    public byte serverIntf()
    {
        return serverIntf;
    }

    public void serverIntf( byte intf )
    {
        IntfConverter.validateArgonIntf( intf );
        serverIntf = intf;
    }

    // One of REQUESTED, REJECTED, RELEASED 
    public byte state()
    {
        return state;
    }
    
    public byte rejectCode()
    {
        return code;
    }
    
    // May only be called before session is established (from UDPNewSessionRequestEvent handler) 
    public void rejectSilently()
    {
        if ( state != REQUESTED ) {
            throw new IllegalStateException( "Unable to reject session that is not in the requested state" );
        }
        
        state = REJECTED_SILENT;
    }
    
    // May only call if the session is in the requested state
    public void endpoint()
    {
        if ( state != REQUESTED ) {
            throw new IllegalStateException( "Unable to reject session that is not in the requested state" );
        }
        
        state = ENDPOINTED;
    }

    // May only be called before session is established (from UDPNewSessionRequestEvent handler) 
    public void rejectReturnUnreachable( byte code )
    {
        if ( state != REQUESTED ) {
            throw new IllegalStateException( "Unable to reject session that is not in the requested state" );
        }

        switch ( code ) {
        case NET_UNREACHABLE:
        case HOST_UNREACHABLE:
        case PROTOCOL_UNREACHABLE:
        case PORT_UNREACHABLE:
        case DEST_HOST_UNKNOWN:
        case PROHIBITED:
            state     = REJECTED;
            this.code = code;
            break;
            
        default:
            throw new IllegalArgumentException( "Invalid code: " + code );
        }
    }

    // May only be called before session is established (from TCPNewSessionRequestEvent handler)
    public void release()
    {
        if ( state != REQUESTED ) {
            throw new IllegalStateException( "Can't release a session that is not in the requested state" );
        }

        state = RELEASED;
    }
}
