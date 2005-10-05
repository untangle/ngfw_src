/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.argon;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import com.metavize.jnetcap.NetcapSession;
import com.metavize.jvector.IncomingSocketQueue;
import com.metavize.jvector.OutgoingSocketQueue;
import com.metavize.jvector.Relay;
import com.metavize.jvector.ResetCrumb;
import com.metavize.jvector.Sink;
import com.metavize.jvector.Source;
import com.metavize.jvector.Vector;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.policy.PolicyRule;
import com.metavize.mvvm.tapi.PipelineFoundry;
import org.apache.log4j.Logger;

/**
 * Helper class for the IP session hooks.
 */
abstract class ArgonHook implements Runnable
{
    private static final Logger logger = Logger.getLogger( ArgonHook.class );
    private static final VectronTable activeVectrons = VectronTable.getInstance();

    /* Reject the client with whatever response the server returned */
    protected static final int REJECT_CODE_SRV = -1;

    /**
     * List of all of the transforms( ArgonAgents )
     */
    protected PipelineDesc pipelineDesc;
    protected List pipeline;
    protected List<Session> sessionList = new ArrayList<Session>();
    protected List<Session> releasedSessionList = new ArrayList<Session>();

    protected Source clientSource;
    protected Sink   clientSink;
    protected Source serverSource;
    protected Sink   serverSink;

    protected Vector vector = null;

    protected SessionGlobalState sessionGlobalState;

    protected IPSessionDesc clientSide;
    protected IPSessionDesc serverSide;

    protected static final PipelineFoundry pipelineFoundry = MvvmContextFactory.context().pipelineFoundry();

    /**
     * State of the session
     */
    protected int state      = IPNewSessionRequest.REQUESTED;
    protected int rejectCode = REJECT_CODE_SRV;

