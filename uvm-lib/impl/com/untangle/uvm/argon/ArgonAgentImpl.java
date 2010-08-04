/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.argon;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.untangle.uvm.localapi.SessionMatcher;
import com.untangle.uvm.policy.Policy;

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
 * by the node and node API, which is now MPipe.
 *
 * @author <a href="mailto:jdi@untangle.com"></a>
 * @version 1.0
 */
public class ArgonAgentImpl implements ArgonAgent {
    protected int state = LIVE_ARGON;
    protected NewSessionEventListener listener = NULL_NEW_SESSION_LISTENER;
    protected Set<Session> activeSessions = new HashSet<Session>();
    private final String name; /* to aid debugging */
    private boolean isDestroyed = false;

    /* Debugging */
    private final Logger logger = Logger.getLogger(getClass());

    private static final NewSessionEventListener NULL_NEW_SESSION_LISTENER = new NewSessionEventListener() {
            public UDPSession newSession( UDPNewSessionRequest request )
            {
                /* Release everything */
                request.release();
                return null;
            }

            public TCPSession newSession( TCPNewSessionRequest request )
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
     * Returns the pipeline <code>state</code> for this MetaPipe.  Either LIVE or DEAD.
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
     * Deactivates an active MetaPipe and disconnects it from argon.  This kills
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

    public synchronized boolean addSession( Session session )
    {
        if ( state == DEAD_ARGON ) return false;

        return activeSessions.add( session );
    }

    public synchronized boolean removeSession( Session session )
    {
        if ( isDestroyed ) return false;

        /* Remove it even if the state is dead */
        return activeSessions.remove( session );
    }

    public String toString()
    {
        return "ArgonAgent[" + name + "]";
    }

    /******************************** PRIVATE ********************************/
    private class ActiveSessionMatcher implements SessionMatcher
    {
        private final Set<Integer> activeSessionIds;
        private final Set<Integer> shtudownSessionIds;

        private ActiveSessionMatcher( Set<Session> sessionSet )
        {
            this.activeSessionIds = new HashSet<Integer>( sessionSet.size());
            this.shtudownSessionIds = new HashSet<Integer>( sessionSet.size());

            for ( Session session : sessionSet ) this.activeSessionIds.add( session.id());
        }
        /**
         * Tells if the session matches */
        public boolean isMatch( Policy policy, com.untangle.uvm.node.IPSessionDesc clientSide,
                                com.untangle.uvm.node.IPSessionDesc serverSide )
        {
            Integer id = clientSide.id();

            /* Get the id from the client side (should match the client side one) */
            if ( this.activeSessionIds.remove( id )) {
                this.shtudownSessionIds.add( id );
                return true;
            } else if ( this.shtudownSessionIds.contains( id )) {
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
