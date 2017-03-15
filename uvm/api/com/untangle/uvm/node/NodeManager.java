/*
 * $Id$
 */
package com.untangle.uvm.node;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.untangle.uvm.node.NodeManagerSettings;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.AppsView;

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
    List<Node> nodeInstances( Integer policyId );
    List<Long> nodeInstancesIds( Integer policyId );

    /**
     * Node instances by name policy, this gets the nodes in the parents to.
     *
     * @param name name of node.
     * @param policy policy of node.
     * @return tids of corresponding nodes.
     */
    List<Node> nodeInstances( String name, Integer policyId );

    /**
     * Node instances by name policy.
     *
     * @param name name of node.
     * @param policy policy of node.
     * @param parents true to fetch the nodes in the parents as well.
     * @return tids of corresponding nodes.
     */
    List<Node> nodeInstances( String name, Integer policyId, boolean parents );

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
     * @exception Exception if the instance cannot be created.
     */
    Node instantiate( String name, Integer policyId ) throws Exception;

    /**
     * Create a new node instance under the default policy, or in
     * the null policy if the node is a service.
     *
     * @param name of the node.
     * @return the Node of the instance
     * @exception Exception if the instance cannot be created.
     */
    Node instantiate( String name ) throws Exception;

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
     * Example arg: 'firewall'
     */
    boolean isInstantiated( String nodeName );
    
    /**
     * Get the nodeSettings for all nodes in one call.
     *
     * @return a <code>Map</code> from node ID to NodeSettings for all nodes
     */
    Map<Long, NodeSettings> allNodeSettings();

    /**
     * Get the nodeProperties for all installed nodes in one call.
     *
     * @return a <code>Map</code> from node ID to NodeProperties for all installed nodes
     */
    Map<Long, NodeProperties> allNodeProperties();
    
    /**
     * Get the nodeProperties for all nodes in one call.
     *
     * @return a <code>List</code> of NodeProperties for all nodes
     */
    List<NodeProperties> getAllNodeProperties();

    /**
     * Get the view of the apps/rack when the specified policy/rack is displayed
     *
     * @param p policy.
     * @return visible nodes for this policy.
     */
    AppsView getAppsView( Integer policyId );

    /**
     * Get the appsview for all policies
     *
     * @return visible nodes for every policy.
     */
    AppsView[] getAppsViews();

    
}
