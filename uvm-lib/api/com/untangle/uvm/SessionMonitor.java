/*
 * $HeadURL: svn://chef/work/src/uvm-lib/impl/com/untangle/uvm/engine/AddressBookFactory.java $
 * Copyright (c) 2003-2010 Untangle, Inc.
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

package com.untangle.uvm;

import java.util.List;

import com.untangle.uvm.vnet.IPSessionDesc;
import com.untangle.uvm.SessionMonitorEntry;
import com.untangle.uvm.security.NodeId;

public interface SessionMonitor
{

    /**
     * This returns a list of descriptors for a certain node
     */
    public List<IPSessionDesc> getNodeSessions(NodeId id);

    /**
     * This returns a list of descriptors for all sessions in the conntrack table
     * It also pulls the list of current "pipelines" from the foundry and adds the UVM informations
     * such as policy
     */
    public List<SessionMonitorEntry> getMergedSessions();

    /**
     * This returns a list of sessions and the bandwidth usage over the last 5 seconds
     * It calls the Jnettop list and merges it with the conntrack and argon lists
     */
    public List<SessionMonitorEntry> getMergedBandwidthSessions();
    public List<SessionMonitorEntry> getMergedBandwidthSessions(String interfaceId);

    
}
