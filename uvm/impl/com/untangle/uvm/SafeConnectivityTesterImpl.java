/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.Objects;
import java.util.function.Supplier;

import org.json.JSONObject;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeConnectivityTesterImpl implements SafeConnectivityTester
{
    private final Supplier<ConnectivityTester> delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeConnectivityTesterImpl(Supplier<ConnectivityTester> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * getStatus through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public JSONObject getStatus()
    {
        return delegate.get().getStatus();
    }
}
