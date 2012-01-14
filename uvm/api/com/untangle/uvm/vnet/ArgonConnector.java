/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.util.List;

import com.untangle.uvm.node.Node;


/**
 * The <code>ArgonConnector</code> interface represents an active ArgonConnector.
 * Most nodes only have one active <code>ArgonConnector</code> at a time, the
 * rest have exactly 2 (casings).
 *
 * This class's instances represent and contain the subscription
 * state, pipeline state, and accessors to get the live sessions for
 * the pipe, as well as
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
public interface ArgonConnector
{
    /**
     * Deactivates an active ArgonConnector and disconnects it from argon.
     * This kills all sessions and threads, and keeps any new sessions
     * or further commands from being issued.
     *
     * The ArgonConnector may not be used again.  State will be
     * <code>DEAD_ARGON</code> from here on out.
     */
    void destroy();

    PipeSpec getPipeSpec();

    long[] liveSessionIds();

    List<VnetSessionDesc> liveSessionDescs();

    List<IPSession> liveSessions();
    
    Node node();
}


