/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.json.JSONObject;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeSessionMonitorImpl implements SafeSessionMonitor
{
    private final Supplier<SessionMonitor> delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeSessionMonitorImpl(Supplier<SessionMonitor> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * getMergedSessions through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public List<SessionMonitorEntry> getMergedSessions()
    {
        return delegate.get().getMergedSessions();
    }

    /**
     * getPoliciesSessionsStats through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public JSONObject getPoliciesSessionsStats()
    {
        return delegate.get().getPoliciesSessionsStats();
    }

    /**
     * getSessionStats through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public JSONObject getSessionStats()
    {
        return delegate.get().getSessionStats();
    }
}
