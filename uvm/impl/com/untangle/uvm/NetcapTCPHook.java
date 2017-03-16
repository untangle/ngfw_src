/**
 * $Id$
 */
package com.untangle.uvm;

import java.net.InetAddress;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.untangle.jnetcap.NetcapCallback;
import com.untangle.jnetcap.NetcapSession;
import com.untangle.jnetcap.NetcapTCPSession;
import com.untangle.jvector.Sink;
import com.untangle.jvector.Source;
import com.untangle.jvector.TCPSink;
import com.untangle.jvector.TCPSource;
import com.untangle.uvm.app.SessionEvent;
import com.untangle.uvm.vnet.AppTCPSession;

public class NetcapTCPHook implements NetcapCallback
{
    private static NetcapTCPHook INSTANCE;
    private final Logger logger = Logger.getLogger(getClass());


    public static NetcapTCPHook getInstance()
    {
        if ( INSTANCE == null )
            init();

        return INSTANCE;
    }

    /* Singleton */
    private NetcapTCPHook() {}

    private static synchronized void init()
    {
        if ( INSTANCE == null )
            INSTANCE = new NetcapTCPHook();
    }

    public void event( long sessionID )
    {
        Thread.currentThread().setName("Session " + sessionID);
        new TCPNetcapHook( sessionID ).run();
    }

    public void event( long conntrackPtr, int type ) {}
    
    private class TCPNetcapHook extends NetcapHook
    {
        protected static final int TIMEOUT = -1;

        /* Get a reasonable time */
        /* After 10 seconds, this session better die if both endpoints are dead */
        protected static final int DEAD_TIMEOUT = 10 * 1000;

        protected final NetcapTCPSession netcapTCPSession;

        protected boolean ifServerComplete = false;
        protected boolean ifClientComplete = false;

        protected final TCPSideListener clientSideListener = new TCPSideListener();
        protected final TCPSideListener serverSideListener = new TCPSideListener();

