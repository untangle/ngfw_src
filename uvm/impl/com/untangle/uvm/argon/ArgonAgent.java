/**
 * $Id$
 */
package com.untangle.uvm.argon;


/**
 * The <code>ArgonAgent</code> interface represents an active Node as seen by
 * the node API and the pipeline implementation (Argon).  Most nodes
 * only have one active <code>ArgonAgent</code> at a time. Casings have two.
 *
 * This class's instances represent and contain the subscription state, pipeline state,
 * and accessors to get the live sessions for the pipe.
 *
 * This class is wrapped inside the view as seen
 * by the node and node API, which is now ArgonConnector.
 */
public interface ArgonAgent
{
    // States.  Easy for now, just live and destroyed/disconnected/dead.
    public static final int LIVE_ARGON = 1;
    public static final int DEAD_ARGON = 0;

    /**
     * Returns the pipeline <code>state</code> for this ArgonConnector.  Either LIVE or DEAD.
     * Death may come from <code>destroy</code> or from below.
     *
     * @return an <code>int</code> either LIVE_ARGON or DEAD_ARGON.
     */
    int state();

    /**
     * Deactivates an active ArgonConnector and disconnects it from argon.  This kills
     * all sessions and threads, and keeps any new sessions or further commands
     * from being issued.
     *
     * State will be <code>DEAD_ARGON</code> from here on out.
     *
     */
    void destroy();

    void setNewSessionEventListener(NewSessionEventListener listener);

    NewSessionEventListener getNewSessionEventListener();
 
    /**
     * Add a session to the map of active sessions.
     * @return True if the session was added, false if the agent is dead, or the session
     *   has already been added.
     */
    boolean addSession( ArgonSession session );

    /**
     * Remove a session from the map of active sessions associated with this argon agent.
     * @return True if the session was removed, false if the session was not in the list 
     *   of active session.
     */
    boolean removeSession( ArgonSession session );
}


