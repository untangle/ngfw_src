/*
 * $Id$
 */
package com.untangle.uvm.node;

import java.util.List;

import com.untangle.uvm.security.NodeId;
import com.untangle.uvm.vnet.VnetSessionDesc;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.logging.LogEvent;

/**
 * Interface for a node instance, provides public runtime control
 * methods for manipulating the instance's state.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public interface Node
{
    public NodeId getNodeId();

    NodeState getRunState();

    /**
     * Connects to ArgonConnector and starts. The node instance reads its
     * configuration each time this method is called. A call to this method
     * is only valid when the instance is in the
     * {@link NodeState#INITIALIZED} state. After successful return,
     * the instance will be in the {@link NodeState#RUNNING} state.
     *
     * @exception IllegalStateException if not called in the {@link
     * NodeState#INITIALIZED} state.
     */
    void start() throws Exception;

    /**
     * Stops node and disconnects from the ArgonConnector. A call to
     * this method is only valid when the instance is in the {@link
     * NodeState#RUNNING} state. After successful return, the
     * instance will be in the {@link NodeState#INITIALIZED}
     * state.
     *
     * @exception IllegalStateException if not called in the {@link
     * NodeState#RUNNING} state.
     */
    void stop() throws Exception;

    NodeContext getNodeContext();

    NodeDesc getNodeDesc();

    List<VnetSessionDesc> liveSessionDescs();

    Policy getPolicy();
    
    void logEvent(LogEvent evt);
}
