/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeDashboardManagerImpl implements SafeDashboardManager
{
    private final Supplier<DashboardManager> delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeDashboardManagerImpl(Supplier<DashboardManager> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * getSettings through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public DashboardSettings getSettings()
    {
        return delegate.get().getSettings();
    }

    /**
     * resetSettingsToDefault through the reviewed RPC contract.
     */
    @Override
    public void resetSettingsToDefault()
    {
        delegate.get().resetSettingsToDefault();
    }

    /**
     * setSettings through the reviewed RPC contract.
     * @param settings The settings argument
     */
    @Override
    public void setSettings(DashboardSettings settings)
    {
        delegate.get().setSettings(settings);
    }
}