    /**
     * Thread hook
     */
    public void run()
    {
        try {
            ClassLoader cl = getClass().getClassLoader();
            Thread.currentThread().setContextClassLoader(cl);

            sessionGlobalState = new SessionGlobalState( netcapSession(), clientSideListener(),
                                                         serverSideListener(), this );

            if ( logger.isDebugEnabled())
                logger.debug( "New thread for session id: " + netcapSession().id() +
                              " " + sessionGlobalState );

            /* Update the server interface with the override table */
            NetcapSession netcapSession = sessionGlobalState.netcapSession();
            InterfaceOverride.getInstance().updateDestinationInterface( netcapSession );
            
            /* If the server interface is still unknown, drop the session */
            byte serverIntf = netcapSession.serverSide().interfaceId();
            byte clientIntf = netcapSession.clientSide().interfaceId();
            if ( IntfConverter.NETCAP_UNKNOWN == serverIntf || 
                 IntfConverter.NETCAP_LOOPBACK == serverIntf ) {
                logger.info( "Session destined to an unknown or local interface[" + serverIntf + 
                             "], destroying" );
                return;
            }
            
            if ( serverIntf == clientIntf ) {
                logger.info( "Session <" + netcapSession +
                             " > has matching client and server interfaces, destroying" );
                return;
            }

            clientSide = new NetcapIPSessionDescImpl( sessionGlobalState, true );

            serverSide = clientSide;

            pipelineDesc = pipelineFoundry.weld( clientSide );
            pipeline = pipelineDesc.getAgents();

            /* Initialize all of the transforms */
            initTransforms();

            /* Connect to the server */
            boolean serverActionCompleted = connectServer();

            /* Now generate the server side since the transforms may have
             * modified the endpoints (we can't do it until we connect to the server since
             * that is what actually modifies the session global state. */
            serverSide = new NetcapIPSessionDescImpl( sessionGlobalState, false );

            /* Connect to the client */
            boolean clientActionCompleted = connectClient();

            if (serverActionCompleted && clientActionCompleted) {
                PolicyRule pr = pipelineDesc.getPolicyRule();
                pipelineFoundry.registerEndpoints( clientSide, serverSide, pr.getPolicy(), pr.isInbound());
            }

            /* Only start vectoring if the session is alive */
            if ( alive()) {
                try {
                    /* Build the pipeline */
                    buildPipeline();

                    /* Insert the vector */
                    activeVectrons.put( vector, clientSide );

                    /* Set the timeout for the vectoring machine */
                    vector.timeout( timeout());

                    if ( logger.isDebugEnabled())
                        logger.debug( "Starting vectoring for session " + sessionGlobalState );

                    /* Start vectoring */
                    vector.vector();

                    /* Call the raze method for each session */
                } catch ( Exception e ) {
                    logger.error( "Exception executing argon hook: " + sessionGlobalState, e );
                }

                if ( logger.isDebugEnabled())
                    logger.debug( "Finished vectoring for session: " + sessionGlobalState );
            } else {
                logger.info( "Session rejected by transform, skipping vectoring: " + sessionGlobalState );
            }

            /* Must raze sessions all sessions in the session list */
            razeSessions();
        } catch ( Exception e ) {
            logger.error( "Exception executing argon hook:", e);
        }

        try {
            /* Let the pipeline foundy know */
            pipelineFoundry.destroy( clientSide, serverSide );

            /* Remove the vector from the vectron table */
            /* You must remove the vector before razing, or else
             * the vector may receive a message(eg shutdown) from another thread */
            activeVectrons.remove( vector );
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

    /**
     * Initialize each of the transforms for the new session. </p>
     */
    protected void initTransforms()
    {
        for ( Iterator<ArgonAgent> iter = pipeline.iterator() ; iter.hasNext() ; ) {
            ArgonAgent agent = iter.next();

            if ( state == IPNewSessionRequest.REQUESTED ) {
                newSessionRequest( agent, iter );
            } else {
                /* Session has been rejected or endpointed, remaining transforms need not be informed */
                // Don't need to remove anything from the pipeline, it is just used here
                // iter.remove();
                break;
            }
        }
    }

    /**
     * Describe <code>connectServer</code> method here.
     *
     * @return a <code>boolean</code> true if the server was completed OR we explicitly rejected
     */
    private boolean connectServer()
    {
        boolean serverActionCompleted = true;
        switch ( state ) {
        case IPNewSessionRequest.REQUESTED:
            /* If the server doesn't complete, we have to "vector" the reset */
            if ( !serverComplete()) {
                /* ??? May want to send different codes, or something ??? */
                if ( vectorReset()) {
                    /* Forward the rejection type that was passed from the server */
                    state        = IPNewSessionRequest.REJECTED;
                    rejectCode = REJECT_CODE_SRV;
                    serverActionCompleted = false;
                } else {
                    state = IPNewSessionRequest.ENDPOINTED;
                }
            }
            break;

            /* Nothing to do on the server side */
        case IPNewSessionRequest.ENDPOINTED: /* fallthrough */
        case IPNewSessionRequest.REJECTED: /* fallthrough */
        case IPNewSessionRequest.REJECTED_SILENT: /* fallthrough */
            break;

        default:
            throw new IllegalStateException( "Invalid state" );

        }
        return serverActionCompleted;
    }

    /**
     * Describe <code>connectClient</code> method here.
     *
     * @return a <code>boolean</code> true if the client was completed OR we explicitly rejected
     */
    private boolean connectClient()
    {
        boolean clientActionCompleted = true;
        boolean createPipelineInfo = true;

        switch ( state ) {
        case IPNewSessionRequest.REQUESTED:
        case IPNewSessionRequest.ENDPOINTED:
            if ( !clientComplete()) {
                logger.info( "Unable to complete connection to client" );
                state = IPNewSessionRequest.REJECTED;
                clientActionCompleted = false;
            }
            break;

        case IPNewSessionRequest.REJECTED:
            logger.debug( "Rejecting session" );
            clientReject();
            break;

        case IPNewSessionRequest.REJECTED_SILENT:
            logger.debug( "Rejecting session silently" );
            clientRejectSilent();
            break;

        default:
            throw new IllegalStateException( "Invalid state" );
        }

        return clientActionCompleted;
    }

    protected void buildPipeline() {
        LinkedList relayList = new LinkedList();

        /* Remove all non-vectored sessions, it is non-efficient to iterate the session
         * list twice, but the list is typically small and this logic may get very complex
         * otherwise */
        for ( Iterator<Session> iter = sessionList.iterator(); iter.hasNext() ; ) {
            Session session = iter.next();
            if ( !session.isVectored()) {
                logger.debug( "Removing non-vectored session from the session list" + session );
                iter.remove();
                /* Append to the released session list */
                releasedSessionList.add( session );
            }
        }

        if ( sessionList.isEmpty() ) {
            if ( state == IPNewSessionRequest.ENDPOINTED ) {
                throw new IllegalStateException( "Endpointed session without any transforms" );
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
            for ( Iterator<Session> iter = sessionList.iterator(); iter.hasNext() ; ) {
                Session session = iter.next();

                if ( first ) {
                    /* First one, link in the client endpoints */
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

            if ( state == IPNewSessionRequest.REQUESTED ) {
                serverSource = makeServerSource();
                serverSink   = makeServerSink();

                relayList.add( new Relay( prevOutgoingSQ, serverSink ));
                relayList.add( new Relay( serverSource, prevIncomingSQ ));
            } else if ( state == IPNewSessionRequest.ENDPOINTED ) {
                /* XXX Also have to close the socket queues if the session is endpointed */
            } else {
            }
        }

        printRelays( relayList );

        vector = new Vector( relayList );
    }

    protected void processSession( IPNewSessionRequest request, Session session )
    {
        if ( logger.isDebugEnabled())
            logger.debug( "Processing session: with state: " + request.state() + " session: " + session );

        switch ( request.state()) {
        case IPNewSessionRequest.RELEASED:
            if ( session == null ) {
                /* Released sessions don't need a session, but for those that redirects may
                 * modify session parameters */
                break;
            }

            if ( session.isVectored()) {
                throw new IllegalStateException( "Released session trying to vector: " + request.state());
            }

            if ( logger.isDebugEnabled())
                logger.debug( "Adding released session: " + session );


            /* Add to the session list, and then move it in buildPipeline,
             * this way, any modifications to the session will occur in order */
            sessionList.add( session );
            break;

        case IPNewSessionRequest.ENDPOINTED:
            /* Set the state to endpointed */
            state = IPNewSessionRequest.ENDPOINTED;

            /* fallthrough */
        case IPNewSessionRequest.REQUESTED:
            if ( session == null ) {
                throw new IllegalStateException( "Session required for this state: " + request.state());
            }

            if ( logger.isDebugEnabled())
                logger.debug( "Adding session: " + session );

            sessionList.add( session );
            break;

        case IPNewSessionRequest.REJECTED:
            rejectCode  = request.rejectCode();

            /* fallthrough */
        case IPNewSessionRequest.REJECTED_SILENT:
            state = request.state();
            break;

        default:
            throw new IllegalStateException( "Invalid session state: " + request.state());
        }
    }

    /**
     * Call finalize on each transform session that participates in this session, also raze
     * all of the sinks associated with the endpoints.  This is just an extra precaution
     * just in case they were not razed by the pipeline.
     */
    private void razeSessions()
    {
        for ( Iterator iter = sessionList.iterator() ; iter.hasNext() ; ) {
            SessionImpl session = (SessionImpl)iter.next();
            session.raze();
        }

        for ( Iterator iter = releasedSessionList.iterator() ; iter.hasNext() ; ) {
            SessionImpl session = (SessionImpl)iter.next();
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
     *   a transform endpointed.
     */
    private boolean vectorReset()
    {
        logger.debug( "vectorReset: " + state );

        /* No need to vector, the session wasn't even requested */
        if ( state != IPNewSessionRequest.REQUESTED ) return true;

        int size = sessionList.size();
        boolean isEndpointed = false;
        // Iterate through each session passing the reset.
        ResetCrumb reset = ResetCrumb.getInstanceNotAcked();

        for ( ListIterator<Session> iter = sessionList.listIterator( size ) ; iter.hasPrevious(); ) {
            SessionImpl session = (SessionImpl)iter.previous();

            if ( !session.isVectored ) {
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
        if ( state == IPNewSessionRequest.REQUESTED || state == IPNewSessionRequest.ENDPOINTED ) {
            return true;
        }

        return false;
    }

    protected void printRelays( List relayList )
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
     * Complete the connection to the server, returning whether or not the
     * connection was succesful
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

    protected abstract void newSessionRequest( ArgonAgent agent, Iterator iter );

    protected abstract void raze();

    static void init()
    {
        SessionImpl.init();
    }
}
