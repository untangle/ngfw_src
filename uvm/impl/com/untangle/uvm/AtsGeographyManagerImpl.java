package com.untangle.uvm;

import java.util.Objects;
import java.util.function.Supplier;

import org.json.JSONObject;

/**
 * Composed ATS facade; internal delegates are never exported.
 */
public final class AtsGeographyManagerImpl implements AtsGeographyManager
{
    private final SafeGeographyManager safe;
    private final Supplier<GeographyManager> delegate;

    /**
     * Create the composed ATS facade.
     * @param safe The normal Safe facade
     * @param delegate The internal delegate
     */
    AtsGeographyManagerImpl(SafeGeographyManager safe, Supplier<GeographyManager> delegate)
    {
        this.safe = Objects.requireNonNull(safe, "safe");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject[] getGeoSessionStats()
    {
        return safe.getGeoSessionStats();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCountryCode(String netAddress)
    {
        return delegate.get().getCountryCode(netAddress);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCountryName(String netAddress)
    {
        return delegate.get().getCountryName(netAddress);
    }
}
