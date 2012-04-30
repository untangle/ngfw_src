/*
 * $Id$
 */
package com.untangle.uvm.node;

import java.util.List;

import com.untangle.uvm.vnet.VnetSessionDesc;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.toolbox.PackageDesc;

/**
 * Interface for a node instance, provides public runtime control
 * methods for manipulating the instance's state.
 */
public interface Node
{
    /**
     * Get the node Settings
     * This returns the generic settings that all nodes share
     * getSettings returns the node-specific settings
     */
    NodeSettings getNodeSettings();

    /**
     * Get the node immutable Properties 
     */
    NodeProperties getNodeProperties();

    /**
     * Get the package desc for this node
     */
    PackageDesc getPackageDesc();
    
    /**
     * Get the current run state of this node
     */
    NodeSettings.NodeState getRunState();

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

    /**
     * Retrieve a list of sessions currently being processed by this node
     */
    List<VnetSessionDesc> liveSessionDescs();

    /**
     * Log an event
     * This is just a convenience method for different parts of the node to log events
     */
    void logEvent(LogEvent evt);

    /**
     * Return statistics tracked for this node (if any)
     */
    List<NodeMetric> getMetrics();
}
