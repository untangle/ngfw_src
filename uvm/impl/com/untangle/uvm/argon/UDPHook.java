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

package com.untangle.uvm.argon;

import java.util.Iterator;

import org.apache.log4j.Logger;

import com.untangle.jnetcap.IPTraffic;
import com.untangle.jnetcap.NetcapHook;
import com.untangle.jnetcap.NetcapSession;
import com.untangle.jnetcap.NetcapUDPSession;
import com.untangle.jvector.Sink;
import com.untangle.jvector.Sink;
import com.untangle.jvector.Sink;
import com.untangle.jvector.Source;
import com.untangle.jvector.UDPSink;
import com.untangle.jvector.UDPSource;
import com.untangle.uvm.policy.PolicyRule;
import com.untangle.uvm.node.PipelineEndpoints;

import com.untangle.uvm.localapi.LocalIntfManager;


public class UDPHook implements NetcapHook
{
    private static UDPHook INSTANCE;
    private final Logger logger = Logger.getLogger(getClass());

    private int icmpServerId = -1;

    public static UDPHook getInstance() {
        if ( INSTANCE == null )
            init();

        return INSTANCE;
    }

    /* Singleton */
    private UDPHook()
    {
    }

    private static synchronized void init()
    {
        if ( INSTANCE == null )
            INSTANCE = new UDPHook();

        ArgonHook.init();
    }

    public void event( int sessionID )
    {
        /* XXX Need to change the priority of the thread */
        // new Thread( new UDPArgonHook( sessionID )).start();
        new UDPArgonHook( sessionID ).run();
    }

    private class UDPArgonHook extends ArgonHook
    {
        /** Default thirty second timeout for UDP sessions **/
        protected static final int TIMEOUT = 30 * 1000;

        protected final NetcapUDPSession netcapUDPSession;

        protected final UDPSideListener clientSideListener = new UDPSideListener();
        protected final UDPSideListener serverSideListener = new UDPSideListener();

        protected boolean isIcmpSession;

        protected IPTraffic serverTraffic = null;
        protected IPTraffic clientTraffic = null;

        protected UDPSession prevSession = null;

        protected UDPArgonHook( int id )
        {
            netcapUDPSession = new NetcapUDPSession( id );
            isIcmpSession = netcapUDPSession.isIcmpSession();
        }

        protected int timeout()
        {
            return TIMEOUT;
        }

        protected NetcapSession netcapSession()
        {
            return netcapUDPSession;
        }

        protected SideListener clientSideListener()
        {
            return clientSideListener;
        }

        protected SideListener serverSideListener()
        {
            return serverSideListener;
        }

        /**
         * Complete the connection for the server side.  This merges
         * the sessions inside of the netcap session table.  It may
         * happen that this session has been merged out, at which point
         * this session is ended.
         */
        protected boolean serverComplete()
        {
            LocalIntfManager lim = Argon.getInstance().getIntfManager();

            if ( sessionList.isEmpty()) {
                /* No sessions, complete with the current session parameters */
                serverTraffic = new IPTraffic( netcapUDPSession.serverSide());
            } else {
                /* Setup the UDP parameters to use the parameters from the last session in the chain */
                UDPSession session = (UDPSession)sessionList.get( sessionList.size() - 1 );

                if ( logger.isInfoEnabled()) {
                    logger.info( "UDP: Completing session:" );
                    logger.info( "Client: " + session.clientAddr() + ":" + session.clientPort());
                    logger.info( "Server: " + session.serverAddr() + ":" + session.serverPort());
                }

                serverTraffic = new IPTraffic( session.clientAddr(), session.clientPort(),
                                               session.serverAddr(), session.serverPort());

                serverTraffic.ttl( session.ttl());
                serverTraffic.tos( session.tos());
                /** XXX Setup the options */

                /* Update the ICMP id */
                icmpServerId = session.icmpId();
            }

            /* Packets cannot go back out on the client interface */
            if ( isMirrored()) {
                serverTraffic.isMarkEnabled( false );
            } else {
                /* Setup the marking */
                serverTraffic.isMarkEnabled( true );

                serverTraffic.mark( lim.toNetcap( clientSide.clientIntf()));
            }

            serverTraffic.lock();

            byte intf = lim.toNetcap( serverSide.serverIntf());

            /* XXXX ICMP HACK */
            if ( isIcmpSession ) {
                if ( !netcapUDPSession.icmpMerge( serverTraffic, icmpServerId, intf )) {
                    /* Merged out and indicate that the session was rejected */
                    state = IPNewSessionRequest.REJECTED;
                    return false;
                }
            } else {
                if ( !netcapUDPSession.merge( serverTraffic, intf )) {
                    /* Merged out and indicate that the session was rejected */
                    state = IPNewSessionRequest.REJECTED;
                    return false;
                }
            }

            return true;
        }

