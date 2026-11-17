/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeSettingsManagerImpl implements SafeSettingsManager
{
    private final Supplier<SettingsManager> delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeSettingsManagerImpl(Supplier<SettingsManager> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * getDiff through the reviewed RPC contract.
     * @param fileName The fileName argument
     * @return The delegated result
     * @throws SettingsManager.SettingsException If the operation fails
     */
    @Override
    public String getDiff(String fileName) throws SettingsManager.SettingsException
    {
        return delegate.get().getDiff(fileName);
    }
}
