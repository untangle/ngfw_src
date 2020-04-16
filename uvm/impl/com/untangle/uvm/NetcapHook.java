/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.untangle.jnetcap.NetcapSession;
import com.untangle.jvector.Relay;
import com.untangle.jvector.ResetCrumb;
import com.untangle.jvector.Sink;
import com.untangle.jvector.Source;
import com.untangle.jvector.Vector;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.GeographyManager;
import com.untangle.uvm.HostTableEntry;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.app.SessionTuple;
import com.untangle.uvm.app.SessionEvent;
import com.untangle.uvm.app.SessionNatEvent;
import com.untangle.uvm.app.SessionStatsEvent;
import com.untangle.uvm.app.PolicyManager;
import com.untangle.uvm.network.InterfaceSettings;

/**
 * Helper class for the IP session hooks.
 */
public abstract class NetcapHook implements Runnable
{
    private static final Logger logger = Logger.getLogger( NetcapHook.class );

    /* Reject the client with whatever response the server returned */
    protected static final int REJECT_CODE_SRV = -1;

    private static final int TCP_HEADER_SIZE_ESTIMATE = 32;
    private static final int IP_HEADER_SIZE = 20;
    private static final int UDP_HEADER_SIZE = 8;

    private static final SessionTableImpl sessionTable = SessionTableImpl.getInstance();
    private static PolicyManager policyManager = null;

    /**
     * List of all of the apps( PipelineConnectorImpls )
     */
    protected List<PipelineConnectorImpl> pipelineConnectors;
    protected Integer policyId = null;
    protected Integer policyRuleId = null;

    protected List<AppSessionImpl> sessionList = new ArrayList<>();
    protected List<AppSessionImpl> releasedSessionList = new ArrayList<>();

    protected Source clientSource;
    protected Sink   clientSink;
    protected Source serverSource;
    protected Sink   serverSink;

    protected Vector vector = null;

    protected SessionGlobalState sessionGlobalState;

    protected SessionTuple clientSide = null;
    protected SessionTuple serverSide = null;

    protected boolean cleanupSessionOnExit = true;

    protected static final PipelineFoundryImpl pipelineFoundry = (PipelineFoundryImpl)UvmContextFactory.context().pipelineFoundry();

    private static final HookCallback PolicyManagerInstantiatedHook;
    private static final HookCallback PolicyManagerAppDestroyedHook;
    private static Object PolicyManagerSync = new Object();
    private static PolicyManager PolicyManagerApp = null;

    static {
        PolicyManagerInstantiatedHook = new AppInstantiatedHook();
        PolicyManagerAppDestroyedHook = new AppDestroyedHook();
        UvmContextFactory.context().hookManager().registerCallback( HookManager.APPLICATION_INSTANTIATE, PolicyManagerInstantiatedHook );
        UvmContextFactory.context().hookManager().registerCallback( HookManager.APPLICATION_DESTROY, PolicyManagerAppDestroyedHook );
        PolicyManagerApp = (PolicyManager) UvmContextFactory.context().appManager().app("policy-manager");
    }

    /**
     * State of the session
     */
    protected int state      = IPNewSessionRequestImpl.REQUESTED;
    protected int rejectCode = REJECT_CODE_SRV;

    /**
     * getVector
     * @return Vector
     */
    public Vector getVector()
    {
        return this.vector;
    }
    
