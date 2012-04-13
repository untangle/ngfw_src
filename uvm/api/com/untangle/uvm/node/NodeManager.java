/*
 * $Id: NodeManager.java,v 1.00 2011/09/21 11:11:11 dmorris Exp $
 */
package com.untangle.uvm.node;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.untangle.uvm.NodeManagerSettings;
import com.untangle.uvm.NodeSettings;

/**
 * Local interface for managing Node instances.
 */
public interface NodeManager
{
    NodeManagerSettings getSettings();

    void setSettings(NodeManagerSettings newSettings);

    void saveTargetState(Long nodeId, NodeSettings.NodeState nodeState);
    
    /**
     * Get <code>NodeSettings</code>s of nodes in the pipeline.
     *
     * @return list of all node ids.
     */
    List<NodeSettings> nodeInstances();

    /**
     * Node instances by name.
     *
     * @param name name of the node.
     * @return tids of corresponding nodes.
     */
    List<NodeSettings> nodeInstances(String name);

    /**
     * Node instances by policy.
     *
     * @param policy policy of node.
     * @return tids of corresponding nodes.
     */
    List<NodeSettings> nodeInstances(Long policyId);

    /**
     * Node instances by name policy, this gets the nodes in the parents to.
     *
     * @param name name of node.
     * @param policy policy of node.
     * @return tids of corresponding nodes.
     */
    List<NodeSettings> nodeInstances(String name, Long policyId);

    /**
     * Node instances by name policy.
     *
     * @param name name of node.
     * @param policy policy of node.
     * @param parents true to fetch the nodes in the parents as well.
     * @return tids of corresponding nodes.
     */
    List<NodeSettings> nodeInstances(String name, Long policyId, boolean parents);

    /**
     * Create a new node instance under the given policy.  Note
     * that it is an error to specify a non-null policy for a service,
     * or a null policy for a non-service.
     *
     * @param name of the node.
     * @param policy the policy this instance is applied to.
     * @return the <code>tid</code> of the instance.
     * @exception DeployException if the instance cannot be created.
     */
    NodeSettings instantiate(String name, Long policyId) throws DeployException;

    /**
     * Create a new node instance under the default policy, or in
     * the null policy if the node is a service.
     *
     * @param name of the node.
     * @return the <code>tid</code> of the instance.
     * @exception DeployException if the instance cannot be created.
     */
    NodeSettings instantiate(String name) throws DeployException;

    /**
     * Create a new node instance under the given policy and then start it.
     * Note that it is an error to specify a non-null policy for a service,
     * or a null policy for a non-service.
     *
     * @param name of the node.
     * @param policy the policy this instance is applied to.
     * @return the <code>tid</code> of the instance.
     * @exception DeployException if the instance cannot be created.
     * @exception NodeStartException if the instance cannot be started.
     */
    NodeSettings instantiateAndStart(String nodeName, Long policyId) throws DeployException;

    /**
     * Remove node instance from the pipeline.
     *
     * @param nodeId of instance to be destroyed.
     */
    void destroy( Long nodeId ) throws Exception;

    /**
     * Get the <code>NodeContext</code> for a node instance.
     *
     * @param tid <code>NodeSettings</code> of the instance.
     * @return the instance's <code>NodeContext</code>.
     */
    NodeContext nodeContext( NodeSettings nodeSettings );

    /**
     * Get the <code>NodeContext</code> for a node instance.
     * This will replace the nodeContext(NodeSettings) function above eventually when NodeSettings goes away
     *
     * @param the node id (long) of the instance.
     * @return the instance's <code>NodeContext</code>.
     */
    NodeContext nodeContext( Long nodeId );
    
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
     * Get the runtime state for all nodes in one call.
     *
     * @return a <code>Map</code> from NodeSettings to NodeState for all nodes
     */
    Map<NodeSettings, NodeSettings.NodeState> allNodeStates();
    
    /**
     * Get a map of nodes that are enabled for a policy, this takes into account
     * parent / child relationships
     */
    Set<String> getEnabledNodes( Long policyId );
    
    /**
     * Clear out any state related to the nodes in the node manager.
     */
    void flushNodeStateCache();

    /**
     * Returns true if the given node/app is instantiated in the rack
     * false otherwise
     *
     * Example arg: 'untangle-node-reporting'
     */
    boolean isInstantiated( String nodeName );
    
}
