/*
 * $Id: NodeManager.java,v 1.00 2011/09/21 11:11:11 dmorris Exp $
 */
package com.untangle.uvm.node;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.untangle.uvm.node.NodeManagerSettings;
import com.untangle.uvm.node.NodeSettings;

/**
 * Local interface for managing Node instances.
 */
public interface NodeManager
{
    /**
     * Get the NodeManager settings
     */
    NodeManagerSettings getSettings();

    /**
     * Set the NodeManager settings
     */
    void setSettings( NodeManagerSettings newSettings );

    /**
     * Get <code>Node</code>s of all instantiated nodes.
     *
     * @return list of all node ids.
     */
    List<Node> nodeInstances();
    List<Long> nodeInstancesIds();
    
    /**
     * Node instances by name.
     *
     * @param name name of the node.
     * @return tids of corresponding nodes.
     */
    List<Node> nodeInstances( String name );

    /**
     * Node instances by policy.
     *
     * @param policy policy of node.
     * @return tids of corresponding nodes.
     */
    List<Node> nodeInstances( Long policyId );
    List<Long> nodeInstancesIds( Long policyId );

    /**
     * Node instances by name policy, this gets the nodes in the parents to.
     *
     * @param name name of node.
     * @param policy policy of node.
     * @return tids of corresponding nodes.
     */
    List<Node> nodeInstances( String name, Long policyId );

    /**
     * Node instances by name policy.
     *
     * @param name name of node.
     * @param policy policy of node.
     * @param parents true to fetch the nodes in the parents as well.
     * @return tids of corresponding nodes.
     */
    List<Node> nodeInstances( String name, Long policyId, boolean parents );

    /**
     * Get the <code>Node</code> for this nodeId
     *
     * @param nodeId of the instance.
     * @return the instance's <code>Node</code>.
     */
    Node node( Long nodeId );

    /**
     * Get the <code>Node</code> for a node instance;
     * if the are more than a node instance for the provided name,
     * the first node instance is returned.
     *
     * @param name of the node.
     * @return the instance's <code>Node</code>.
     */
    Node node( String name );
    
    /**
     * Create a new node instance under the given policy.  Note
     * that it is an error to specify a non-null policy for a service,
     * or a null policy for a non-service.
     *
     * @param name of the node.
     * @param policy the policy this instance is applied to.
     * @return the Node of the instance
     * @exception DeployException if the instance cannot be created.
     */
    Node instantiate( String name, Long policyId ) throws DeployException;

    /**
     * Create a new node instance under the default policy, or in
     * the null policy if the node is a service.
     *
     * @param name of the node.
     * @return the Node of the instance
     * @exception DeployException if the instance cannot be created.
     */
    Node instantiate( String name ) throws DeployException;

    /**
     * Create a new node instance under the given policy and then start it.
     * Note that it is an error to specify a non-null policy for a service,
     * or a null policy for a non-service.
     *
     * @param name of the node.
     * @param policy the policy this instance is applied to.
     * @return the Node of the instance
     * @exception DeployException if the instance cannot be created.
     * @exception NodeStartException if the instance cannot be started.
     */
    Node instantiateAndStart( String nodeName, Long policyId ) throws DeployException;

    /**
     * Destroy a node instance.
     *
     * @param nodeId of instance to be destroyed.
     */
    void destroy( Long nodeId ) throws Exception;

    /**
     * Save the new target state of the specified node
     *
     * @param node instance 
     */
    void saveTargetState( Node node, NodeSettings.NodeState nodeState );

    /**
     * Get the runtime state for all nodes in one call.
     *
     * @return a <code>Map</code> from node ID to NodeState for all nodes
     */
    Map<Long, NodeSettings.NodeState> allNodeStates();
    
    /**
     * Returns true if the given node/app is instantiated in the rack
     * false otherwise
     *
     * Example arg: 'untangle-node-reporting'
     */
    boolean isInstantiated( String nodeName );
    
}
