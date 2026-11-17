/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.Objects;
import java.util.function.Supplier;

import org.json.JSONArray;

import com.untangle.uvm.vnet.PipelineFoundry;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafePipelineFoundryImpl implements SafePipelineFoundry
{
    private final Supplier<PipelineFoundry> delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafePipelineFoundryImpl(Supplier<PipelineFoundry> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

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
    @Override
    public JSONArray getPipelineOrder(Integer policyId, short protocol, String clientIp, String serverIp, int clientPort, int serverPort)
    {
        return delegate.get().getPipelineOrder(policyId, protocol, clientIp, serverIp, clientPort, serverPort);
    }
}
