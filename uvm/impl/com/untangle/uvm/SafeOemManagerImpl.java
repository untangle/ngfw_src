/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeOemManagerImpl implements SafeOemManager
{
    private final Supplier<OemManager> delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeOemManagerImpl(Supplier<OemManager> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * getOemUrl through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public String getOemUrl()
    {
        return delegate.get().getOemUrl();
    }

    /**
     * getLicenseAgreementUrl through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public String getLicenseAgreementUrl()
    {
        return delegate.get().getLicenseAgreementUrl();
    }
}
