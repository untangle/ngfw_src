/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: TCPHook.java,v 1.27 2005/02/10 06:37:58 rbscott Exp $
 */

package com.metavize.mvvm.argon;

import java.net.InetAddress;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.metavize.jnetcap.Netcap;
import com.metavize.jnetcap.NetcapHook;
import com.metavize.jnetcap.NetcapSession;
import com.metavize.jnetcap.NetcapTCPSession;
import com.metavize.jnetcap.IPTraffic;
import com.metavize.jnetcap.Inet4AddressConverter;
import com.metavize.jnetcap.Shield;

import com.metavize.jvector.Sink;
import com.metavize.jvector.TCPSink;
import com.metavize.jvector.Source;
import com.metavize.jvector.TCPSource;
import com.metavize.jvector.IncomingSocketQueue;
import com.metavize.jvector.OutgoingSocketQueue;

public class TCPHook implements NetcapHook
{
    private static TCPHook INSTANCE;
    private static final Logger logger = Logger.getLogger( TCPHook.class );

    private final Shield shield;
    
    public static TCPHook getInstance() {
        if ( INSTANCE == null )
            init();

        return INSTANCE;
    }

    /* Singleton */
    private TCPHook()
    {
        shield = Shield.getInstance();
    }

    private static synchronized void init()
    {
        if ( INSTANCE == null )
            INSTANCE = new TCPHook();

        ArgonHook.init();
    }


    public void event( int sessionID )
    {
        /* XXX Need to change the priority of the thread */
        new Thread( new TCPArgonHook( sessionID )).start();
    }
        
    private class TCPArgonHook extends ArgonHook
    {
        protected static final int TIMEOUT = -1;

        /* Get a reasonable time */
        /* After 10 seconds, this session better die if both endpoints are dead */
        protected static final int DEAD_TIMEOUT = 10 * 1000;

        protected final NetcapTCPSession netcapTCPSession;

        protected boolean ifServerComplete = false;
        protected boolean ifClientComplete = false;

        protected TCPSession prevSession = null;
        protected final TCPSideListener clientSideListener = new TCPSideListener();
        protected final TCPSideListener serverSideListener = new TCPSideListener();
        protected final long shieldUser;

        protected TCPArgonHook( int id )
        {
            netcapTCPSession   = new NetcapTCPSession( id );
            shieldUser = Inet4AddressConverter.toLong( netcapTCPSession.clientSide().client().host());
        }
        
        protected int timeout()
        {
            return TIMEOUT;
        }

        protected NetcapSession netcapSession()
        {
            return netcapTCPSession;
        }

        protected SideListener clientSideListener()
        {
            return clientSideListener;
        }

        protected SideListener serverSideListener()
        {
            return serverSideListener;
        }
        
        protected boolean serverComplete() 
        {
            InetAddress clientAddr;
            int clientPort;
            InetAddress serverAddr;
            int serverPort;
            int flags = NetcapTCPSession.NON_LOCAL_BIND;
            
            if ( sessionList.isEmpty()) {
                clientAddr = netcapTCPSession.serverSide().client().host();
                clientPort = netcapTCPSession.serverSide().client().port();

                serverAddr = netcapTCPSession.serverSide().server().host();
                serverPort = netcapTCPSession.serverSide().server().port();
            } else {
                /* Complete with the parameters from the last transform */
                TCPSession session = (TCPSession)sessionList.get( sessionList.size() - 1 );
                
                clientAddr = session.clientAddr();
                clientPort = session.clientPort();
                serverAddr = session.serverAddr();
                serverPort = session.serverPort();
            }

            /* XXX Have to check if it is destined locally, if so, you don't create two 
             * connections, you just connect locally, instead, we could redirect the connection
             * from 127.0.0.1 to 127.0.0.1, it just limits the number of possible sessions
             * to that one server to 0xFFFF */

            if ( logger.isInfoEnabled()) {
                logger.info( "TCP - Completing server connection: " + sessionGlobalState );

                if (( flags & NetcapTCPSession.NON_LOCAL_BIND) == NetcapTCPSession.NON_LOCAL_BIND ) {
                    logger.debug( "Using non-local binding" );
                }
                logger.debug( "Client: " + clientAddr + ":" + clientPort );
                logger.debug( "Server: " + serverAddr + ":" + serverPort );
            }

            try {
                netcapTCPSession.serverComplete( clientAddr, clientPort, serverAddr, serverPort, flags );
                netcapTCPSession.tcpServerSide().blocking( false );
                ifServerComplete = true;
            } catch ( Exception e ) {
                logger.error( "TCP - Unable to complete connection to the server", e );
                ifServerComplete = false;
            }
            
            return ifServerComplete;
        }
        
