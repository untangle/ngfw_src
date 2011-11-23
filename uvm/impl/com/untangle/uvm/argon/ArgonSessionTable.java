/*
 * $Id$
 */
package com.untangle.uvm.argon;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.untangle.jvector.Vector;
import com.untangle.uvm.SessionMatcher;
import com.untangle.uvm.SessionMatcherFactory;

/**
 * This table stores a global list of all currently active sessions being vectored
 */
public class ArgonSessionTable
{
    /* Debugging */
    private final Logger logger = Logger.getLogger(getClass());
    private static final ArgonSessionTable INSTANCE = new ArgonSessionTable();

    private final Map<Vector,SessionGlobalState> activeSessions = new HashMap<Vector,SessionGlobalState>();

    /* Singleton */
    private ArgonSessionTable() {}

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

    synchronized int count(short protocol)
    {
        int count = 0;
        
        for ( SessionGlobalState state : activeSessions.values() ) {
            if (state.protocol() == protocol)
                count++;
        }

        return count;
    }
    
    /**
     * This kills all active vectors, since this is synchronized, it pauses the creation
     * of new vectoring machines, but it doesn't prevent the creating of new vectoring
     * machines
     * @return - Returns false if there are no active sessions. */
    synchronized boolean shutdownActive()
    {
        if ( activeSessions.isEmpty()) return false;

        for ( Iterator<Vector> iter = activeSessions.keySet().iterator();
              iter.hasNext() ; ) {
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

        logger.debug( "Shutting down matching sessions with: matcher " + matcher );

        if ( activeSessions.isEmpty()) return;

        /* This matcher doesn't match anything */
        if ( matcher == SessionMatcherFactory.getNullInstance()) {
            logger.debug( "NULL Session matcher" );
            return;
        } else if ( matcher == SessionMatcherFactory.getAllInstance()) {
            logger.debug( "ALL Session matcher" );

            /* Just clear all, without checking for matches */
            for ( Iterator<Vector> iter = activeSessions.keySet().iterator() ;
                  iter.hasNext() ; ) {
                Vector vector = iter.next();
                vector.shutdown();
            }
            return;
        }

        /* XXXX THIS IS INCREDIBLY INEFFICIENT AND LOCKS THE CREATION OF NEW SESSIONS */
        for ( Iterator<Map.Entry<Vector,SessionGlobalState>> iter = activeSessions.entrySet().iterator() ; iter.hasNext() ; ) {
            Map.Entry<Vector,SessionGlobalState> e = iter.next();
            boolean isMatch;

            SessionGlobalState session = e.getValue();
            Vector vector  = e.getKey();

            ArgonHook argonHook = session.argonHook();

            isMatch = matcher.isMatch( argonHook.policy, argonHook.clientSide, argonHook.serverSide );

            if ( isDebugEnabled )
                logger.debug( "Tested session: " + session + " id: " + session.id() +
                              " matched: " + isMatch );

            if ( isMatch )
                vector.shutdown();
        }
    }

    public static ArgonSessionTable getInstance()
    {
        return INSTANCE;
    }
}
