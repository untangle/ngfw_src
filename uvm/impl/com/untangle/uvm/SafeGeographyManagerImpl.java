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
public final class SafeGeographyManagerImpl implements SafeGeographyManager
{
    private final Supplier<GeographyManager> delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeGeographyManagerImpl(Supplier<GeographyManager> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * getGeoSessionStats through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public JSONObject[] getGeoSessionStats()
    {
        return ((GeographyManagerImpl) delegate.get()).getGeoSessionStats();
    }
}