        /**
         * Complete the connection for the client side.  This pretty much just locks
         * the client side traffic object.  The connection always completes, if the
         * client is uninterested, they will send an ICMP packet which will one
         * day get vectored.
         */
        protected boolean clientComplete()
        {
            clientTraffic = IPTraffic.makeSwapped( netcapUDPSession.clientSide());

            /* Setup the marking */
            if ( isMirrored()) {
                clientTraffic.isMarkEnabled( false );
            } else {
                clientTraffic.isMarkEnabled( true );

                /* Packets cannot go back out on the server interface */
                clientTraffic.mark( Argon.getInstance().getIntfManager().toNetcap( serverSide.serverIntf()));
            }

            clientTraffic.lock();
            return true;
        }

        protected void clientReject()
        {
            /* XXX do something */
        }


        protected void clientRejectSilent()
        {
            /* XXX do something */
        }

        protected Sink makeClientSink()
        {
            /* XXX ICMP Hack */
            int icmpClientId = 0;
            if ( netcapUDPSession.isIcmpSession()) icmpClientId = netcapUDPSession.icmpClientId();

            return new UDPSink( clientTraffic, clientSideListener, netcapUDPSession.icmpClientMailbox(),
                                icmpClientId );
        }

        protected Sink makeServerSink()
        {
            return new UDPSink( serverTraffic, serverSideListener, netcapUDPSession.icmpServerMailbox(),
                                icmpServerId );
        }

        protected Source makeClientSource()
        {
            return new UDPSource( netcapUDPSession.clientMailbox(), clientSideListener );
        }

        protected Source makeServerSource()
        {
            return new UDPSource( netcapUDPSession.serverMailbox(), serverSideListener );
        }

        protected void newSessionRequest( ArgonAgent agent, Iterator iter, byte originalServerIntf, PipelineEndpoints pe )
        {
            UDPNewSessionRequest request;

            if ( prevSession == null ) {
                request = new UDPNewSessionRequestImpl( sessionGlobalState, agent, originalServerIntf, pe );
            } else {
                request = new UDPNewSessionRequestImpl( prevSession, agent, originalServerIntf, pe );
            }

            PolicyRule pr = pipelineDesc.getPolicyRule();
            boolean isInbound = pr == null ? true : pr.isInbound();
            UDPSession session = agent.getNewSessionEventListener().newSession( request, isInbound );

            processSession( request, session );

            if ( iter.hasNext()) {
                /* Only advance the previous session if the node requested the session */
                if (( request.state() == IPNewSessionRequest.REQUESTED ) ||
                    ( request.state() == IPNewSessionRequest.RELEASED && session != null )) {
                    logger.debug( "Passing new session data client: " + session.clientAddr());
                    prevSession = session;
                } else {
                    logger.debug( "Reusing session data" );
                }
            } else {
                prevSession = null;
            }
        }

        protected void liberate()
        {
            netcapUDPSession.liberate();
        }

        protected void raze()
        {
            try {
                netcapUDPSession.raze();
            } catch ( Exception e ) {
                logger.error( "Unable to raze UDP Session", e );
            }

            /* No longer need these */

            try {
                if ( serverTraffic != null )
                    serverTraffic.raze();
            } catch ( Exception e ) {
                logger.error( "Unable to raze server traffic", e );
            }

            try {
                if ( clientTraffic != null )
                    clientTraffic.raze();
            } catch ( Exception e ) {
                logger.error( "Unable to raze client traffic", e );
            }
        }

        public void checkEndpoints()
        {
            /* If both sides are shutdown, give a timeout to complete vectoring */
            if ( clientSideListener.isShutdown() && serverSideListener.isShutdown()) {
                if ( logger.isDebugEnabled()) {
                    logger.debug( "Stats" );
                    logger.debug( "client side: " + clientSideListener.stats());
                    logger.debug( "server side: " + serverSideListener.stats());
                }
            }
        }

        private class UDPSideListener extends SideListener
        {
            protected UDPSideListener()
            {
            }

            public void shutdownEvent( Source source )
            {
                super.shutdownEvent( source );
                checkEndpoints();
            }

            public void shutdownEvent( Sink sink )
            {
                super.shutdownEvent( sink );
                checkEndpoints();
            }
        }
    }
}
