/**
 * $Id$
 */
package com.untangle.uvm.vnet;

import java.util.List;

import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.node.Node;

/**
 * The <code>PipelineConnector</code> interface represents an active PipelineConnector.
 * Most nodes only have one active <code>PipelineConnector</code> at a time, the
 * rest have exactly 2 (casings).
 *
 * This class's instances represent and contain the subscription
 * state, pipeline state, and accessors to get the live sessions for
 * the pipe, as well as
 */
public interface PipelineConnector
{
    /**
     * Deactivates an active PipelineConnector and disconnects it from argon.
     * This kills all sessions and threads, and keeps any new sessions
     * or further commands from being issued.
     *
     * The PipelineConnector may not be used again.  State will be
     * <code>DEAD_ARGON</code> from here on out.
     */
    void destroy();

    PipeSpec getPipeSpec();

    long[] liveSessionIds();

    List<NodeSession> liveSessions();
    
    Node node();

    Fitting getInputFitting();

    Fitting getOutputFitting();
}


