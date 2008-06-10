/*
 * $HeadURL: svn://chef/branch/prod/web-ui/work/src/uvm-lib/impl/com/untangle/uvm/engine/UvmContextImpl.java $
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.untangle.uvm.logging.BlingerState;
import com.untangle.uvm.logging.Counters;
import com.untangle.uvm.logging.NodeStats;
import com.untangle.uvm.logging.RemoteBlingerManager;
import com.untangle.uvm.node.LocalNodeManager;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.security.Tid;

class BlingerManagerImpl implements RemoteBlingerManager
{
    private LocalNodeManager nodeManager;

    BlingerManagerImpl(LocalNodeManager nodeManager)
    {
        this.nodeManager = nodeManager;
    }

    public BlingerState getBlingerState()
    {
        List<Tid> tids = nodeManager.nodeInstances();
        return getNodeStats(tids);
    }

    public BlingerState getBlingerState(Policy p)
    {
        List<Tid> tids = nodeManager.nodeInstances(p);
        return getNodeStats(tids);
    }

    // private methods ---------------------------------------------------------

    private BlingerState getNodeStats(List<Tid> tids)
    {
        Map<Tid, NodeStats> stats = new HashMap<Tid, NodeStats>(tids.size());

        for (Tid t : tids) {
            Counters c = nodeManager.nodeContext(t).node().getCounters();
            stats.put(t, c.getAllStats());
        }

        return new BlingerState(stats);
    }
}