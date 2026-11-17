/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.net.InetAddress;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeAdminManagerImpl implements SafeAdminManager
{
    private final Supplier<AdminManager> delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeAdminManagerImpl(Supplier<AdminManager> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * getKernelVersion through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public String getKernelVersion()
    {
        return delegate.get().getKernelVersion();
    }

    /**
     * getModificationState through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public String getModificationState()
    {
        return delegate.get().getModificationState();
    }

    /**
     * getRebootCount through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public String getRebootCount()
    {
        return delegate.get().getRebootCount();
    }

    /**
     * getSettings through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public AdminSettings getSettings()
    {
        return delegate.get().getSettings();
    }

    /**
     * getSettingsV2 through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public AdminSettings getSettingsV2()
    {
        return delegate.get().getSettingsV2();
    }

    /**
     * logAdminLoginEvent through the reviewed RPC contract.
     * @param login The login argument
     * @param local The local argument
     * @param clientAddress The clientAddress argument
     * @param succeeded The succeeded argument
     * @param reason The reason argument
     */
    @Override
    public void logAdminLoginEvent(String login, boolean local, InetAddress clientAddress, boolean succeeded, String reason)
    {
        delegate.get().logAdminLoginEvent(login, local, clientAddress, succeeded, reason);
    }

    /**
     * setSettings through the reviewed RPC contract.
     * @param settings The settings argument
     */
    @Override
    public void setSettings(AdminSettings settings)
    {
        delegate.get().setSettings(settings);
    }

    /**
     * setSettingsV2 through the reviewed RPC contract.
     * @param settings The settings argument
     */
    @Override
    public void setSettingsV2(AdminSettings settings)
    {
        delegate.get().setSettingsV2(settings);
    }
}
