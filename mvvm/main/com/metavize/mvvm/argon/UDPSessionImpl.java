/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: UDPSessionImpl.java,v 1.3 2005/02/07 08:40:26 rbscott Exp $
 */

package com.metavize.mvvm.argon;

import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.metavize.jvector.IncomingSocketQueue;
import com.metavize.jvector.OutgoingSocketQueue;
import com.metavize.jvector.Source;
import com.metavize.jvector.Sink;
import com.metavize.jvector.UDPSink;
import com.metavize.jvector.Crumb;
import com.metavize.jvector.UDPPacketCrumb;


public class UDPSessionImpl extends IPSessionImpl implements UDPSession
{
    protected final byte ttl;
    protected final byte tos;
    protected final byte options[];

    private static final Logger logger = Logger.getLogger( UDPSessionImpl.class );

    public UDPSessionImpl( UDPNewSessionRequest request )
    {
        super( request );
        
        ttl = request.ttl();
        tos = request.tos();
        options = request.options();
    }

    /**
     * Retrieve the TTL for a session, this only has an impact for the last session in the chain
     * when passing data crumbs (UDPPacketCrumbs have TTL value inside of them)
     */
    public byte ttl() 
    { 
        return ttl;
    }

    /**
     * Retrieve the TOS for a session, this only has an impact for the last session in the chain
     * when passing data crumbs (UDPPacketCrumbs have TOS value inside of them).
     */
    public byte tos()
    {
        return tos;
    }

    /**
     * Retrieve the options associated with the first UDP packet in the session.
     */
    public byte[] options()
    {
        return options;
    }

    class UDPSessionSocketQueueListener extends SessionSocketQueueListener
    {
        public void event( IncomingSocketQueue in )
        {
            Crumb crumb;
            UDPSink  sink;
            int ttl = -1;

            crumb = in.peek();

            /* XXX This whole sequence is somewhat of a hack to pass the TTL
             * to the endpoint, If the UDP Packet Crumb should be vectored, it
             * wouldn't be necessary */
            if ( crumb.type() == Crumb.TYPE_UDP_PACKET )
                ttl = ((UDPPacketCrumb)crumb).desc().ttl();
            
            if ( in == serverIncomingSocketQueue ) {
                if ( logger.isDebugEnabled()) {
                    logger.debug( "IncomingSocketQueueEvent: server - " + in +
                                  " " + sessionGlobalState );
                }

                listener.serverEvent( in );
                sink = (UDPSink)sessionGlobalState.argonHook().clientSink;

            } else if ( in == clientIncomingSocketQueue ) {
                if ( logger.isDebugEnabled()) {                    
                    logger.debug( "IncomingSocketQueueEvent: client - " + in +
                                  " " + sessionGlobalState );
                }
                
                listener.clientEvent( in );
                sink = (UDPSink)sessionGlobalState.argonHook().serverSink;

            } else {
                /* This should never happen */
                throw new IllegalStateException( "Invalid socket queue: " + in );
            }

            if ( ttl > 0 )
                sink.ttl( (byte)ttl );
        }
    }

}
