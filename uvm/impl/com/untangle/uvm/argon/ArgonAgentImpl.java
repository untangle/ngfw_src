/*
 * $Id$
 */
package com.untangle.uvm.argon;

import java.util.HashSet;
import java.util.Set;
import java.util.Map;

import org.apache.log4j.Logger;

import com.untangle.uvm.node.SessionTuple;
import com.untangle.uvm.UvmContextFactory;

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
    protected boolean live = true;
    protected NewSessionEventListener listener = NULL_NEW_SESSION_LISTENER;
    protected Set<ArgonSession> activeSessions = new HashSet<ArgonSession>();
    private final String name; /* to aid debugging */

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

    public synchronized void destroy()
    {
        /* NodeSession is already dead, no need to do anything */
        if ( ! this.live ) return;

        live = false;

        /* Remove the listener */
        listener = NULL_NEW_SESSION_LISTENER;

        ArgonSessionTable.getInstance().shutdownActive();

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
        if ( ! live ) return false;

        return activeSessions.add( session );
    }

    public synchronized boolean removeSession( ArgonSession session )
    {
        if ( live ) return false;

        return activeSessions.remove( session );
    }

    public String toString()
    {
        return "ArgonAgent[" + name + "]";
    }
}
