/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.uvm.node;

import java.util.List;
import java.util.Map;

import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.security.Tid;

/**
 * Manages node instances in the pipeline.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public interface NodeManager
{
    /**
     * Get <code>Tid</code>s of nodes in the pipeline.
     *
     * @return list of all node ids.
     */
    List<Tid> nodeInstances();

    /**
     * Node instances by name.
     *
     * @param name name of the node.
     * @return tids of corresponding nodes.
     */
    List<Tid> nodeInstances(String name);

    /**
     * Node instances by policy.
     *
     * @param policy policy of node.
     * @return tids of corresponding nodes.
     */
    List<Tid> nodeInstances(Policy policy);

    /**
     * Node instances by policy, the visible ones only, for the GUI.
     *
     * @param policy policy of node.
     * @return tids of corresponding nodes.
     */
    List<Tid> nodeInstancesVisible(Policy policy);

    /**
     * Node instances by name policy.
     *
     * @param name name of node.
     * @param policy policy of node.
     * @return tids of corresponding nodes.
     */
    List<Tid> nodeInstances(String name, Policy policy);

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
    Tid instantiate(String name, Policy policy) throws DeployException;

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
    Tid instantiate(String name, Policy policy, String[] args)
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
    Tid instantiate(String name, String[] args) throws DeployException;

    /**
     * Create a new node instance under the default policy, or in
     * the null policy if the node is a service.
     *
     * @param name of the node.
     * @return the <code>tid</code> of the instance.
     * @exception DeployException if the instance cannot be created.
     */
    Tid instantiate(String name) throws DeployException;

    /**
     * Remove node instance from the pipeline.
     *
     * @param tid <code>Tid</code> of instance to be destroyed.
     * @exception UndeployException if detruction fails.
     */
    void destroy(Tid tid) throws UndeployException;

    /**
     * Get the <code>NodeContext</code> for a node instance.
     *
     * @param tid <code>Tid</code> of the instance.
     * @return the instance's <code>NodeContext</code>.
     */
    NodeContext nodeContext(Tid tid);

    /**
     * Get the statistics and counts for all nodes in one call.
     *
     * @return a <code>Map</code> from Tid to NodeStats for all
     * nodes in RUNNING state.
     */
    Map<Tid, NodeStats> allNodeStats();
}
