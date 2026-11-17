/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeDeviceTableImpl implements SafeDeviceTable
{
    private final Supplier<DeviceTable> delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeDeviceTableImpl(Supplier<DeviceTable> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * getDevicesSettings through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public DevicesSettings getDevicesSettings()
    {
        return delegate.get().getDevicesSettings();
    }

    /**
     * setDevicesSettings through the reviewed RPC contract.
     * @param newSettings The newSettings argument
     */
    @Override
    public void setDevicesSettings(DevicesSettings newSettings)
    {
        delegate.get().setDevicesSettings(newSettings);
    }
}
