/*
 * $Id: SessionMonitor.java,v 1.00 2011/08/17 14:18:11 dmorris Exp $
 */
package com.untangle.uvm;

import java.util.List;

import com.untangle.uvm.vnet.VnetSessionDesc;
import com.untangle.uvm.SessionMonitorEntry;
import com.untangle.uvm.node.NodeSettings;

public interface SessionMonitor
{

    /**
     * This returns a list of descriptors for a certain node
     */
    public List<VnetSessionDesc> getNodeSessions(NodeSettings id);

    /**
     * This returns a list of descriptors for all sessions in the conntrack table
     * It also pulls the list of current "pipelines" from the foundry and adds the UVM informations
     * such as policy
     */
    public List<SessionMonitorEntry> getMergedSessions();

    /**
     * This returns a list of sessions and the bandwidth usage over the last 5 seconds
     * It calls the Jnettop list and merges it with the conntrack and argon lists
     * This takes 5 seconds to return
     */
    public List<SessionMonitorEntry> getMergedBandwidthSessions();

    /**
     * This returns a list of sessions and the bandwidth usage over the last 5 seconds
     * It calls the Jnettop list and merges it with the conntrack and argon lists
     * It calls jnettop on the specified interface Id (example: "0")
     * This takes 5 seconds to return
     */
    public List<SessionMonitorEntry> getMergedBandwidthSessions(String interfaceId);
    
}
