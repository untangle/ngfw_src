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
package com.untangle.node.nat;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.untangle.uvm.tapi.IPNewSessionRequest;
import com.untangle.uvm.tapi.IPSession;
import com.untangle.uvm.tapi.MPipeException;
import com.untangle.uvm.tapi.Protocol;
import com.untangle.uvm.tapi.TCPSession;
import org.apache.log4j.Logger;

class NatSessionManager
{
    Map<Integer,NatSessionData> map = new ConcurrentHashMap<Integer,NatSessionData>();

    Map<SessionRedirectKey,SessionRedirect> redirectMap =
        new ConcurrentHashMap<SessionRedirectKey,SessionRedirect>();

    private final Logger logger = Logger.getLogger( this.getClass());
    NatImpl node;

    NatSessionManager( NatImpl node )
    {
        this.node = node;
    }

    void registerSession( IPNewSessionRequest request, Protocol protocol,
                          InetAddress clientAddr, int clientPort,
                          InetAddress serverAddr, int serverPort )
    {
        NatSessionData data =
            new NatSessionData( clientAddr, clientPort,
                                request.clientAddr(), request.clientPort(),
                                serverAddr, serverPort,
                                request.serverAddr(), request.serverPort());

        if (logger.isDebugEnabled()) {
            logger.debug( "Registering session: " + request.id());
        }

        /* Insert the data into the map */
        NatSessionData tmp;
        if (( tmp = map.put( request.id(), data )) != null ) {
            logger.error( "Duplicate session key: " + tmp );
        }
    }

    void releaseSession( IPSession session, Protocol protocol )
    {
        NatSessionData sessionData;
        if (logger.isDebugEnabled()) {
            logger.debug( "Releasing session: " + session.id());
        }
        if (( sessionData = map.remove( session.id())) == null ) {
            logger.error( "Released an unmanaged session: " + session );
            return;
        }

        /* Have to release all of the SessionRedirect */
        for ( Iterator<SessionRedirect> iter = sessionData.redirectList().iterator() ; iter.hasNext() ; ) {
            SessionRedirect sessionRedirect = iter.next();

            /* Remove the item from the iterating list */
            iter.remove();

            SessionRedirect currentRedirect = redirectMap.remove( sessionRedirect.key );

            /* Remove the key from the redirect hash map */
            if ( currentRedirect != null && currentRedirect != sessionRedirect  ) {
                logger.error( "Redirect map mismatch" );
            }

            /* Cleanup the redirect */
            sessionRedirect.cleanup( node );
        }
    }

    NatSessionData getSessionData( TCPSession session )
    {
        return map.get( session.id());
    }

    /**
     * Request to redirect a session.
     */
    void registerSessionRedirect( NatSessionData data, SessionRedirectKey key, SessionRedirect redirect )
    {
        /* Add the redirect to the list monitored by this session */
        data.addRedirect( redirect );

        if ( logger.isDebugEnabled()) {
            logger.debug( "Registering[" + key + "],[" + redirect + "]" );
        }

        /* Add the redict to the map of redirects */
        redirectMap.put( key, redirect );
    }

    /**
     * Check to see if this session should be redirected because of one of the
     * it is in the session redirect map
     */
    boolean isSessionRedirect( IPNewSessionRequest request, Protocol protocol ) throws MPipeException
    {
        SessionRedirectKey key = new SessionRedirectKey( request, protocol );
        SessionRedirect redirect;

        // logger.debug( "Looking up session: " + key );

        if (( redirect = redirectMap.remove( key )) == null ) {
            return false;
        }

        // logger.debug( "Session redirect match: " + redirect );

        /* Apply the redirect to the request */
        redirect.redirect( request, node );

        return true;
    }
}

/* For a temporary session redirect you know everything about the session except for the
 * client port */
class SessionRedirectKey
{
    final Protocol    protocol;
    final InetAddress serverAddr;
    final int         serverPort;
    final int         hashCode;

