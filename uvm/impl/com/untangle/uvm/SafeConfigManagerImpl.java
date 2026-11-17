/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeConfigManagerImpl implements SafeConfigManager
{
    private final Supplier<ConfigManager> delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeConfigManagerImpl(Supplier<ConfigManager> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * doFactoryReset through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public Object doFactoryReset()
    {
        return delegate.get().doFactoryReset();
    }
}
