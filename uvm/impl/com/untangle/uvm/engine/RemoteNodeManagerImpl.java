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

package com.untangle.uvm.engine;

import java.util.List;
import java.util.Map;

import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.security.Tid;
import com.untangle.uvm.node.DeployException;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.NodeManager;
import com.untangle.uvm.node.NodeStats;
import com.untangle.uvm.node.UndeployException;

class RemoteNodeManagerImpl implements NodeManager
{
    private final NodeManagerImpl nodeManager;

    RemoteNodeManagerImpl(NodeManagerImpl nodeManager)
    {
        this.nodeManager = nodeManager;
    }

    public List<Tid> nodeInstances()
    {
        return nodeManager.nodeInstances();
    }

    public List<Tid> nodeInstances(String name)
    {
        return nodeManager.nodeInstances(name);
    }

    public List<Tid> nodeInstances(Policy policy)
    {
        return nodeManager.nodeInstances(policy);
    }

    public List<Tid> nodeInstancesVisible(Policy policy)
    {
        return nodeManager.nodeInstancesVisible(policy);
    }

    public List<Tid> nodeInstances(String name, Policy policy)
    {
        return nodeManager.nodeInstances(name, policy);
    }

    public Tid instantiate(String name, Policy policy) throws DeployException
    {
        return nodeManager.instantiate(name, policy);
    }

    public Tid instantiate(String name, Policy policy, String[] args)
        throws DeployException
    {
        return nodeManager.instantiate(name, policy, args);
    }

    public Tid instantiate(String name, String[] args) throws DeployException
    {
        return nodeManager.instantiate(name, args);
    }

    public Tid instantiate(String name) throws DeployException
    {
        return nodeManager.instantiate(name);
    }

    public void destroy(Tid tid) throws UndeployException
    {
        nodeManager.destroy(tid);
    }

    public NodeContext nodeContext(Tid tid)
    {
        return nodeManager.nodeContext(tid);
    }

    public Map<Tid, NodeStats> allNodeStats()
    {
        return nodeManager.allNodeStats();
    }
}
