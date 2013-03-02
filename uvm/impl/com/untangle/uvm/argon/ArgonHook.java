/**
 * $Id$
 */
package com.untangle.uvm.argon;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.untangle.jnetcap.NetcapSession;
import com.untangle.jvector.IncomingSocketQueue;
import com.untangle.jvector.OutgoingSocketQueue;
import com.untangle.jvector.Relay;
import com.untangle.jvector.ResetCrumb;
import com.untangle.jvector.Sink;
import com.untangle.jvector.Source;
import com.untangle.jvector.Vector;
import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.NetworkManager;
import com.untangle.uvm.HostTable;
import com.untangle.uvm.HostTableEntry;
import com.untangle.uvm.engine.PipelineFoundryImpl;
import com.untangle.uvm.node.SessionTuple;
import com.untangle.uvm.node.SessionTupleImpl;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.node.SessionNatEvent;
import com.untangle.uvm.node.SessionStatsEvent;
import com.untangle.uvm.node.PolicyManager;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.node.HostnameLookup;
import com.untangle.uvm.network.InterfaceSettings;

/**
 * Helper class for the IP session hooks.
 */
public abstract class ArgonHook implements Runnable
{
    private final Logger logger = Logger.getLogger(getClass());
    private static final ArgonSessionTable activeSessions = ArgonSessionTable.getInstance();

    /* Reject the client with whatever response the server returned */
    protected static final int REJECT_CODE_SRV = -1;

    /**
     * List of all of the nodes( ArgonAgents )
     */
    protected List<ArgonAgent> pipelineAgents;
    protected Long policyId = null;

    protected List<ArgonSession> sessionList = new ArrayList<ArgonSession>();
    protected List<ArgonSession> releasedSessionList = new ArrayList<ArgonSession>();

    protected Source clientSource;
    protected Sink   clientSink;
    protected Source serverSource;
    protected Sink   serverSink;

    protected Vector vector = null;

    protected SessionGlobalState sessionGlobalState;

    protected SessionTuple clientSide = null;
    protected SessionTuple serverSide = null;

    protected static final PipelineFoundryImpl pipelineFoundry = (PipelineFoundryImpl)UvmContextFactory.context().pipelineFoundry();
    
    /**
     * State of the session
     */
    protected int state      = ArgonIPNewSessionRequest.REQUESTED;
    protected int rejectCode = REJECT_CODE_SRV;

