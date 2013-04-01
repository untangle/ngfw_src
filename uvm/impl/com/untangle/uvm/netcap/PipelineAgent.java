/**
 * $Id$
 */
package com.untangle.uvm.netcap;

import java.util.HashSet;
import java.util.Set;
import java.util.Map;

import org.apache.log4j.Logger;

import com.untangle.uvm.node.SessionTuple;
import com.untangle.uvm.vnet.NodeUDPSession;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.UvmContextFactory;

/**
 * The <code>PipelineAgent</code> represents an active Node as seen by
 * the node API and the pipeline implementation (Netcap).  Most nodes
 * only have one active <code>PipelineAgent</code> at a time. Casings have two.
 *
 * This class's instances represent and contain the subscription state, pipeline state,
 * and accessors to get the live sessions for the pipe.
 *
 * This class is wrapped inside the view as seen
 * by the node and node API, which is now PipelineConnector.
 */
public class PipelineAgent
{
    protected boolean live = true;
    protected NewSessionEventListener listener = NULL_NEW_SESSION_LISTENER;
    protected Set<NodeSession> activeSessions = new HashSet<NodeSession>();
    private final String name; /* to aid debugging */

    /* Debugging */
    private final Logger logger = Logger.getLogger(getClass());

    private static final NewSessionEventListener NULL_NEW_SESSION_LISTENER = new NewSessionEventListener() {
            public NodeUDPSession newSession( NetcapUDPNewSessionRequest request )
            {
                /* Release everything */
                request.release();
                return null;
            }

            public NodeTCPSession newSession( NetcapTCPNewSessionRequest request )
            {
                /* Release everything */
                request.release();
                return null;
            }
        };

    public PipelineAgent(String name)
    {
        this.name = name;
    }

    public PipelineAgent(String name, NewSessionEventListener listener )
    {
        this.listener = listener;
        this.name = name;
    }

    /**
     * Deactivates an active PipelineConnector and disconnects it from netcap.  This kills
     * all sessions and threads, and keeps any new sessions or further commands
     * from being issued.
     *
     * State will be <code>DEAD_ARGON</code> from here on out.
     *
     */
    public synchronized void destroy()
    {
        /* NodeSession is already dead, no need to do anything */
        if ( ! this.live ) return;

        live = false;

        /* Remove the listener */
        listener = NULL_NEW_SESSION_LISTENER;

        NetcapSessionTable.getInstance().shutdownActive();

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

    /**
     * Add a session to the map of active sessions.
     * @return True if the session was added, false if the agent is dead, or the session
     *   has already been added.
     */
    public synchronized boolean addSession( NodeSession session )
    {
        if ( ! live ) return false;

        return activeSessions.add( session );
    }

    /**
     * Remove a session from the map of active sessions associated with this netcap agent.
     * @return True if the session was removed, false if the session was not in the list 
     *   of active session.
     */
    public synchronized boolean removeSession( NodeSession session )
    {
        if ( live ) return false;

        return activeSessions.remove( session );
    }

    public String toString()
    {
        return "PipelineAgent[" + name + "]";
    }
}