    /**
     * Thread hook
     */
    public final void run()
    {
        long start = 0;
        SessionEvent sessionEvent = null;
        
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

            this.sessionGlobalState = new SessionGlobalState( netcapSession(), clientSideListener(), serverSideListener(), this );
            boolean serverActionCompleted = false;
            boolean clientActionCompleted = false;
            boolean entitled = true;

            NetcapSession netcapSession = sessionGlobalState.netcapSession();
            int clientIntf = netcapSession.clientSide().interfaceId();
            int serverIntf = netcapSession.serverSide().interfaceId();
            InetAddress clientAddr = netcapSession.clientSide().client().host();
            InetAddress serverAddr = netcapSession.serverSide().server().host();
            long sessionId = sessionGlobalState.id();
            
            if ( logger.isDebugEnabled()) {
                logger.debug( "New thread for session id: " + sessionId + " " + sessionGlobalState );
            }

            if (serverIntf < InterfaceSettings.MIN_INTERFACE_ID || serverIntf > InterfaceSettings.MAX_INTERFACE_ID) {
                logger.warn( "Unknown destination interface: " + netcapSession + ". Killing session." );
                raze();
                return;
            } else {
                netcapSession.setServerIntf(serverIntf);
            }
            sessionGlobalState.setClientIntf( clientIntf );
            sessionGlobalState.setServerIntf( serverIntf );
            
            /**
             * Create the initial tuples based on current information
             */
            clientSide = new SessionTuple( sessionGlobalState.getProtocol(),
                                           netcapSession.clientSide().client().host(),
                                           netcapSession.clientSide().server().host(),
                                           netcapSession.clientSide().client().port(),
                                           netcapSession.clientSide().server().port(),
                                           netcapSession.clientSide().interfaceId(),
                                           netcapSession.serverSide().interfaceId());
            sessionGlobalState.setClientSideTuple( clientSide );
            serverSide = new SessionTuple( sessionGlobalState.getProtocol(),
                                           netcapSession.serverSide().client().host(),
                                           netcapSession.serverSide().server().host(),
                                           netcapSession.serverSide().client().port(),
                                           netcapSession.serverSide().server().port(),
                                           netcapSession.serverSide().interfaceId(),
                                           netcapSession.clientSide().interfaceId());
            sessionGlobalState.setServerSideTuple( clientSide );

            
            HostTableEntry hostEntry = null;
            DeviceTableEntry deviceEntry = null;
            UserTableEntry userEntry = null;
            String username = null;
            String hostname = null;
            
            /**
             * Find Host Table Entry
             */
            if ( hostEntry == null ) {
                /* If the client is a non-wan, use the client's host */
                if ( ! UvmContextFactory.context().networkManager().isWanInterface( clientIntf ) ) {
                    hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( clientAddr, true ); /* create/get host */
                }
                /* Lastly if use the server's host if there is still no host and the server is local */
                if ( hostEntry == null && ! UvmContextFactory.context().networkManager().isWanInterface( serverIntf ) ) {
                    hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( serverAddr, true ); /* create/get host */
                }
            } 
            
            /**
             * If host entry exists
             * Update host entry and also update SessionGlobalState
             */
            if ( hostEntry != null ) {
                hostEntry.setLastSessionTime( System.currentTimeMillis() );
                hostname = hostEntry.getHostname();
                username = hostEntry.getUsername();
                sessionGlobalState.addTags( hostEntry.getTags() );

                // if the local host is creating the session
                // and the source interface does not equal the host's current interface
                // correct it
                if ( clientAddr.equals(hostEntry.getAddress()) && clientIntf != hostEntry.getInterfaceId() ) {
                    hostEntry.setInterfaceId( clientIntf );
                }
                if ( ! hostEntry.getEntitled() ) {
                    entitled = false;
                }
            }

            /**
             * Find Device Table Entry
             */
            if ( hostEntry != null && hostEntry.getMacAddress() != null ) {
                deviceEntry = UvmContextFactory.context().deviceTable().getDevice( hostEntry.getMacAddress() );
            }
            /**
             * If device exists
             * Update device entry and also update SessionGlobalState
             */
            if ( deviceEntry != null ) {
                deviceEntry.setLastSessionTime( System.currentTimeMillis() );
                sessionGlobalState.addTags( deviceEntry.getTags() );
                // if the local host is creating the session
                // and the source interface does not equal the host's current interface
                // correct it
                if ( clientAddr.equals(hostEntry.getAddress()) && clientIntf != deviceEntry.getInterfaceId() ) {
                    deviceEntry.setInterfaceId( clientIntf );
                }
                if ( username == null ) /* if we don't know if from the host entry, use the device entry */
                    username = deviceEntry.getUsername();
                if ( hostname == null ) /* if we don't know if from the host entry, use the device entry */
                    hostname = deviceEntry.getHostname();
            }
            
            /**
             * Find User Table Entry
             */
            if ( username != null ) {
                userEntry = UvmContextFactory.context().userTable().getUserTableEntry( username, true );
            }
            sessionGlobalState.setUser( username );
            /**
             * If user exists
             * Update device entry and also update SessionGlobalState
             */
            if ( userEntry != null ) {
                userEntry.setLastSessionTime( System.currentTimeMillis() );
                sessionGlobalState.addTags( userEntry.getTags() );
            }

            /**
             * If at this point the hostname is not known, determine it by all methods but do not update host table.
             */
            if ( hostname == null || hostname.length() == 0 ) {
                hostname = SessionEvent.determineBestHostname( clientAddr, clientIntf, serverAddr, serverIntf );
            }
            

            /**
             * Determine the policy to process this session
             */
            if(entitled){
                synchronized(PolicyManagerSync){
                    if(PolicyManagerApp != null){
                        PolicyManager.PolicyManagerResult result = PolicyManagerApp.findPolicyId( sessionGlobalState.getProtocol(),
                                                                                       netcapSession.clientSide().interfaceId(), netcapSession.serverSide().interfaceId(),
                                                                                       netcapSession.clientSide().client().host(), netcapSession.serverSide().server().host(),
                                                                                       netcapSession.clientSide().client().port(), netcapSession.serverSide().server().port());
                        this.policyId  = result.policyId;
                        this.policyRuleId  = result.policyRuleId;
                    }
                }
            }
            if ( this.policyId == null )
                this.policyId = 1; /* Default Policy */
            if ( this.policyRuleId == null )
                this.policyRuleId = 0; /* No rule */

            pipelineConnectors = pipelineFoundry.weld( sessionGlobalState.id(), clientSide, policyId, entitled );
            sessionGlobalState.setPipelineConnectorImpls(pipelineConnectors);

            /* Create the sessionEvent early so they can be available at request time. */
            sessionEvent =  new SessionEvent( );
            sessionEvent.setSessionId( sessionGlobalState.id() );
            sessionEvent.setBypassed( false );
            sessionEvent.setEntitled( entitled );
            sessionEvent.setProtocol( sessionGlobalState.getProtocol() );
            sessionEvent.setClientIntf( clientIntf );
            sessionEvent.setServerIntf( serverIntf );
            sessionEvent.setUsername( username );
            sessionEvent.setHostname( hostname );
            sessionEvent.setPolicyId( policyId );
            sessionEvent.setPolicyRuleId( policyRuleId );
            sessionEvent.setCClientAddr( clientSide.getClientAddr() );
            sessionEvent.setCClientPort( clientSide.getClientPort() );
            sessionEvent.setCServerAddr( clientSide.getServerAddr() );
            sessionEvent.setCServerPort( clientSide.getServerPort() );
            sessionEvent.setSClientAddr( serverSide.getClientAddr() );
            sessionEvent.setSClientPort( serverSide.getClientPort() );
            sessionEvent.setSServerAddr( serverSide.getServerAddr() );
            sessionEvent.setSServerPort( serverSide.getServerPort() );
            sessionEvent.setTagsString( sessionGlobalState.getTagsString() );
            
            if ( UvmContextFactory.context().networkManager().isWanInterface( clientIntf ) ) {
                sessionEvent.setLocalAddr( serverSide.getServerAddr() );
                sessionEvent.setRemoteAddr( clientSide.getClientAddr() );
            } else {
                sessionEvent.setLocalAddr( clientSide.getClientAddr() );
                sessionEvent.setRemoteAddr( serverSide.getServerAddr() );
            }

            // lookup the country, latitude, and longitude for WAN clients
            if ( UvmContextFactory.context().networkManager().isWanInterface( clientIntf ) ) {
                GeographyManager.Coordinates clientGeoip = UvmContextFactory.context().geographyManager().getCoordinates(clientSide.getClientAddr().getHostAddress());
                if (clientGeoip != null) {
                    sessionEvent.setClientCountry(clientGeoip.country);
                    sessionEvent.setClientLatitude(clientGeoip.latitude);
                    sessionEvent.setClientLongitude(clientGeoip.longitude);
                } else {
                    sessionEvent.setClientCountry("XU");
                }
            } else {
                sessionEvent.setClientCountry("XL");
            }

            // lookup the country, latitude, and longitude for WAN servers
            if ( UvmContextFactory.context().networkManager().isWanInterface( serverIntf ) ) {
                GeographyManager.Coordinates serverGeoip = UvmContextFactory.context().geographyManager().getCoordinates(serverSide.getServerAddr().getHostAddress());
                if (serverGeoip != null) {
                    sessionEvent.setServerCountry(serverGeoip.country);
                    sessionEvent.setServerLatitude(serverGeoip.latitude);
                    sessionEvent.setServerLongitude(serverGeoip.longitude);
                } else {
                    sessionEvent.setServerCountry("XU");
                }
            } else {
                sessionEvent.setServerCountry("XL");
            }

            sessionGlobalState.setSessionEvent( sessionEvent );

            int tupleHashCodeOriginal =
                sessionEvent.getSClientAddr().hashCode() + 
                sessionEvent.getSClientPort() + 
                sessionEvent.getSServerAddr().hashCode() + 
                sessionEvent.getSServerPort();

            /* log the session event */
            UvmContextFactory.context().logEvent( sessionEvent );

            /* Initialize all of the apps, sending the request events to each in turn */
            initializeAppSessions( sessionEvent );

            int tupleHashCodeNew =
                sessionEvent.getSClientAddr().hashCode() + 
                sessionEvent.getSClientPort() + 
                sessionEvent.getSServerAddr().hashCode() + 
                sessionEvent.getSServerPort();

            /* If any NAT/transformation of the session has taken place, log a NAT event to update the server side attributes */
            if (  tupleHashCodeOriginal != tupleHashCodeNew ) {
                SessionNatEvent natEvent = new SessionNatEvent( sessionEvent,
                                                                serverIntf,
                                                                sessionEvent.getSClientAddr(),
                                                                sessionEvent.getSClientPort(),                                                               
                                                                sessionEvent.getSServerAddr(),
                                                                sessionEvent.getSServerPort());
                UvmContextFactory.context().logEvent(natEvent);
            }
            
            /* Connect to the server */
            serverActionCompleted = connectServerIfNecessary( sessionEvent );

            /* Now generate the server side since the apps may have
             * modified the sessionEvent (we can't do it until we connect
             * to the server since that is what actually modifies the
             * session global state. */
            serverSide = new SessionTuple( sessionGlobalState.getProtocol(),
                                               sessionEvent.getSClientAddr(),
                                               sessionEvent.getSServerAddr(),
                                               sessionEvent.getSClientPort(),
                                               sessionEvent.getSServerPort(),
                                               sessionEvent.getServerIntf(),
                                               sessionEvent.getClientIntf());

            /* Connect to the client */
            clientActionCompleted = connectClientIfNecessary();

            /* Remove all non-vectored sessions, it is non-efficient
             * to iterate the session list twice, but the list is
             * typically small and this logic may get very complex
             * otherwise */
            for ( Iterator<AppSessionImpl> iter = sessionList.iterator(); iter.hasNext() ; ) {
                AppSessionImpl appSession = iter.next();
                if ( !appSession.isVectored() ) {
                    logger.debug( "Removing non-vectored appSession from the appSession list" + appSession );
                    iter.remove();
                    /* Append to the released appSession list */
                    releasedSessionList.add( appSession );
                }

                // Complete (if we completed both server and client)
                if (serverActionCompleted && clientActionCompleted)
                    appSession.complete();
            }

            /* Only start vectoring if the session is alive */
            if ( alive() ) {

                /* if host is not null and this is a TCP session updated host host */
                if ( hostEntry != null && sessionGlobalState.getProtocol() == 6 ) {
                    hostEntry.setLastCompletedTcpSessionTime( System.currentTimeMillis() );
                }

                try {
                    /* Build the pipeline */
                    buildPipeline();

                    /* Insert the vector */
                    sessionTable.put( sessionId, sessionGlobalState );

                    /* Set the timeout for the vectoring machine */
                    vector.timeout( timeout() );

                    if ( logger.isDebugEnabled()) {
                        logger.debug( "Starting vectoring for session " + sessionGlobalState );
                    }
                    /**
                     * If this is UDP and there are only 2 relays, then no apps were interested in
                     * this data. If so, go ahead and initiate the release to bypass
                     * We still continue and call vector() to pass the existing data.
                     */
                    if ( vector.length() == 2 && sessionGlobalState.getProtocol() == 17 ) {
                        releaseToBypass();
                    }
                    
                    //vector.print();
                    vector.vector();

                    /* Call the raze method for each session */
                } catch ( Exception e ) {
                    logger.error( "Exception inside netcap hook: " + sessionGlobalState, e );
                }

                if ( logger.isDebugEnabled())
                    logger.debug( "Finished vectoring for session: " + sessionGlobalState );
            } else {
                logger.debug( "Session rejected, skipping vectoring: " + sessionGlobalState );
            }
        } catch ( Exception e ) {
            String message = e.getMessage();
            if ( message == null )
                message = ""; // some exceptions have null message

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
                logger.warn( "Exception executing netcap hook:", e );
            }
        }

        sessionGlobalState.setEndTime( System.currentTimeMillis() );
        if ( sessionEvent != null )
            logSessionStatsEvent( sessionEvent );

        /**
         * Remove the session from the active sessions table
         * If its TCP, the put it in the recently completed table
         */
        try {
            /**
             * Let the conntrack monitor know this session died if its TCP
             * We do this so that it knows when it sees the conntrack entry in time_wait
             */
            if ( sessionGlobalState.getProtocol() == 6 ) {
                ConntrackMonitorImpl.getInstance().addDeadTcpSession( sessionGlobalState );
            }
            
            /**
             * If cleanupSessionOnExit is true (almost always is)
             * We remove the session from the global session table
             *
             * If it is false we leave the session in the session table
             * This is only done when we release a UDP session back to the kernel
             * In this case, the session is still running, but the vectoring is done
             * As such, we leave the session in the session table to store the
             * metadata, and it will be cleaned up later by the conntrack hook.
             */
            if ( cleanupSessionOnExit )
                sessionTable.remove( sessionGlobalState.id() );
        } catch ( Exception e ) {
            logger.error( "Exception destroying pipeline", e );
        }

        try {
            /* Must raze sessions all sessions in the session list */
            razeSessions();
        } catch ( Exception e ) {
            logger.error( "Exception razing sessions", e );
        }

        try {
            /* Delete the vector */
            if ( vector != null ) {
                vector.raze();
                vector = null;
            }

            /* Delete everything else */
            raze();

            if ( logger.isDebugEnabled()) logger.debug( "Exiting thread: " + sessionGlobalState );
        } catch ( Exception e ) {
            logger.error( "Exception razing vector and session", e );
        }
    }

    /**
     * getClientSide - get the client side tuple
     * @return SessionTuple
     */
    public SessionTuple getClientSide()
    {
        return this.clientSide;
    }

    /**
     * getServerSide - get the server side tuple
     * @return SessionTuple
     */
    public SessionTuple getServerSide()
    {
        return this.serverSide;
    }

    /**
     * getPolicyId - get the policy ID for this session
     * @return int
     */
    public Integer getPolicyId()
    {
        return this.policyId;
    }

    /**
     * setCleanupSessionOnExit
     * @param newValue
     */
    public void setCleanupSessionOnExit( boolean newValue )
    {
        this.cleanupSessionOnExit = newValue;
    }
    
    /**
     * Describe <code>connectServer</code> method here.
     *
     * @param sessionEvent
     * @return a <code>boolean</code> true if the server was completed
     * OR we explicitly rejected
     */
    private boolean connectServerIfNecessary( SessionEvent sessionEvent )
    {
        boolean serverActionCompleted = true;
        switch ( state ) {
        case IPNewSessionRequestImpl.REQUESTED:
            /* If the server doesn't complete, we have to "vector" the reset */
            boolean connected = serverComplete( sessionEvent );
            if ( ! connected ) {
                if ( vectorReset() ) {
                    /* Forward the rejection type that was passed from the server */
                    state        = IPNewSessionRequestImpl.REJECTED;
                    rejectCode = REJECT_CODE_SRV;
                    serverActionCompleted = false;
                } else {
                    state = IPNewSessionRequestImpl.ENDPOINTED;
                }
            }
            break;

            /* Nothing to do on the server side */
        case IPNewSessionRequestImpl.ENDPOINTED: /* fallthrough */
        case IPNewSessionRequestImpl.REJECTED: /* fallthrough */
        case IPNewSessionRequestImpl.REJECTED_SILENT: /* fallthrough */
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
    private boolean connectClientIfNecessary()
    {
        boolean clientActionCompleted = true;

        switch ( state ) {
        case IPNewSessionRequestImpl.REQUESTED:
        case IPNewSessionRequestImpl.ENDPOINTED:
            if ( !clientComplete()) {
                logger.debug( "Unable to complete connection to client" );
                state = IPNewSessionRequestImpl.REJECTED;
                clientActionCompleted = false;
            }
            break;

        case IPNewSessionRequestImpl.REJECTED:
            logger.debug( "Rejecting session" );
            clientReject();
            break;

        case IPNewSessionRequestImpl.REJECTED_SILENT:
            logger.debug( "Rejecting session silently" );
            clientRejectSilent();
            break;

        default:
            throw new IllegalStateException( "Invalid state" );
        }

        return clientActionCompleted;
    }

    /**
     * buildPipeline - builds the actual pipeline (list of relays)
     */
    protected void buildPipeline() 
    {
        LinkedList<Relay> relayList = new LinkedList<>();

        if ( sessionList.isEmpty() ) {
            if ( state == IPNewSessionRequestImpl.ENDPOINTED ) {
                throw new IllegalStateException( "Endpointed session without any apps" );
            }

            clientSource = makeClientSource();
            clientSink   = makeClientSink();
            serverSource = makeServerSource();
            serverSink   = makeServerSink();

            relayList.add( new Relay( clientSource, serverSink ));
            relayList.add( new Relay( serverSource, clientSink ));
        } else {
            Sink   prevSink = null;
            Source prevSource = null;

            boolean first = true;
            AppSessionImpl prevSession = null;
            Iterator<AppSessionImpl> iter = sessionList.iterator();
            do {
                AppSessionImpl session = null;
                try { session = iter.next(); } catch ( Exception e ) {};

                Source source;
                Sink sink;
                
                if ( session != null ) {
                    source = session.clientOutgoingSocketQueue();
                    sink   = session.clientIncomingSocketQueue();
                } else { 
                    // If session is null, we are past the end of the list
                    // as such, wrap things up by using the actual server source/sink
                    source = makeServerSource();
                    sink   = makeServerSink();
                }
                if ( first ) {
                    // If this is the first app, start things with the actual client source/sink
                    prevSource = makeClientSource();
                    prevSink = makeClientSink();
                    first = false;
                }

                Relay c2sInputRelay = new Relay( prevSource, sink );
                Relay s2cOutputRelay = new Relay( source, prevSink );

                relayList.add( c2sInputRelay );
                relayList.add( s2cOutputRelay );

                if ( session == null )
                    break;
                
                prevSource = session.serverOutgoingSocketQueue();
                prevSink = session.serverIncomingSocketQueue();
                prevSession = session;

                if ( logger.isDebugEnabled()) {
                    logger.debug( "NetcapHook: buildPipeline - added session: " + session );
                }

                session.pipelineConnector().addSession( session );
            } while ( true ) ;
        }

        printRelays( relayList );

        vector = new Vector( relayList );
    }

    /**
     * processSession
     * @param request
     * @param session
     */
    @SuppressWarnings("fallthrough")
    protected void processSession( IPNewSessionRequestImpl request, AppSessionImpl session )
    {
        if ( logger.isDebugEnabled())
            logger.debug( "Processing session: with state: " + request.state() + " session: " + session );

        switch ( request.state()) {
        case IPNewSessionRequestImpl.RELEASED:
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

        case IPNewSessionRequestImpl.ENDPOINTED:
            /* Set the state to endpointed */
            state = IPNewSessionRequestImpl.ENDPOINTED;

            /* fallthrough */
        case IPNewSessionRequestImpl.REQUESTED:
            if ( session == null ) {
                throw new IllegalStateException( "Session required for this state: " + request.state());
            }

            if ( logger.isDebugEnabled())
                logger.debug( "Adding session: " + session );

            sessionList.add( session );
            break;

        case IPNewSessionRequestImpl.REJECTED:
            rejectCode  = request.rejectCode();

            /* fallthrough */
        case IPNewSessionRequestImpl.REJECTED_SILENT:
            state = request.state();

            /* Done if the session wants to be notified of complete */
            if ( session != null ) sessionList.add( session );
            break;

        default:
            throw new IllegalStateException( "Invalid session state: " + request.state());
        }
    }

    /**
     * Call finalize on each app session that participates in this
     * session, also raze all of the sinks associated with the
     * sessionEvent.  This is just an extra precaution just in case they
     * were not razed by the pipeline.
     */
    private void razeSessions()
    {
        for ( Iterator<AppSessionImpl> iter = sessionList.iterator() ; iter.hasNext() ; ) {
            AppSessionImpl session = iter.next();
            session.raze();
        }

        for ( Iterator<AppSessionImpl> iter = releasedSessionList.iterator() ; iter.hasNext() ; ) {
            AppSessionImpl session = iter.next();
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
     *   a app endpointed.
     */
    private boolean vectorReset()
    {
        logger.debug( "vectorReset: " + state );

        /* No need to vector, the session wasn't even requested */
        if ( state != IPNewSessionRequestImpl.REQUESTED ) return true;

        int size = sessionList.size();
        boolean isEndpointed = false;
        // Iterate through each session passing the reset.
        ResetCrumb reset = ResetCrumb.getInstanceNotAcked();

        for ( ListIterator<AppSessionImpl> iter = sessionList.listIterator( size ) ; iter.hasPrevious(); ) {
            AppSessionImpl session = iter.previous();

            if ( !session.isVectored()) {
                logger.debug( "vectorReset: skipping non-vectored session" );
                continue;
            }

            session.serverIncomingSocketQueue().send_event( reset );

            /* Empty the server queue */
            while ( session.serverIncomingSocketQueue() != null && !session.serverIncomingSocketQueue().isEmpty() ) {
                logger.debug( "vectorReset: Removing crumb left in IncomingSocketQueue:" );
                session.serverIncomingSocketQueue().read();
            }

            /* Indicate that the server is shutdown */
            session.setServerShutdown(true);

            /* Check if they passed the reset */
            if ( session.clientOutgoingSocketQueue() != null && session.clientOutgoingSocketQueue().isEmpty() ) {

                logger.debug( "vectorReset: ENDPOINTED by " + session );
                isEndpointed = true;
                
            } else {

                if ( session.clientOutgoingSocketQueue() != null && !session.clientOutgoingSocketQueue().containsReset() ) {
                    /* Sent data or non-reset, catch this error. */
                    logger.error( "Sent non-reset crumb before vectoring." );
                }

                if ( logger.isDebugEnabled()) {
                    logger.debug( "vectorReset: " + session + " passed reset" );
                }

                session.setClientShutdown( true );
            }
        }

        logger.debug( "vectorReset: isEndpointed - " + isEndpointed );

        return !isEndpointed;
    }

    /**
     * logSessionStatsEvent - logs the SessionStatsEvent
     * @param sessionEvent
     */
    private void logSessionStatsEvent( SessionEvent sessionEvent )
    {
        try {
            SessionStatsEvent statEvent = new SessionStatsEvent(sessionEvent);
            long c2pBytes = sessionGlobalState.clientSideListener().rxBytes;
            long p2cBytes = sessionGlobalState.clientSideListener().txBytes;
            long c2pChunks = sessionGlobalState.clientSideListener().rxChunks;
            long p2cChunks = sessionGlobalState.clientSideListener().txChunks;

            long s2pBytes = sessionGlobalState.serverSideListener().rxBytes;
            long p2sBytes = sessionGlobalState.serverSideListener().txBytes;
            long s2pChunks = sessionGlobalState.serverSideListener().rxChunks;
            long p2sChunks = sessionGlobalState.serverSideListener().txChunks;

            /**
             * Adjust for packet headers
             */
            if ( sessionGlobalState.getProtocol() == 6 ) {
                c2pBytes = c2pBytes + (c2pChunks * IP_HEADER_SIZE) + (c2pChunks * TCP_HEADER_SIZE_ESTIMATE);
                p2cBytes = p2cBytes + (p2cChunks * IP_HEADER_SIZE) + (p2cChunks * TCP_HEADER_SIZE_ESTIMATE);
                s2pBytes = s2pBytes + (s2pChunks * IP_HEADER_SIZE) + (s2pChunks * TCP_HEADER_SIZE_ESTIMATE);
                p2sBytes = p2sBytes + (p2sChunks * IP_HEADER_SIZE) + (p2sChunks * TCP_HEADER_SIZE_ESTIMATE);
            }
            if ( sessionGlobalState.getProtocol() == 17 ) {
                c2pBytes = c2pBytes + (c2pChunks * IP_HEADER_SIZE) + (c2pChunks * UDP_HEADER_SIZE);
                p2cBytes = p2cBytes + (p2cChunks * IP_HEADER_SIZE) + (p2cChunks * UDP_HEADER_SIZE);
                s2pBytes = s2pBytes + (s2pChunks * IP_HEADER_SIZE) + (s2pChunks * UDP_HEADER_SIZE);
                p2sBytes = p2sBytes + (p2sChunks * IP_HEADER_SIZE) + (p2sChunks * UDP_HEADER_SIZE);
            }
                    
            statEvent.setC2pBytes(c2pBytes);
            statEvent.setP2cBytes(p2cBytes);
            //statEvent.setC2pChunks(sessionGlobalState.clientSideListener().rxChunks);
            //statEvent.setP2cChunks(sessionGlobalState.clientSideListener().txChunks);
            statEvent.setS2pBytes(s2pBytes);
            statEvent.setP2sBytes(p2sBytes);
            //statEvent.setS2pChunks(sessionGlobalState.serverSideListener().rxChunks);
            //statEvent.setP2sChunks(sessionGlobalState.serverSideListener().txChunks);
            statEvent.setEndTime(sessionGlobalState.getEndTime());
                
            UvmContextFactory.context().logEvent( statEvent );
        } catch ( Exception e ) {
            logger.error( "Exception logging session stats.", e );
        }
    }
    
    /**
     * alive - returns true if session is alive, false otherwise
     * @return bool
     */
    protected boolean alive()
    {
        if ( state == IPNewSessionRequestImpl.REQUESTED || state == IPNewSessionRequestImpl.ENDPOINTED ) {
            return true;
        }

        return false;
    }

    /**
     * printRelays - prints relays list/description to debug
     * @param relayList
     */
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

    /**
     * Get the desired timeout for the vectoring machine
     * @return int
     */
    protected abstract int  timeout();

    /**
     * netcapSession
     * @return NetcapSession
     */
    protected abstract NetcapSession netcapSession();

    /**
     * clientSideListener
     * @return SideListener
     */
    protected abstract SideListener clientSideListener();

    /**
     * serverSideListener
     * @return SideListener
     */
    protected abstract SideListener serverSideListener();

    /**
     * Complete the connection to the server, returning whether or not
     * the connection was succesful.
     * @param sessionEvent
     * @return - True connection was succesful, false otherwise.
     */
    protected abstract boolean serverComplete( SessionEvent sessionEvent );

    /**
     * Complete the connection to the client, returning whether or not the
     * connection was succesful
     * @return - True connection was succesful, false otherwise.
     */
    protected abstract boolean clientComplete();

    /**
     * clientReject
     */
    protected abstract void clientReject();

    /**
     * clientRejectSilent
     */
    protected abstract void clientRejectSilent();

    /**
     * makeClientSink
     * @return Sink
     */
    protected abstract Sink makeClientSink();

    /**
     * makeServerSink
     * @return Sink
     */
    protected abstract Sink makeServerSink();

    /**
     * makeClientSource
     * @return Source
     */
    protected abstract Source makeClientSource();

    /**
     * makeServerSource
     * @return Source
     */
    protected abstract Source makeServerSource();

    /**
     * initializeAppSessions
     * @param sessionEvent
     */
    protected abstract void initializeAppSessions( SessionEvent sessionEvent );

    /**
     * raze
     */
    protected abstract void raze();

    /**
     * Release this session back to the kernel.
     * Only supported in UDP
     */
    protected abstract void releaseToBypass();

    /**
     * Hook into application instantiations.
     */
    static private class AppInstantiatedHook implements HookCallback
    {
        /**
        * @return Name of callback hook
        */
        public String getName()
        {
            return "netcap-hook-application-instantiate-hook";
        }

        /**
         * Callback documentation
         *
         * @param args  Args to pass
         */
        public void callback( Object... args )
        {
            String appName = (String) args[0];
            Object app = args[1];

            if(appName == null){
                return;
            }
            if(appName.equals("policy-manager")){
                synchronized(PolicyManagerSync){
                    PolicyManagerApp = (PolicyManager) app;
                }
            }
        }
    }

    /**
     * Hook into application destroys.
     */
    static private class AppDestroyedHook implements HookCallback
    {
        /**
        * @return Name of callback hook
        */
        public String getName()
        {
            return "netcap-hook-application-destroy-hook";
        }

        /**
         * Callback documentation
         *
         * @param args  Args to pass
         */
        public void callback( Object... args )
        {
            String appName = (String) args[0];

            if(appName == null){
                return;
            }
            if(appName.equals("policy-manager")){
                synchronized(PolicyManagerSync){
                    PolicyManagerApp = null;
                }
            }
        }
    }


}
