/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import org.json.JSONArray;

/**
 * Reviewed callable operations for SafePipelineFoundry.
 */
public interface SafePipelineFoundry
{
    /**
     * getPipelineOrder through the reviewed RPC contract.
     * @param policyId The policyId argument
     * @param protocol The protocol argument
     * @param clientIp The clientIp argument
     * @param serverIp The serverIp argument
     * @param clientPort The clientPort argument
     * @param serverPort The serverPort argument
     * @return The delegated result
     */
    JSONArray getPipelineOrder(Integer policyId, short protocol, String clientIp, String serverIp, int clientPort, int serverPort);
}
