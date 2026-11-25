package com.untangle.uvm;

import java.util.Objects;
import java.util.function.Supplier;

import org.json.JSONObject;

/**
 * Composed ATS facade; internal delegates are never exported.
 */
public final class AtsMetricManagerImpl implements AtsMetricManager
{
    private final SafeMetricManager safe;
    private final Supplier<MetricManager> delegate;

    /**
     * Create the composed ATS facade.
     * @param safe The normal Safe facade
     * @param delegate The internal delegate
     */
    AtsMetricManagerImpl(SafeMetricManager safe, Supplier<MetricManager> delegate)
    {
        this.safe = Objects.requireNonNull(safe, "safe");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long getMemTotal()
    {
        return safe.getMemTotal();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject getMetricsAndStats()
    {
        return safe.getMetricsAndStats();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject getStats()
    {
        return delegate.get().getStats();
    }
}
