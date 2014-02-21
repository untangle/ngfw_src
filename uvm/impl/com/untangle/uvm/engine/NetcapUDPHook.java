/**
 * $Id$
 */
package com.untangle.uvm.engine;

import java.util.Iterator;

import org.apache.log4j.Logger;

import com.untangle.jnetcap.UDPAttributes;
import com.untangle.jnetcap.NetcapCallback;
import com.untangle.jnetcap.NetcapSession;
import com.untangle.jnetcap.NetcapUDPSession;
import com.untangle.jvector.Sink;
import com.untangle.jvector.Source;
import com.untangle.jvector.UDPSink;
import com.untangle.jvector.UDPSource;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.vnet.NodeUDPSession;

public class NetcapUDPHook implements NetcapCallback
{
    private static NetcapUDPHook INSTANCE;
    private final Logger logger = Logger.getLogger(getClass());

    public static NetcapUDPHook getInstance() {
        if ( INSTANCE == null )
            init();

        return INSTANCE;
    }

    /* Singleton */
    private NetcapUDPHook()
    {
    }

    private static synchronized void init()
    {
        if ( INSTANCE == null )
            INSTANCE = new NetcapUDPHook();
    }

    public void event( long sessionID )
    {
        Thread.currentThread().setName("Session " + sessionID);
        new UDPNetcapHook( sessionID ).run();
    }

    private class UDPNetcapHook extends NetcapHook
    {
        /**
         * Default 70 second timeout for UDP sessions this fixes VOIP
         * which defaults to a 60 second timeout.
         **/
        protected static final int TIMEOUT = 70 * 1000;

        protected final NetcapUDPSession netcapUDPSession;

        protected final UDPSideListener clientSideListener = new UDPSideListener();
        protected final UDPSideListener serverSideListener = new UDPSideListener();

        protected UDPAttributes serverTraffic = null;
        protected UDPAttributes clientTraffic = null;

        protected NodeUDPSession prevSession = null;

        protected UDPNetcapHook( long id )
        {
            netcapUDPSession = new NetcapUDPSession( id );
            if ( logger.isDebugEnabled() ) {
                logger.debug("New Session: Client side: " + id + " (UDP " +
                             netcapUDPSession.clientSide().client().host().getHostAddress() + ":" + netcapUDPSession.clientSide().client().port() + " -> " +
                             netcapUDPSession.clientSide().server().host().getHostAddress() + ":" + netcapUDPSession.clientSide().server().port() + ")");
                logger.debug("New Session: Server side: " + id + " (UDP " +
                             netcapUDPSession.serverSide().client().host().getHostAddress() + ":" + netcapUDPSession.serverSide().client().port() + " -> " +
                             netcapUDPSession.serverSide().server().host().getHostAddress() + ":" + netcapUDPSession.serverSide().server().port() + ")");
            }
            
            Thread.currentThread().setName("Session " + id + " (UDP " +
                                           netcapUDPSession.clientSide().client().host().getHostAddress() + ":" + netcapUDPSession.clientSide().client().port() + " -> " +
                                           netcapUDPSession.serverSide().server().host().getHostAddress() + ":" + netcapUDPSession.serverSide().server().port() + ")");
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
         * Complete the connection for the server side.  
         */
        protected boolean serverComplete()
        {

            if ( sessionList.isEmpty()) {
                /* No sessions, complete with the current session parameters */
                serverTraffic = new UDPAttributes( netcapUDPSession.serverSide());
            } else {
                /* Setup the UDP parameters to use the parameters from the last session in the chain */
                NodeUDPSession session = (NodeUDPSession)sessionList.get( sessionList.size() - 1 );

                if ( logger.isDebugEnabled()) {
                    logger.debug( "UDP: Completing session:" );
                    logger.debug( "Client: " + session.getClientAddr().getHostAddress() + ":" + session.getClientPort());
                    logger.debug( "Server: " + session.getServerAddr().getHostAddress() + ":" + session.getServerPort());
                }

                serverTraffic = new UDPAttributes( session.getClientAddr(), session.getClientPort(), session.getServerAddr(), session.getServerPort());

                serverTraffic.ttl( ((NodeUDPSessionImpl)session).ttl());
                serverTraffic.tos( ((NodeUDPSessionImpl)session).tos());
            }

            /* Packets cannot go back out on the client interface */
            /* Setup the marking */
            serverTraffic.isMarkEnabled( true );
            
            int nfmark = clientSide.getClientIntf() + ( serverSide.getServerIntf() << 8 );
            serverTraffic.mark( nfmark);

            serverTraffic.lock();

            this.netcapUDPSession.setServerTraffic(serverTraffic);

            netcapUDPSession.serverComplete( serverTraffic );

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
            clientTraffic = UDPAttributes.makeSwapped( netcapUDPSession.clientSide());

            /* Setup the marking */
            clientTraffic.isMarkEnabled( true );
                
            /* Packets cannot go back out on the server interface */
            int nfmark = clientSide.getServerIntf() + ( serverSide.getClientIntf() << 8 );
            clientTraffic.mark( nfmark );

            clientTraffic.lock();

            this.netcapUDPSession.setClientTraffic(clientTraffic);

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
            return new UDPSink( clientTraffic, clientSideListener);
        }

        protected Sink makeServerSink()
        {
            return new UDPSink( serverTraffic, serverSideListener);
        }

        protected Source makeClientSource()
        {
            return new UDPSource( netcapUDPSession.clientMailbox(), clientSideListener );
        }

        protected Source makeServerSource()
        {
            return new UDPSource( netcapUDPSession.serverMailbox(), serverSideListener );
        }

        protected void newSessionRequest( PipelineConnectorImpl agent, Iterator<?> iter, SessionEvent pe )
        {
            UDPNewSessionRequestImpl request;

            if ( prevSession == null ) {
                request = new UDPNewSessionRequestImpl( sessionGlobalState, agent, pe );
            } else {
                request = new UDPNewSessionRequestImpl( prevSession, agent, pe, sessionGlobalState );
            }

            NodeUDPSession session = agent.getDispatcher().newSession( request );

            try {
                processSession( request, ((NodeUDPSessionImpl)session) );
            } catch (IllegalStateException e) {
                logger.warn(agent.toString() + " Exception: ", e);
                throw e;
            }

            if ( iter.hasNext()) {
                /* Only advance the previous session if the node requested the session */
                if (( request.state() == IPNewSessionRequestImpl.REQUESTED ) ||
                    ( request.state() == IPNewSessionRequestImpl.RELEASED && session != null )) {
                    logger.debug( "Passing new session data client: " + session.getClientAddr());
                    prevSession = session;
                } else {
                    logger.debug( "Reusing session data" );
                }
            } else {
                prevSession = null;
            }
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