    SessionRedirectKey( Protocol protocol, InetAddress serverAddr, int serverPort )
    {
        this.protocol   = protocol;
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
        hashCode = calculateHashCode();
    }

    SessionRedirectKey( IPNewSessionRequest request, Protocol protocol )
    {
        this( protocol, request.serverAddr(), request.serverPort());
    }

    SessionRedirectKey( TCPSession session )
    {
        this( Protocol.TCP, session.serverAddr(), session.serverPort());
    }

    SessionRedirectKey( IPSession session, Protocol protocol )
    {
        this( protocol, session.serverAddr(), session.serverPort());
    }

    public int hashCode()
    {
        return hashCode;
    }

    public boolean equals( Object o )
    {
        if (!( o instanceof SessionRedirectKey )) return false;

        SessionRedirectKey key = (SessionRedirectKey)o;
        
        if ( this.protocol != key.protocol || 
             !this.serverAddr.equals( key.serverAddr ) || 
             this.serverPort != key.serverPort ) {
            return false;
        }

        return true;
    }

    public String toString()
    {
        return "SessionRedirectKey| [" + protocol +"] " + "/" + serverAddr + ":" + serverPort;
    }


    private int calculateHashCode()
    {
        int result = 17;
        result = ( 37 * result ) + protocol.hashCode();
        result = ( 37 * result ) + serverAddr.hashCode();
        result = ( 37 * result ) + serverPort;

        return result;
    }
}

class SessionRedirect
{
    /* For each item, the item is null or zero if it is unused */
    final InetAddress clientAddr;
    final int         clientPort;

    final InetAddress serverAddr;
    final int         serverPort;

    // True once this redirect has been used.
    boolean           isExpired = false;

    // Set to a non-zero value to reserve a port
    int               reservedPort;

    final SessionRedirectKey key;

    SessionRedirect( InetAddress clientAddr, int clientPort,
                     InetAddress serverAddr, int serverPort,
                     int reservedPort, SessionRedirectKey key )
    {
        this.clientAddr   = clientAddr;
        this.clientPort   = clientPort;

        this.serverAddr   = serverAddr;
        this.serverPort   = serverPort;

        this.reservedPort = reservedPort;

        this.key          = key;
    }

    /* XXX I think this function is no longer used */
    synchronized void redirect( IPNewSessionRequest request, NatImpl node ) throws MPipeException
    {
        NatAttachment attachment = (NatAttachment)request.attachment();

        if ( isExpired ) throw new MPipeException( node.getNatMPipe(), "Expired redirect" );

        /* Have to take this off the list, if it is reserved */
        if ( reservedPort > 0 ) {
            if ( attachment.releasePort() != 0 ) {
                /* This will be cleaned up when the session is cleaned up */
                throw new MPipeException( node.getNatMPipe(), "Session is already using a NAT port" );
            }
        }

        /* Modify the client */
        if ( this.clientAddr != null ) {
            request.clientAddr( clientAddr );
        }

        if ( this.clientPort != 0 ) {
            request.clientPort( clientPort );
        }

        /* Modify the server */
        if ( this.serverAddr != null ) {
            request.serverAddr( serverAddr );
        }

        if ( this.serverPort != 0 ) {
            request.serverPort( serverPort );
        }

        if ( this.reservedPort > 0 ) {
            attachment.releasePort( reservedPort );
            this.reservedPort = 0;
        }

        /* Indicate to never use this redirect again */
        isExpired = true;
    }

    public String toString()
    {
        return "SessionRedirect| " + clientAddr + ":" + clientPort + "/" + serverAddr + ":" + serverPort;
    }

    synchronized void cleanup( NatImpl node )
    {
        if ( reservedPort > 0 ) {
            node.getHandler().releasePort( key.protocol, reservedPort );
        }

        reservedPort = 0;
    }
}

