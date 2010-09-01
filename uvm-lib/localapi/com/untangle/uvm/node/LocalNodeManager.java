/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
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
public interface LocalNodeManager
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
    NodeDesc instantiate(String name, Policy policy, String[] args)
        throws DeployException;

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
    NodeDesc instantiateAndStart(String nodeName, Policy p) throws DeployException, NodeStartException;

    /**
     * Remove node instance from the pipeline.
     *
     * @param tid <code>NodeId</code> of instance to be destroyed.
     * @exception UndeployException if detruction fails.
     */
    void destroy(NodeId tid) throws UndeployException;

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
}
