/*
 * $Id: NodeManager.java,v 1.00 2011/09/21 11:11:11 dmorris Exp $
 */
package com.untangle.uvm.node;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.security.NodeId;

/**
 * Local interface for managing Node instances.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public interface NodeManager
{
    /**
     * Get <code>NodeId</code>s of nodes in the pipeline.
     *
     * @return list of all node ids.
     */
    List<NodeId> nodeInstances();

    /**
     * Node instances by name.
     *
     * @param name name of the node.
     * @return tids of corresponding nodes.
     */
    List<NodeId> nodeInstances(String name);

    /**
     * Node instances by policy.
     *
     * @param policy policy of node.
     * @return tids of corresponding nodes.
     */
    List<NodeId> nodeInstances(Policy policy);

    /**
     * Node instances by policy, the visible ones only, for the GUI.
     *
     * @param policy policy of node.
     * @return <code>NodeDesc</code>s of corresponding nodes.
     */
    List<NodeDesc> visibleNodes(Policy policy);

    /**
     * Node instances by name policy, this gets the nodes in the parents to.
     *
     * @param name name of node.
     * @param policy policy of node.
     * @return tids of corresponding nodes.
     */
    List<NodeId> nodeInstances(String name, Policy policy);

    /**
     * Node instances by name policy.
     *
     * @param name name of node.
     * @param policy policy of node.
     * @param parents true to fetch the nodes in the parents as well.
     * @return tids of corresponding nodes.
     */
    List<NodeId> nodeInstances(String name, Policy policy,boolean parents);

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
    NodeDesc instantiate(String name, Policy policy) throws DeployException;

    /**
     * Create a new node instance under the given policy.  Note
     * that it is an error to specify a non-null policy for a service,
     * or a null policy for a non-service.
     *
     * @param name of the node.
     * @param policy the policy this instance is applied to.
     * @param args node args.
     * @return the <code>tid</code> of the instance.
     * @exception DeployException if the instance cannot be created.
     */
    NodeDesc instantiate(String name, Policy policy, String[] args) throws DeployException;

    /**
     * Create a new node instance under the default policy, or in
     * the null policy if the node is a service.
     *
     * @param name of the node.
     * @param args node args.
     * @return the <code>tid</code> of the instance.
     * @exception DeployException if the instance cannot be created.
     */
    NodeDesc instantiate(String name, String[] args) throws DeployException;

    /**
     * Create a new node instance under the default policy, or in
     * the null policy if the node is a service.
     *
     * @param name of the node.
     * @return the <code>tid</code> of the instance.
     * @exception DeployException if the instance cannot be created.
     */
    NodeDesc instantiate(String name) throws DeployException;

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
    NodeDesc instantiateAndStart(String nodeName, Policy p) throws DeployException;

    /**
     * Remove node instance from the pipeline.
     *
     * @param tid <code>NodeId</code> of instance to be destroyed.
     * @exception UndeployException if detruction fails.
     */
    void destroy(NodeId tid) throws Exception;

    /**
     * Get the <code>NodeContext</code> for a node instance.
     *
     * @param tid <code>NodeId</code> of the instance.
     * @return the instance's <code>NodeContext</code>.
     */
    NodeContext nodeContext(NodeId tid);

    /**
     * Get the <code>Node</code> for a node instance;
     * if the are more than a node instance for the provided name,
     * the first node instance is returned.
     *
     * @param name of the node.
     * @return the instance's <code>Node</code>.
     */
    public Node node(String name);

    /**
     * Get the runtime state for all nodes in one call.
     *
     * @return a <code>Map</code> from NodeId to NodeState for all nodes
     */
    Map<NodeId, NodeState> allNodeStates();
    
    /**
     * Get a map of nodes that are enabled for a policy, this takes into account
     * parent / child relationships
     */
    Set<String> getEnabledNodes(Policy policy);
    
    /**
     * Clear out any state related to the nodes in the node manager.
     */
    void flushNodeStateCache();

    /**
     * Return the NodeContext for the thread.
     *
     * @return the NodeContext for the current thread, or null if
     * none.
     */
    NodeContext threadContext();

    /**
     * Sets the thread's context.
     *
     * @param ctx context to associate with thread.
     */
    void registerThreadContext(NodeContext ctx);

    /**
     * Returns the thread context to the UVM context.
     */
    void deregisterThreadContext();

    /**
     * Returns true if the given node/app is instantiated in the rack
     * false otherwise
     *
     * Example arg: 'untangle-node-reporting'
     */
    boolean isInstantiated(String nodeName);
    
}
