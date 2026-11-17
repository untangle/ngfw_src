/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.List;

import org.json.JSONObject;

/**
 * Reviewed callable operations for SafeSessionMonitor.
 */
public interface SafeSessionMonitor
{
    /**
     * getMergedSessions through the reviewed RPC contract.
     * @return The delegated result
     */
    List<SessionMonitorEntry> getMergedSessions();

    /**
     * getPoliciesSessionsStats through the reviewed RPC contract.
     * @return The delegated result
     */
    JSONObject getPoliciesSessionsStats();

    /**
     * getSessionStats through the reviewed RPC contract.
     * @return The delegated result
     */
    JSONObject getSessionStats();
}