    /**
     * Thread hook
     */
    public final void run()
    {
        long start = 0;
        SessionEvent sessionEvent = null;
        
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

            sessionGlobalState = new SessionGlobalState( netcapSession(), clientSideListener(), serverSideListener(), this );
            boolean serverActionCompleted = false;
            boolean clientActionCompleted = false;
            NetcapSession netcapSession = sessionGlobalState.netcapSession();
            int clientIntf = netcapSession.clientSide().interfaceId();
            int serverIntf = netcapSession.serverSide().interfaceId();
            InetAddress clientAddr = netcapSession.clientSide().client().host();
            long sessionId = sessionGlobalState.id();
            if ( logger.isDebugEnabled()) {
                logger.debug( "New thread for session id: " + sessionId + " " + sessionGlobalState );
            }

            if ( serverIntf == IntfConstants.UNKNOWN_INTF ) {
                logger.warn( "Unknown destination interface: " + netcapSession + ". Killing session." );
                raze();
                return;
            } else {
                netcapSession.setServerIntf(serverIntf);
            }

            /**
             * Create the initial tuples based on current information
             * Set the current serverSide = clientSide, the apps (like router) will change the tuple if it gets NATd or port forwarded
             */
            clientSide = new SessionTupleImpl( sessionGlobalState.id(),
                                               sessionGlobalState.getProtocol(),
                                               netcapSession.clientSide().interfaceId(), /* always get clientIntf from client side */
                                               netcapSession.serverSide().interfaceId(), /* always get serverIntf from server side */
                                               netcapSession.clientSide().client().host(),
                                               netcapSession.clientSide().server().host(),
                                               netcapSession.clientSide().client().port(),
                                               netcapSession.clientSide().server().port());
            serverSide = clientSide;

            /* lookup the user information */
            HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry( clientAddr );
            String username = ( entry == null ? null : entry.getUsername() );
            if (username != null && username.length() > 0 ) { 
                logger.debug( "user information: " + username );
                sessionGlobalState.setUser( username );
                sessionGlobalState.attach( NodeSession.KEY_PLATFORM_USERNAME, username );
            }
            /* lookup the hostname information */
            HostnameLookup router = (HostnameLookup) UvmContextFactory.context().nodeManager().node("untangle-node-router");
            HostnameLookup reporting = (HostnameLookup) UvmContextFactory.context().nodeManager().node("untangle-node-reporting");
            String hostname = ( entry == null ? null : entry.getHostname() );
            if ((hostname == null || hostname.length() == 0) && reporting != null)
                hostname = reporting.lookupHostname( clientAddr );
            if ((hostname == null || hostname.length() == 0) && router != null)
                hostname = router.lookupHostname( clientAddr );
            if ((hostname == null || hostname.length() == 0))
                hostname = clientAddr.getHostAddress();
            if (hostname != null && hostname.length() > 0 ) {
                sessionGlobalState.attach( NodeSession.KEY_PLATFORM_HOSTNAME, hostname );
                /* If the hostname isn't known in the host table and its a local host (not from WAN) then set hostname */
                if ( entry == null || !entry.isHostnameKnown()) {
                    InterfaceSettings intfSettings = UvmContextFactory.context().networkManager().findInterfaceId( netcapSession.clientSide().interfaceId() );
                    if ( intfSettings != null && ! intfSettings.getIsWan() ) {
                        entry = UvmContextFactory.context().hostTable().getHostTableEntry( clientAddr, true ); /* create/get entry */
                        entry.setHostname( hostname );
                    }
                }
            }
            
            PolicyManager policyManager = (PolicyManager) UvmContextFactory.context().nodeManager().node("untangle-node-policy");
            if (policyManager != null) {
                this.policyId  = policyManager.findPolicyId( clientSide, username, hostname );
            } else {
                this.policyId = 1L; /* Default Policy */
            }

            pipelineAgents = pipelineFoundry.weld( sessionGlobalState.id(), clientSide, policyId );
            sessionGlobalState.setArgonAgents(pipelineAgents);
            
            /* Create the sessionEvent early so they can be available at request time. */
            sessionEvent =  new SessionEvent( );
            sessionEvent.setSessionId( sessionGlobalState.id() );
            sessionEvent.setProtocol( sessionGlobalState.getProtocol() );
            sessionEvent.setClientIntf( clientSide.getClientIntf() );
            sessionEvent.setServerIntf( clientSide.getServerIntf() );
            sessionEvent.setUsername( username );
            sessionEvent.setHostname( hostname );
            sessionEvent.setPolicyId(policyId);
            sessionEvent.setCClientAddr( clientSide.getClientAddr() );
            sessionEvent.setCClientPort( clientSide.getClientPort() );
            sessionEvent.setCServerAddr( clientSide.getServerAddr() );
            sessionEvent.setCServerPort( clientSide.getServerPort() );
            sessionEvent.setSClientAddr( clientSide.getClientAddr() );
            sessionEvent.setSClientPort( clientSide.getClientPort() );
            sessionEvent.setSServerAddr( clientSide.getServerAddr() );
            sessionEvent.setSServerPort( clientSide.getServerPort() );

            /* log the session event */
            UvmContextFactory.context().logEvent(sessionEvent);
            
            /* Initialize all of the nodes, sending the request events to each in turn */
            initNodes( sessionEvent );

            /* Connect to the server */
            serverActionCompleted = connectServer();

            /* Now generate the server side since the nodes may have
             * modified the sessionEvent (we can't do it until we connect
             * to the server since that is what actually modifies the
             * session global state. */
            serverSide = new SessionTupleImpl( sessionGlobalState.id(),
                                               sessionGlobalState.getProtocol(),
                                               netcapSession.clientSide().interfaceId(), /* always get clientIntf from client side */
                                               netcapSession.serverSide().interfaceId(), /* always get serverIntf from server side */
                                               netcapSession.serverSide().client().host(),
                                               netcapSession.serverSide().server().host(),
                                               netcapSession.serverSide().client().port(),
                                               netcapSession.serverSide().server().port());

            /* Connect to the client */
            clientActionCompleted = connectClient();

            /* If any NAT/transformation of the session has taken place, log a NAT event to update the server side attributes */
            if (  clientSide.getServerIntf() != serverSide.getServerIntf() ||
                ! clientSide.getClientAddr().equals( serverSide.getClientAddr() ) ||
                  clientSide.getClientPort() !=  serverSide.getClientPort()  ||
                ! clientSide.getServerAddr().equals( serverSide.getServerAddr() ) ||
                  clientSide.getServerPort() != serverSide.getServerPort() ) {
                SessionNatEvent natEvent = new SessionNatEvent( sessionEvent.getSessionId(),
                                                                serverSide.getServerIntf(),
                                                                serverSide.getClientAddr(),
                                                                serverSide.getClientPort(),                                                               
                                                                serverSide.getServerAddr(),
                                                                serverSide.getServerPort());
                UvmContextFactory.context().logEvent(natEvent);

                /* Re-set any attribute thats may have been changed by the node */
                sessionEvent.setSClientAddr( serverSide.getClientAddr() );
                sessionEvent.setSClientPort( serverSide.getClientPort() );
                sessionEvent.setSServerAddr( serverSide.getServerAddr() );
                sessionEvent.setSServerPort( serverSide.getServerPort() );
                sessionEvent.setServerIntf( serverSide.getServerIntf() );
            }

            /* Remove all non-vectored sessions, it is non-efficient
             * to iterate the session list twice, but the list is
             * typically small and this logic may get very complex
             * otherwise */
            for ( Iterator<ArgonSession> iter = sessionList.iterator(); iter.hasNext() ; ) {
                ArgonSession session = iter.next();
                if ( !session.isVectored()) {
                    logger.debug( "Removing non-vectored session from the session list" + session );
                    iter.remove();
                    /* Append to the released session list */
                    releasedSessionList.add( session );
                }

                // Complete (if we completed both server and client)
                if (serverActionCompleted && clientActionCompleted)
                    ((ArgonSessionImpl)session).complete();
            }

            /* Only start vectoring if the session is alive */
            if ( alive()) {
                try {
                    /* Build the pipeline */
                    buildPipeline();

                    /* Insert the vector */
                    activeSessions.put( vector, sessionGlobalState );

                    /* Set the timeout for the vectoring machine */
                    vector.timeout( timeout() );

                    if ( logger.isDebugEnabled())
                        logger.debug( "Starting vectoring for session " + sessionGlobalState );
                    
                    /* Start vectoring */
                    vector.vector();

                    /* Call the raze method for each session */
                } catch ( Exception e ) {
                    logger.error( "Exception inside argon hook: " + sessionGlobalState, e );
                }

                if ( logger.isDebugEnabled())
                    logger.debug( "Finished vectoring for session: " + sessionGlobalState );
            } else {
                logger.info( "Session rejected, skipping vectoring: " + sessionGlobalState );
            }
        } catch ( Exception e ) {
            /* Some exceptions have null messages, who knew */
            String message = e.getMessage();
            if ( message == null ) message = "";

            if ( message.startsWith( "Invalid netcap interface" )) {
                try {
                    logger.warn( "invalid interface: " + sessionGlobalState.netcapSession());
                } catch( Exception exn ) {
                    /* Just in case */
                    logger.warn( "exception debugging invalid netcap interface: ", exn );
                }
            } else if ( message.startsWith( "netcap_interface_dst_intf" )) {
                logger.warn( "Unable to determine the outgoing interface: " + sessionGlobalState.netcapSession() );
            } else {
                logger.warn( "Exception executing argon hook:", e );
            }
        }

