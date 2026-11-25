package com.untangle.uvm;

import java.util.Objects;
import java.util.function.Supplier;

import org.json.JSONArray;

/**
 * Composed ATS facade; internal delegates are never exported.
 */
public final class AtsDeviceTableImpl implements AtsDeviceTable
{
    private final SafeDeviceTable safe;
    private final Supplier<DeviceTable> delegate;

    /**
     * Create the composed ATS facade.
     * @param safe The normal Safe facade
     * @param delegate The internal delegate
     */
    AtsDeviceTableImpl(SafeDeviceTable safe, Supplier<DeviceTable> delegate)
    {
        this.safe = Objects.requireNonNull(safe, "safe");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DevicesSettings getDevicesSettings()
    {
        return safe.getDevicesSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDevicesSettings(DevicesSettings newSettings)
    {
        safe.setDevicesSettings(newSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONArray lookupMacVendor(String macAddress)
    {
        return delegate.get().lookupMacVendor(macAddress);
    }
}
