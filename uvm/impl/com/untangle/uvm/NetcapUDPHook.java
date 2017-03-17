/**
 * $Id$
 */
package com.untangle.uvm;

import java.net.InetAddress;
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
import com.untangle.uvm.app.SessionEvent;
import com.untangle.uvm.vnet.AppUDPSession;

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

    public void event( long conntrackPtr, int type )
    {}

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

        protected UDPAttributes serviceSideAttributes = null;
        protected UDPAttributes clientSideAttributes = null;

        protected UDPNewSessionRequestImpl prevRequest = null;

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
        protected boolean serverComplete( SessionEvent sessionEvent )
        {
            InetAddress clientAddr = sessionEvent.getSClientAddr();
            int clientPort = sessionEvent.getSClientPort();
            InetAddress serverAddr = sessionEvent.getSServerAddr();
            int serverPort = sessionEvent.getSServerPort();

            serviceSideAttributes = new UDPAttributes( clientAddr, clientPort, serverAddr, serverPort );
            
            //serviceSideAttributes.ttl( ((AppUDPSessionImpl)session).ttl()); XXX
            //serviceSideAttributes.tos( ((AppUDPSessionImpl)session).tos()); XXX

            /* Setup the marking */
            serviceSideAttributes.isMarkEnabled( true );
            int nfmark = netcapUDPSession.clientSide().interfaceId() + ( netcapUDPSession.serverSide().interfaceId() << 8 );
            serviceSideAttributes.mark( nfmark);
            serviceSideAttributes.lock();

            this.netcapUDPSession.setServerTraffic(serviceSideAttributes);

            netcapUDPSession.serverComplete( serviceSideAttributes );

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
            clientSideAttributes = UDPAttributes.makeSwapped( netcapUDPSession.clientSide());

            /* Setup the marking */
            clientSideAttributes.isMarkEnabled( true );
                
            /* Packets cannot go back out on the server interface */
            int nfmark = netcapUDPSession.serverSide().interfaceId() + ( netcapUDPSession.clientSide().interfaceId() << 8 );
            clientSideAttributes.mark( nfmark );

            clientSideAttributes.lock();

            this.netcapUDPSession.setClientTraffic(clientSideAttributes);

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
            return new UDPSink( clientSideAttributes, clientSideListener);
        }

        protected Sink makeServerSink()
        {
            return new UDPSink( serviceSideAttributes, serverSideListener);
        }

        protected Source makeClientSource()
        {
            return new UDPSource( netcapUDPSession.clientMailbox(), clientSideListener );
        }

        protected Source makeServerSource()
        {
            return new UDPSource( netcapUDPSession.serverMailbox(), serverSideListener );
        }

        protected void initializeAppSessions( SessionEvent sessionEvent )
        {
            UDPNewSessionRequestImpl prevRequest = null;

            for ( PipelineConnectorImpl agent : pipelineConnectors ) {
                if ( this.state != IPNewSessionRequestImpl.REQUESTED )
                    break;

                UDPNewSessionRequestImpl request;
                if ( prevRequest == null ) {
                    request = new UDPNewSessionRequestImpl( sessionGlobalState, agent, sessionEvent );
                } else {
                    request = new UDPNewSessionRequestImpl( prevRequest, agent, sessionEvent, sessionGlobalState );
                }

                AppUDPSession session = agent.getDispatcher().newSession( request );

                try {
                    processSession( request, ((AppUDPSessionImpl)session) );
                } catch (IllegalStateException e) {
                    logger.warn(agent.toString() + " Exception: ", e);
                    throw e;
                }
            
                prevRequest = request;
            }

            /* update the session event in case any apps changed the metadata */
            if ( prevRequest != null ) {
                sessionEvent.setSClientAddr( prevRequest.getNewClientAddr() );
                sessionEvent.setSClientPort( prevRequest.getNewClientPort() );
                sessionEvent.setSServerAddr( prevRequest.getNewServerAddr() );
                sessionEvent.setSServerPort( prevRequest.getNewServerPort() );
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
                if ( serviceSideAttributes != null )
                    serviceSideAttributes.raze();
            } catch ( Exception e ) {
                logger.error( "Unable to raze server traffic", e );
            }

            try {
                if ( clientSideAttributes != null )
                    clientSideAttributes.raze();
            } catch ( Exception e ) {
                logger.error( "Unable to raze client traffic", e );
            }
        }

        protected void checkEndpoints()
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

        @Override
        protected void releaseToBypass()
        {
            logger.debug("Releasing to bypass: " + netcapSession());

            netcapSession().orClientMark( 0x01000000 );
            netcapSession().orServerMark( 0x01000000 );

            // set a really low timeout so it will exit after sending any remaining data            
            if ( vector != null )
                vector.timeout(1000); 

            // set the flag so this session does not get removed from the session table
            // we need to maintain the metadata in the untangle-vm because even though
            // this thread is dead, the session is still alive
            // It will get cleaned up later by the ConntrackMonitor.
            this.setCleanupSessionOnExit( false );
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