        try {
            /* Must raze sessions all sessions in the session list */
            razeSessions();
        } catch ( Exception e ) {
            logger.error( "Exception razing sessions", e );
        }

        try {
            /* Let the pipeline foundry know */
            if (clientSide != null) {
                SessionStatsEvent statEvent = new SessionStatsEvent(sessionEvent);
                statEvent.setC2pBytes(sessionGlobalState.clientSideListener().rxBytes);
                statEvent.setP2cBytes(sessionGlobalState.clientSideListener().txBytes);
                statEvent.setC2pChunks(sessionGlobalState.clientSideListener().rxChunks);
                statEvent.setP2cChunks(sessionGlobalState.clientSideListener().txChunks);
                statEvent.setS2pBytes(sessionGlobalState.serverSideListener().rxBytes);
                statEvent.setP2sBytes(sessionGlobalState.serverSideListener().txBytes);
                statEvent.setS2pChunks(sessionGlobalState.serverSideListener().rxChunks);
                statEvent.setP2sChunks(sessionGlobalState.serverSideListener().txChunks);
                UvmContextFactory.context().logEvent( statEvent );

                /* log and destroy the session */
                pipelineFoundry.destroy( sessionGlobalState.id() );
            }

            /* Remove the vector from the vectron table */
            /* You must remove the vector before razing, or else the
             * vector may receive a message(eg shutdown) from another
             * thread */
            activeSessions.remove( vector );
        } catch ( Exception e ) {
            logger.error( "Exception destroying pipeline", e );
        }

