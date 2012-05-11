/*
 * $Id$
 */
package com.untangle.uvm.argon;

import java.util.HashSet;
import java.util.Set;
import java.util.Map;

import org.apache.log4j.Logger;

import com.untangle.uvm.SessionMatcher;

/**
 * The <code>ArgonAgent</code> interface represents an active Node as seen by
 * the node API and the pipeline implementation (Argon).  Most nodes
 * only have one active <code>ArgonAgent</code> at a time, the rest have exactly 2
 * (casings).
 *
 * This class's instances represent and contain the subscription state, pipeline state,
 * and accessors to get the live sessions for the pipe.
 *
 * This class is wrapped inside the view as seen
 * by the node and node API, which is now ArgonConnector.
 */
public class ArgonAgentImpl implements ArgonAgent
{
    protected int state = LIVE_ARGON;
    protected NewSessionEventListener listener = NULL_NEW_SESSION_LISTENER;
    protected Set<ArgonSession> activeSessions = new HashSet<ArgonSession>();
    private final String name; /* to aid debugging */
    private boolean isDestroyed = false;

    /* Debugging */
    private final Logger logger = Logger.getLogger(getClass());

    private static final NewSessionEventListener NULL_NEW_SESSION_LISTENER = new NewSessionEventListener() {
            public ArgonUDPSession newSession( ArgonUDPNewSessionRequest request )
            {
                /* Release everything */
                request.release();
                return null;
            }

            public ArgonTCPSession newSession( ArgonTCPNewSessionRequest request )
            {
                /* Release everything */
                request.release();
                return null;
            }
        };

    public ArgonAgentImpl(String name)
    {
        this.name = name;
    }

    public ArgonAgentImpl(String name, NewSessionEventListener listener )
    {
        this.listener = listener;
        this.name = name;
    }

    /**
     * Returns the pipeline <code>state</code> for this ArgonConnector.  Either LIVE or DEAD.
     * Death may come from <code>destroy</code> or from below.
     *
     * @return an <code>int</code> either LIVE_ARGON or DEAD_ARGON.
     */
    public synchronized int state()
    {
        return state;
    }

    /**
     * Set the state to dead and return the previous state, this is
     * useful in methods that want to kill the state and know what the
     * previous state was, by doing it this was, you don't have to hold the
     * lock while deleting shutting down all of the sessions */
    protected synchronized int deadState()
    {
        int prevState = state;
        state = DEAD_ARGON;
        return prevState;
    }

    /**
     * Deactivates an active ArgonConnector and disconnects it from argon.  This kills
     * all sessions and threads, and keeps any new sessions or further commands
     * from being issued.
     *
     * The ArgonAgent may not be used again.  State will be <code>DEAD_ARGON</code>
     * from here on out.
     *
     */
    public synchronized void destroy()
    {
        /* Session is already dead, no need to do anything */
        if ( deadState() == DEAD_ARGON ) return;

        /* This means DO not remove sessions in raze, they are cleared at the end */
        isDestroyed = true;

        /* Remove the listener */
        listener = NULL_NEW_SESSION_LISTENER;

        /* Create a session matcher to shutdown all active sessions */
        ActiveSessionMatcher matcher = new ActiveSessionMatcher( this.activeSessions );

        ArgonSessionTable.getInstance().shutdownMatches( matcher );

        int numActiveSessions = matcher.getNumberActiveSessions();
        if ( numActiveSessions == 0 ) {
            logger.info( "Shutdown all active sessions" );
        } else {
            logger.warn( "There were " + numActiveSessions + "that where not shutdown." );
        }

        /* Remove all of the active sessions */
        activeSessions.clear();
    }

    public void setNewSessionEventListener(NewSessionEventListener listener)
    {
        this.listener = listener;
    }

    public NewSessionEventListener getNewSessionEventListener()
    {
        return listener;
    }

    public synchronized boolean addSession( ArgonSession session )
    {
        if ( state == DEAD_ARGON ) return false;

        return activeSessions.add( session );
    }

    public synchronized boolean removeSession( ArgonSession session )
    {
        if ( isDestroyed ) return false;

        /* Remove it even if the state is dead */
        return activeSessions.remove( session );
    }

    public String toString()
    {
        return "ArgonAgent[" + name + "]";
    }

    private class ActiveSessionMatcher implements SessionMatcher
    {
        private final Set<Long> activeSessionIds;
        private final Set<Long> shutdownSessionIds;

        private ActiveSessionMatcher( Set<ArgonSession> sessionSet )
        {
            this.activeSessionIds = new HashSet<Long>( sessionSet.size());
            this.shutdownSessionIds = new HashSet<Long>( sessionSet.size());

            for ( ArgonSession session : sessionSet ) this.activeSessionIds.add( session.id());
        }
        /**
         * Tells if the session matches */
        public boolean isMatch( Long policyId, com.untangle.uvm.node.IPSessionDesc clientSide, com.untangle.uvm.node.IPSessionDesc serverSide, Map<String,Object> attachments )
        {
            Long id = clientSide.id();

            /* Get the id from the client side (should match the client side one) */
            if ( this.activeSessionIds.remove( id )) {
                this.shutdownSessionIds.add( id );
                return true;
            } else if ( this.shutdownSessionIds.contains( id )) {
                logger.warn( "session id: [" + id + "] appears to have been shutdown twice." );
                return true;
            }

            return false;
        }

        int getNumberActiveSessions()
        {
            return this.activeSessionIds.size();
        }

    }
}
