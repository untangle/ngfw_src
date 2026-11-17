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
public final class SafeMetricManagerImpl implements SafeMetricManager
{
    private final Supplier<MetricManager> delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeMetricManagerImpl(Supplier<MetricManager> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * getMemTotal through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public Long getMemTotal()
    {
        return delegate.get().getMemTotal();
    }

    /**
     * getMetricsAndStats through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public JSONObject getMetricsAndStats()
    {
        return delegate.get().getMetricsAndStats();
    }
}
