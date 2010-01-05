/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.engine;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.untangle.uvm.node.DeployException;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.NodeDesc;
import com.untangle.uvm.node.NodeStartException;
import com.untangle.uvm.node.NodeState;
import com.untangle.uvm.node.RemoteNodeManager;
import com.untangle.uvm.node.UndeployException;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.security.Tid;

/**
 * Adapts NodeManagerImpl to a RemoteNodeManager.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class RemoteNodeManagerAdaptor implements RemoteNodeManager
{
    private final NodeManagerImpl nodeManager;

    RemoteNodeManagerAdaptor(NodeManagerImpl nodeManager)
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

    public List<NodeDesc> visibleNodes(Policy policy)
    {
        return nodeManager.visibleNodes(policy);
    }

    public List<Tid> nodeInstances(String name, Policy policy)
    {
        return nodeManager.nodeInstances(name, policy);
    }

    public NodeDesc instantiate(String name, Policy policy) throws DeployException
    {
        return nodeManager.instantiate(name, policy);
    }

    public NodeDesc instantiate(String name, Policy policy, String[] args)
        throws DeployException
    {
        return nodeManager.instantiate(name, policy, args);
    }

    public NodeDesc instantiate(String name, String[] args) throws DeployException
    {
        return nodeManager.instantiate(name, args);
    }

    public NodeDesc instantiate(String name) throws DeployException
    {
        return nodeManager.instantiate(name);
    }

    public NodeDesc instantiateAndStart(String nodeName, Policy p) throws DeployException, NodeStartException {
        return nodeManager.instantiateAndStart(nodeName, p);
    }
    
    public void destroy(Tid tid) throws UndeployException
    {
        nodeManager.destroy(tid);
    }

    public NodeContext nodeContext(Tid tid)
    {
        return nodeManager.nodeContext(tid);
    }

    public Node node(String name)
    {
        return nodeManager.node(name);
    }

    public Map<Tid, NodeState> allNodeStates()
    {
        return nodeManager.allNodeStates();
    }

    @Override
    public Set<String> getEnabledNodes(Policy policy) {
        return nodeManager.getEnabledNodes(policy);
    }
    
}