        protected boolean clientComplete() 
        {
            if ( logger.isInfoEnabled()) {
                logger.info( "TCP - Completing client connection: " + sessionGlobalState );
                logger.info( "TCP - client acked: " + netcapTCPSession.acked());
            }
            
            try {
                if ( !netcapTCPSession.acked()) 
                    netcapTCPSession.clientComplete();

                netcapTCPSession.tcpClientSide().blocking( false );

                ifClientComplete = true;
            } catch ( Exception e ) {
                logger.error( "TCP - Unable to complete connection to the client", e );
                ifClientComplete = false;
            }
            
            return ifClientComplete;
        }

        protected void clientReject()
        {
            if ( logger.isDebugEnabled()) {
                logger.debug( "TCP - Rejecting client" );
            }
            /* XXX Must implement the ability to return an error code */
            netcapTCPSession.clientReset();
        }

        protected void clientRejectSilent()
        {
            /* XXX Not quite sure what to do here, for now we are rejecting
             * all session */
            logger.error( "Reseting client when silent reject requested." );
            clientReject();
        }
        
        protected Sink makeClientSink()
        {
            return new TCPSink( netcapTCPSession.tcpClientSide().fd(), clientSideListener );
        }
        
        protected Sink makeServerSink()
        {
            if ( !ifServerComplete ) {
                throw new IllegalStateException( "Requesting server sink for an uncompleted connection" );
            }

            return new TCPSink( netcapTCPSession.tcpServerSide().fd(), serverSideListener );
        }

        protected Source makeClientSource()
        {
            return new TCPSource( netcapTCPSession.tcpClientSide().fd(), clientSideListener );
        }
        
        protected Source makeServerSource()
        {
            if ( !ifServerComplete ) {
                throw new IllegalStateException( "Requesting server source for an uncompleted connection" );
            }

            return new TCPSource( netcapTCPSession.tcpServerSide().fd(), serverSideListener );
        }

        protected void newSessionRequest( ArgonAgent agent, Iterator iter )
        {
            TCPNewSessionRequest request;

            if ( prevSession == null )
                request = new TCPNewSessionRequestImpl( sessionGlobalState, agent );
            else
                request = new TCPNewSessionRequestImpl( prevSession, agent );
                            
            TCPSession session = agent.getNewSessionEventListener().newSession( request );
            
            processSession( request, session );
            
            if ( iter.hasNext()) {
                /* Only advance the previous session if the transform requested the session */
                if ( request.state() == IPNewSessionRequest.REQUESTED ) prevSession = session;
            } else {
                prevSession = null;
            }
        }

        protected void raze() 
        {
            netcapTCPSession.raze();
        }

        public void checkEndpoints()
        {
            /* If both sides are shutdown, give a timeout to complete vectoring */
            if ( clientSideListener.isShutdown() && serverSideListener.isShutdown()) {
                if ( logger.isDebugEnabled()) {
                    logger.debug( "Setting timeout for dead endpoint TCP session to " + DEAD_TIMEOUT );
                    logger.debug( "Stats" );
                    logger.debug( "client side: " + clientSideListener.stats());
                    logger.debug( "server side: " + serverSideListener.stats());
                }
                    
                vector.timeout( DEAD_TIMEOUT );
            }
        }

        private class TCPSideListener extends SideListener
        {
            protected TCPSideListener()
            {
            }

            public void dataEvent( Source source, int numBytes )
            {
                super.dataEvent( source, numBytes );
                shield.addChunk( shieldUser , Netcap.IPPROTO_TCP, numBytes );
            }
            
            public void dataEvent( Sink sink, int numBytes )
            {
                super.dataEvent( sink, numBytes );
                shield.addChunk( shieldUser, Netcap.IPPROTO_TCP, numBytes );
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