        try {
            /* Delete the vector */
            if ( vector != null ) vector.raze();

            /* Delete everything else */
            raze();

            if ( logger.isDebugEnabled()) logger.debug( "Exiting thread: " + sessionGlobalState );
        } catch ( Exception e ) {
            logger.error( "Exception razing vector and session", e );
        }
    }

    public SessionTuple getClientSide()
    {
        return this.clientSide;
    }

    public SessionTuple getServerSide()
    {
        return this.serverSide;
    }

    public Long getPolicyId()
    {
        return this.policyId;
    }
    
    /**
     * Initialize each of the nodes for the new session. </p>
     */
    private void initNodes( SessionEvent event )
    {
        for ( Iterator<ArgonAgent> iter = pipelineAgents.iterator() ; iter.hasNext() ; ) {
            ArgonAgent agent = iter.next();

            if ( state == ArgonIPNewSessionRequest.REQUESTED ) {
                newSessionRequest( agent, iter, event );
            } else {
                /* NodeSession has been rejected or endpointed, remaining
                 * nodes need not be informed 
                 * Don't need to remove anything from the pipeline, it
                 * is just used here iter.remove();
                 */
                break;
            }
        }
    }

    /**
     * Describe <code>connectServer</code> method here.
     *
     * @return a <code>boolean</code> true if the server was completed
     * OR we explicitly rejected
     */
    private boolean connectServer()
    {
        boolean serverActionCompleted = true;
        switch ( state ) {
        case ArgonIPNewSessionRequest.REQUESTED:
            /* If the server doesn't complete, we have to "vector" the reset */
            if ( !serverComplete()) {
                /* ??? May want to send different codes, or something ??? */
                if ( vectorReset()) {
                    /* Forward the rejection type that was passed from
                     * the server */
                    state        = ArgonIPNewSessionRequest.REJECTED;
                    rejectCode = REJECT_CODE_SRV;
                    serverActionCompleted = false;
                } else {
                    state = ArgonIPNewSessionRequest.ENDPOINTED;
                }
            }
            break;

            /* Nothing to do on the server side */
        case ArgonIPNewSessionRequest.ENDPOINTED: /* fallthrough */
        case ArgonIPNewSessionRequest.REJECTED: /* fallthrough */
        case ArgonIPNewSessionRequest.REJECTED_SILENT: /* fallthrough */
            break;

        default:
            throw new IllegalStateException( "Invalid state" );

        }
        return serverActionCompleted;
    }

    /**
     * Describe <code>connectClient</code> method here.
     *
     * @return a <code>boolean</code> true if the client was completed
     * OR we explicitly rejected.
     */
    private boolean connectClient()
    {
        boolean clientActionCompleted = true;

        switch ( state ) {
        case ArgonIPNewSessionRequest.REQUESTED:
        case ArgonIPNewSessionRequest.ENDPOINTED:
            if ( !clientComplete()) {
                logger.info( "Unable to complete connection to client" );
                state = ArgonIPNewSessionRequest.REJECTED;
                clientActionCompleted = false;
            }
            break;

        case ArgonIPNewSessionRequest.REJECTED:
            logger.debug( "Rejecting session" );
            clientReject();
            break;

        case ArgonIPNewSessionRequest.REJECTED_SILENT:
            logger.debug( "Rejecting session silently" );
            clientRejectSilent();
            break;

        default:
            throw new IllegalStateException( "Invalid state" );
        }

        return clientActionCompleted;
    }

    protected void buildPipeline() 
    {
        LinkedList<Relay> relayList = new LinkedList<Relay>();

        if ( sessionList.isEmpty() ) {
            if ( state == ArgonIPNewSessionRequest.ENDPOINTED ) {
                throw new IllegalStateException( "Endpointed session without any nodes" );
            }

            clientSource = makeClientSource();
            clientSink   = makeClientSink();
            serverSource = makeServerSource();
            serverSink   = makeServerSink();

            relayList.add( new Relay( clientSource, serverSink ));
            relayList.add( new Relay( serverSource, clientSink ));
        } else {
            IncomingSocketQueue prevIncomingSQ = null;
            OutgoingSocketQueue prevOutgoingSQ = null;

            boolean first = true;
            for ( Iterator<ArgonSession> iter = sessionList.iterator(); iter.hasNext() ; ) {
                ArgonSession session = iter.next();

                if ( first ) {
                    /* First one, link in the client sessionEvent */
                    clientSource = makeClientSource();
                    clientSink   = makeClientSink();

                    relayList.add( new Relay( clientSource, session.clientIncomingSocketQueue()));
                    relayList.add( new Relay( session.clientOutgoingSocketQueue(), clientSink ));
                } else {
                    relayList.add( new Relay( prevOutgoingSQ, session.clientIncomingSocketQueue()));
                    relayList.add( new Relay( session.clientOutgoingSocketQueue(), prevIncomingSQ ));
                }

                if ( logger.isDebugEnabled()) {
                    logger.debug( "ArgonHook: buildPipeline - added session: " + session );
                }

                session.argonAgent().addSession( session );

                prevOutgoingSQ = session.serverOutgoingSocketQueue();
                prevIncomingSQ = session.serverIncomingSocketQueue();

                first = false;
            }

            if ( state == ArgonIPNewSessionRequest.REQUESTED ) {
                serverSource = makeServerSource();
                serverSink   = makeServerSink();

                relayList.add( new Relay( prevOutgoingSQ, serverSink ));
                relayList.add( new Relay( serverSource, prevIncomingSQ ));
            } else if ( state == ArgonIPNewSessionRequest.ENDPOINTED ) {
                /* XXX Also have to close the socket queues if the
                 * session is endpointed */
            } else {
            }
        }

        printRelays( relayList );

        vector = new Vector( relayList );
    }

    @SuppressWarnings("fallthrough")
    protected void processSession( ArgonIPNewSessionRequest request, ArgonSession session )
    {
        if ( logger.isDebugEnabled())
            logger.debug( "Processing session: with state: " + request.state() + " session: " + session );

        switch ( request.state()) {
        case ArgonIPNewSessionRequest.RELEASED:
            if ( session == null ) {
                /* Released sessions don't need a session, but for
                 * those that redirects may modify session
                 * parameters */
                break;
            }

            if ( session.isVectored()) {
                throw new IllegalStateException( "Released session trying to vector: " + request.state());
            }

            if ( logger.isDebugEnabled())
                logger.debug( "Adding released session: " + session );


            /* Add to the session list, and then move it in
             * buildPipeline, this way, any modifications to the
             * session will occur in order */
            sessionList.add( session );
            break;

        case ArgonIPNewSessionRequest.ENDPOINTED:
            /* Set the state to endpointed */
            state = ArgonIPNewSessionRequest.ENDPOINTED;

            /* fallthrough */
        case ArgonIPNewSessionRequest.REQUESTED:
            if ( session == null ) {
                throw new IllegalStateException( "Session required for this state: " + request.state());
            }

            if ( logger.isDebugEnabled())
                logger.debug( "Adding session: " + session );

            sessionList.add( session );
            break;

        case ArgonIPNewSessionRequest.REJECTED:
            rejectCode  = request.rejectCode();

            /* fallthrough */
        case ArgonIPNewSessionRequest.REJECTED_SILENT:
            state = request.state();

            /* Done if the session wants to be notified of complete */
            if ( session != null ) sessionList.add( session );
            break;

        default:
            throw new IllegalStateException( "Invalid session state: " + request.state());
        }
    }

    /**
     * Call finalize on each node session that participates in this
     * session, also raze all of the sinks associated with the
     * sessionEvent.  This is just an extra precaution just in case they
     * were not razed by the pipeline.
     */
    private void razeSessions()
    {
        for ( Iterator<ArgonSession> iter = sessionList.iterator() ; iter.hasNext() ; ) {
            ArgonSessionImpl session = (ArgonSessionImpl)iter.next();
            session.raze();
        }

        for ( Iterator<ArgonSession> iter = releasedSessionList.iterator() ; iter.hasNext() ; ) {
            ArgonSessionImpl session = (ArgonSessionImpl)iter.next();
            /* Raze all of the released sessions */
            session.raze();
        }

        if ( clientSource != null ) clientSource.raze();
        if ( clientSink   != null ) clientSink.raze();
        if ( serverSource != null ) serverSource.raze();
        if ( serverSink   != null ) serverSink.raze();
    }

    /**
     * Call this to fake vector a reset before starting vectoring</p>
     * @return True if the reset made it all the way through, false if
     *   a node endpointed.
     */
    private boolean vectorReset()
    {
        logger.debug( "vectorReset: " + state );

        /* No need to vector, the session wasn't even requested */
        if ( state != ArgonIPNewSessionRequest.REQUESTED ) return true;

        int size = sessionList.size();
        boolean isEndpointed = false;
        // Iterate through each session passing the reset.
        ResetCrumb reset = ResetCrumb.getInstanceNotAcked();

        for ( ListIterator<ArgonSession> iter = sessionList.listIterator( size ) ; iter.hasPrevious(); ) {
            ArgonSessionImpl session = (ArgonSessionImpl)iter.previous();

            if ( !session.isVectored()) {
                logger.debug( "vectorReset: skipping non-vectored session" );
                continue;
            }

            session.serverIncomingSocketQueue.send_event( reset );

            /* Make sure the guardian didn't leave a crumb in the queue */
            /* XXX Don't really need to do this */
            while ( !session.serverIncomingSocketQueue.isEmpty()) {
                logger.debug( "vectorReset: Removing crumb left in IncomingSocketQueue:" );
                session.serverIncomingSocketQueue.read();
            }

            /* Indicate that the server is shutdown */
            session.isServerShutdown = true;

            /* Check if they passed the reset */
            if ( session.clientOutgoingSocketQueue.isEmpty()) {
                logger.debug( "vectorReset: ENDPOINTED by " + session );
                isEndpointed = true;
            } else {
                if ( !session.clientOutgoingSocketQueue.containsReset()) {
                    /* Sent data or non-reset, catch this error. */
                    logger.error( "Sent non-reset crumb before vectoring." );
                }

                if ( logger.isDebugEnabled()) {
                    logger.debug( "vectorReset: " + session + " passed reset" );
                }

                session.isClientShutdown = true;
            }
        }

        logger.debug( "vectorReset: isEndpointed - " + isEndpointed );

        return !isEndpointed;
    }

    protected boolean alive()
    {
        if ( state == ArgonIPNewSessionRequest.REQUESTED || state == ArgonIPNewSessionRequest.ENDPOINTED ) {
            return true;
        }

        return false;
    }

    protected void printRelays( List<Relay> relayList )
    {
        if ( logger.isDebugEnabled()) {
            logger.debug( "Relays: " );
            for ( Iterator<Relay>iter = relayList.iterator() ; iter.hasNext() ;) {
                Relay relay = iter.next();
                logger.debug( "" + relay.source() + " --> " + relay.sink());
            }
        }
    }

    /* Get the desired timeout for the vectoring machine */
    protected abstract int  timeout();

    protected abstract NetcapSession netcapSession();

    protected abstract SideListener clientSideListener();
    protected abstract SideListener serverSideListener();

    /**
     * Complete the connection to the server, returning whether or not
     * the connection was succesful.
     * @return - True connection was succesful, false otherwise.
     */
    protected abstract boolean serverComplete();

    /**
     * Complete the connection to the client, returning whether or not the
     * connection was succesful
     * @return - True connection was succesful, false otherwise.
     */
    protected abstract boolean clientComplete();
    protected abstract void clientReject();
    protected abstract void clientRejectSilent();

    protected abstract Sink makeClientSink();
    protected abstract Sink makeServerSink();
    protected abstract Source makeClientSource();
    protected abstract Source makeServerSource();

    protected abstract void newSessionRequest( ArgonAgent agent, Iterator<?> iter, SessionEvent pe );

    protected abstract void raze();

    static void init()
    {
        ArgonSessionImpl.init();
    }
}
