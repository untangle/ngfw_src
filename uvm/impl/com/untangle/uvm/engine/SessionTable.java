/*
 * $Id$
 */
package com.untangle.uvm.engine;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.untangle.jvector.Vector;
import com.untangle.uvm.SessionMatcher;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.PipelineConnector;

/**
 * This table stores a global list of all currently active sessions being vectored
 */
public class SessionTable
{
    /* Debugging */
    private final Logger logger = Logger.getLogger(getClass());
    private static final SessionTable INSTANCE = new SessionTable();

    private final Map<Vector,SessionGlobalState> activeSessions = new HashMap<Vector,SessionGlobalState>();

    /* Singleton */
    private SessionTable() {}

    /**
     * Add a vector to the hash set.
     * @param  vector - The vector to add.
     * @return - True if the item did not already exist
     */
    synchronized boolean put( Vector vector, SessionGlobalState session )
    {
        return ( activeSessions.put( vector, session ) == null ) ? true : false;
    }

    /**
     * Remove a vector from the hash set.
     * @param  vector - The vector to remove.
     * @return - True if the item was removed, false if it wasn't in the set.
     */
    synchronized boolean remove( Vector vector )
    {
        return ( activeSessions.remove( vector ) == null ) ? false : true;
    }

    /**
     * Get the number of vectors remaining
     */
    synchronized int count()
    {
        return activeSessions.size();
    }

    synchronized int count( short protocol )
    {
        int count = 0;
        
        for ( SessionGlobalState state : activeSessions.values() ) {
            if (state.getProtocol() == protocol)
                count++;
        }

        return count;
    }
    
    /**
     * This kills all active vectors, since this is synchronized, it pauses the creation
     * of new vectoring machines, but it doesn't prevent the creating of new vectoring
     * machines
     * @return - Returns false if there are no active sessions. */
    public synchronized boolean shutdownActive()
    {
        if ( activeSessions.isEmpty()) return false;

        for ( Iterator<Vector> iter = activeSessions.keySet().iterator(); iter.hasNext() ; ) {
            Vector vector = iter.next();
            vector.shutdown();
            /* Don't actually remove the item, it is removed when the session exits */
        }

        return true;
    }

    public synchronized List<SessionGlobalState> getSessions()
    {
        return new LinkedList<SessionGlobalState>(this.activeSessions.values());
    }
    
    synchronized void shutdownMatches( SessionMatcher matcher )
    {
        boolean isDebugEnabled = logger.isDebugEnabled();

        logger.debug( "shutdownMatches() called" );

        if ( activeSessions.isEmpty()) return;

        /**
         * Iterate through all sessions and reset matching sessions
         */
        for ( Iterator<Map.Entry<Vector,SessionGlobalState>> iter = activeSessions.entrySet().iterator() ; iter.hasNext() ; ) {
            Map.Entry<Vector,SessionGlobalState> e = iter.next();
            boolean isMatch;

            SessionGlobalState session = e.getValue();
            Vector vector  = e.getKey();
            NetcapHook netcapHook = session.netcapHook();

            isMatch = matcher.isMatch( netcapHook.policyId, netcapHook.clientSide.getProtocol(),
                                       netcapHook.clientSide.getClientIntf(), netcapHook.serverSide.getServerIntf(),
                                       netcapHook.clientSide.getClientAddr(), netcapHook.serverSide.getServerAddr(),
                                       netcapHook.clientSide.getClientPort(), netcapHook.serverSide.getServerPort(),
                                       session.getAttachments() );

            logger.debug( "shutdownMatches(): Tested    session: " + session + " id: " + session.id() + " matched: " + isMatch );
            if ( isMatch ) {
                logger.info( "shutdownMatches(): Shutdown  session: " + session + " id: " + session.id() + " matched: " + isMatch );
                vector.shutdown();
            }
        }
    }

    synchronized void shutdownMatches( SessionMatcher matcher, PipeSpec ps )
    {
        boolean isDebugEnabled = logger.isDebugEnabled();

        logger.info( "shutdownMatches() called" );

        if ( activeSessions.isEmpty()) return;

        /**
         * Build the list of agents associated with this pipespec
         */
        List<PipelineConnector> pipelineConnectors = ps.getPipelineConnectors();
        
        /**
         * Iterate through all sessions and reset matching sessions
         */
        for ( Iterator<Map.Entry<Vector,SessionGlobalState>> iter = activeSessions.entrySet().iterator() ; iter.hasNext() ; ) {
            Map.Entry<Vector,SessionGlobalState> e = iter.next();
            boolean isMatch;

            SessionGlobalState session = e.getValue();
            Vector vector  = e.getKey();
            NetcapHook netcapHook = session.netcapHook();

            /**
             * Only process sessions involving the specified pipespec and associated connectors
             */
            if (session.getPipelineConnectors() == null)
                continue;
            boolean matchesOne = false;
            for ( PipelineConnector conn : pipelineConnectors )
                if ( session.getPipelineConnectors().contains(conn) )
                    matchesOne = true;
            if (!matchesOne)
                continue;

            isMatch = matcher.isMatch( netcapHook.policyId, netcapHook.clientSide.getProtocol(),
                                       netcapHook.clientSide.getClientIntf(), netcapHook.serverSide.getServerIntf(),
                                       netcapHook.clientSide.getClientAddr(), netcapHook.serverSide.getServerAddr(),
                                       netcapHook.clientSide.getClientPort(), netcapHook.serverSide.getServerPort(),
                                       session.getAttachments() );

            logger.info( "shutdownMatches(): Tested    session: " + session + " id: " + session.id() + " matched: " + isMatch );
            if ( isMatch ) {
                logger.info( "shutdownMatches(): Shutdown  session: " + session + " id: " + session.id() + " matched: " + isMatch );
                vector.shutdown();
            }
        }
    }
    
    public static SessionTable getInstance()
    {
        return INSTANCE;
    }
}