        protected TCPNetcapHook( long id )
        {
            netcapTCPSession   = new NetcapTCPSession( id );

            if ( logger.isDebugEnabled() ) {
                logger.debug("New Session: Client side: " + id + " (TCP " +
                             netcapTCPSession.clientSide().client().host().getHostAddress() + ":" + netcapTCPSession.clientSide().client().port() + " -> " +
                             netcapTCPSession.clientSide().server().host().getHostAddress() + ":" + netcapTCPSession.clientSide().server().port() + ")");
                logger.debug("New Session: Server side: " + id + " (TCP " +
                             netcapTCPSession.serverSide().client().host().getHostAddress() + ":" + netcapTCPSession.serverSide().client().port() + " -> " +
                             netcapTCPSession.serverSide().server().host().getHostAddress() + ":" + netcapTCPSession.serverSide().server().port() + ")");
            }

            Thread.currentThread().setName("Session " + id + " (TCP " +
                                           netcapTCPSession.clientSide().client().host().getHostAddress() + ":" + netcapTCPSession.clientSide().client().port() + " -> " +
                                           netcapTCPSession.serverSide().server().host().getHostAddress() + ":" + netcapTCPSession.serverSide().server().port() + ")");
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

        protected boolean serverComplete( SessionEvent sessionEvent )
        {
            InetAddress clientAddr = sessionEvent.getSClientAddr();
            int clientPort = sessionEvent.getSClientPort();
            InetAddress serverAddr = sessionEvent.getSServerAddr();
            int serverPort = sessionEvent.getSServerPort();

            if ( logger.isDebugEnabled()) {
                logger.debug( "TCP - Completing server connection: " + sessionGlobalState );
                logger.debug( "Client: " + clientAddr + ":" + clientPort );
                logger.debug( "Server: " + serverAddr + ":" + serverPort );
            }

            try {
                int intfId = netcapTCPSession.serverSide().interfaceId();
                netcapTCPSession.serverComplete( clientAddr, clientPort, serverAddr, serverPort, intfId );
                netcapTCPSession.serverSide().blocking( false );
                ifServerComplete = true;
            } catch ( Exception e ) {
                logger.debug( "TCP - Unable to complete connection to the server: " + e );
                ifServerComplete = false;
            }

            return ifServerComplete;
        }

        protected boolean clientComplete()
        {
            if ( logger.isDebugEnabled()) {
                logger.debug( "TCP - Completing client connection: " + sessionGlobalState );
            }

            try {
                netcapTCPSession.clientComplete();
                netcapTCPSession.clientSide().blocking( false );

                ifClientComplete = true;
            } catch ( Exception e ) {
                logger.debug( "TCP - Unable to complete connection to the client: " + e );
                ifClientComplete = false;
            }

            return ifClientComplete;
        }

        protected void clientReject()
        {
            if ( logger.isDebugEnabled()) logger.debug( "TCP - Rejecting client" );

            switch( rejectCode ) {
            case IPNewSessionRequestImpl.TCP_REJECT_RESET:
                netcapTCPSession.clientReset();
                break;

            case IPNewSessionRequestImpl.NET_UNREACHABLE:
            case IPNewSessionRequestImpl.HOST_UNREACHABLE:
            case IPNewSessionRequestImpl.PROTOCOL_UNREACHABLE:
            case IPNewSessionRequestImpl.PORT_UNREACHABLE:
            case IPNewSessionRequestImpl.DEST_HOST_UNKNOWN:
            case IPNewSessionRequestImpl.PROHIBITED:
                netcapTCPSession.clientSendIcmpDestUnreach((byte)rejectCode );
                break;

            case REJECT_CODE_SRV:
                netcapTCPSession.clientForwardReject();
                break;

            default:
                logger.error( "TCP - Unknown reject code: " + rejectCode + " resetting " );
                netcapTCPSession.clientReset();
            }
        }

        protected void clientRejectSilent()
        {
            if ( logger.isDebugEnabled()) logger.debug( "TCP - Dropping client" );

            netcapTCPSession.clientDrop();
        }

        protected Sink makeClientSink()
        {
            return new TCPSink( netcapTCPSession.clientSide().fd(), clientSideListener );
        }

        protected Sink makeServerSink()
        {
            if ( !ifServerComplete ) {
                throw new IllegalStateException( "Requesting server sink for an uncompleted connection" );
            }

            return new TCPSink( netcapTCPSession.serverSide().fd(), serverSideListener );
        }

        protected Source makeClientSource()
        {
            return new TCPSource( netcapTCPSession.clientSide().fd(), clientSideListener );
        }

        protected Source makeServerSource()
        {
            if ( !ifServerComplete ) {
                throw new IllegalStateException( "Requesting server source for an uncompleted connection" );
            }

            return new TCPSource( netcapTCPSession.serverSide().fd(), serverSideListener );
        }

        protected void initializeAppSessions( SessionEvent sessionEvent )
        {
            TCPNewSessionRequestImpl prevRequest = null;

            for ( PipelineConnectorImpl agent : pipelineConnectors ) {
                if ( this.state != IPNewSessionRequestImpl.REQUESTED )
                    break;

                TCPNewSessionRequestImpl request;
                if ( prevRequest == null ) {
                    request = new TCPNewSessionRequestImpl( sessionGlobalState, agent, sessionEvent );
                } else {
                    request = new TCPNewSessionRequestImpl( prevRequest, agent, sessionEvent, sessionGlobalState );
                }

                if ( agent.getDispatcher() == null ) {
                    logger.warn("NULL DISPATCHER XXX " + agent.getName());
                }
                    
                AppTCPSession session = agent.getDispatcher().newSession( request );

                try {
                    processSession( request, ((AppTCPSessionImpl)session) );
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
            netcapTCPSession.raze();
        }

        protected void checkEndpoints()
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

        @Override
        protected void releaseToBypass()
        {
            logger.warn("releaseToBypass() not supported for TCP.", new Exception());
        }
        
        private class TCPSideListener extends SideListener
        {
            protected TCPSideListener()
            {
            }

            public void dataEvent( Source source, int numBytes )
            {
                super.dataEvent( source, numBytes );
            }

            public void dataEvent( Sink sink, int numBytes )
            {
                super.dataEvent( sink, numBytes );
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
