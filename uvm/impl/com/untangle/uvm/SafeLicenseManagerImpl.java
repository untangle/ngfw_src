/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import com.untangle.uvm.app.License;
import com.untangle.uvm.app.LicenseManager;
import com.untangle.uvm.app.UserLicenseMessage;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeLicenseManagerImpl implements SafeLicenseManager
{
    private final Supplier<LicenseManager> delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeLicenseManagerImpl(Supplier<LicenseManager> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * getLicense through the reviewed RPC contract.
     * @param productIdentifier The productIdentifier argument
     * @return The delegated result
     */
    @Override
    public License getLicense(String productIdentifier)
    {
        return delegate.get().getLicense(productIdentifier);
    }

    /**
     * getLicenseServerConnectivity through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public boolean getLicenseServerConnectivity()
    {
        return delegate.get().getLicenseServerConnectivity();
    }

    /**
     * getLicenses through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public List<License> getLicenses()
    {
        return delegate.get().getLicenses();
    }

    /**
     * getUserLicenseMessages through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public List<UserLicenseMessage> getUserLicenseMessages()
    {
        return delegate.get().getUserLicenseMessages();
    }

    /**
     * isLicenseValid through the reviewed RPC contract.
     * @param productIdentifier The productIdentifier argument
     * @return The delegated result
     */
    @Override
    public boolean isLicenseValid(String productIdentifier)
    {
        return delegate.get().isLicenseValid(productIdentifier);
    }

    /**
     * isRestricted through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public boolean isRestricted()
    {
        return delegate.get().isRestricted();
    }

    /**
     * reloadLicenses through the reviewed RPC contract.
     * @param blocking The blocking argument
     */
    @Override
    public void reloadLicenses(boolean blocking)
    {
        delegate.get().reloadLicenses(blocking);
    }
}
