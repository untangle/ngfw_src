/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import org.json.JSONArray;
import org.json.JSONObject;

import com.untangle.uvm.network.DeviceStatus;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.network.InterfaceStatus;
import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.generic.InterfaceStatusGeneric;
import com.untangle.uvm.network.generic.NetworkSettingsGeneric;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeNetworkManagerImpl implements SafeNetworkManager
{
    private final Supplier<NetworkManager> delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeNetworkManagerImpl(Supplier<NetworkManager> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * getAllInterfacesStatusV2 through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public List<InterfaceStatusGeneric> getAllInterfacesStatusV2()
    {
        return delegate.get().getAllInterfacesStatusV2();
    }

    /**
     * getDeviceStatus through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public List<DeviceStatus> getDeviceStatus()
    {
        return delegate.get().getDeviceStatus();
    }

    /**
     * getEnabledInterfaces through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public List<InterfaceSettings> getEnabledInterfaces()
    {
        return delegate.get().getEnabledInterfaces();
    }

    /**
     * getInterfaceStatus through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public List<InterfaceStatus> getInterfaceStatus()
    {
        return delegate.get().getInterfaceStatus();
    }

    /**
     * getInterfaceStatus through the reviewed RPC contract.
     * @param interfaceId The interfaceId argument
     * @return The delegated result
     */
    @Override
    public InterfaceStatus getInterfaceStatus(int interfaceId)
    {
        return delegate.get().getInterfaceStatus(interfaceId);
    }

    /**
     * getInterfaceStatusV2 through the reviewed RPC contract.
     * @param device The device argument
     * @return The delegated result
     */
    @Override
    public InterfaceStatusGeneric getInterfaceStatusV2(String device)
    {
        return delegate.get().getInterfaceStatusV2(device);
    }

    /**
     * getLogFile through the reviewed RPC contract.
     * @param device The device argument
     * @return The delegated result
     */
    @Override
    public String getLogFile(String device)
    {
        return delegate.get().getLogFile(device);
    }

    /**
     * getNetworkSettings through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public NetworkSettings getNetworkSettings()
    {
        return delegate.get().getNetworkSettings();
    }

    /**
     * getNetworkSettingsV2 through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public NetworkSettingsGeneric getNetworkSettingsV2()
    {
        return delegate.get().getNetworkSettingsV2();
    }

    /**
     * getPublicUrl through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public String getPublicUrl()
    {
        return delegate.get().getPublicUrl();
    }

    /**
     * getStatus through the reviewed RPC contract.
     * @param command The command argument
     * @param argument The argument argument
     * @return The delegated result
     */
    @Override
    public String getStatus(NetworkManager.StatusCommands command, String argument)
    {
        return delegate.get().getStatus(command, argument);
    }

    /**
     * getWirelessChannels through the reviewed RPC contract.
     * @param systemDev The systemDev argument
     * @param region The region argument
     * @return The delegated result
     */
    @Override
    public JSONArray getWirelessChannels(String systemDev, String region)
    {
        return delegate.get().getWirelessChannels(systemDev, region);
    }

    /**
     * getWirelessEncryption through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public InterfaceSettings.WirelessEncryption getWirelessEncryption()
    {
        return ((NetworkManagerImpl) delegate.get()).getWirelessEncryption();
    }

    /**
     * getWirelessPassword through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public String getWirelessPassword()
    {
        return ((NetworkManagerImpl) delegate.get()).getWirelessPassword();
    }

    /**
     * getWirelessRegulatoryCountryCode through the reviewed RPC contract.
     * @param systemDev The systemDev argument
     * @return The delegated result
     */
    @Override
    public String getWirelessRegulatoryCountryCode(String systemDev)
    {
        return delegate.get().getWirelessRegulatoryCountryCode(systemDev);
    }

    /**
     * getWirelessSsid through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public String getWirelessSsid()
    {
        return ((NetworkManagerImpl) delegate.get()).getWirelessSsid();
    }

    /**
     * getWirelessValidRegulatoryCountryCodes through the reviewed RPC contract.
     * @param systemDev The systemDev argument
     * @return The delegated result
     */
    @Override
    public JSONArray getWirelessValidRegulatoryCountryCodes(String systemDev)
    {
        return delegate.get().getWirelessValidRegulatoryCountryCodes(systemDev);
    }

    /**
     * isVrrpMaster through the reviewed RPC contract.
     * @param interfaceId The interfaceId argument
     * @return The delegated result
     */
    @Override
    public boolean isVrrpMaster(int interfaceId)
    {
        return delegate.get().isVrrpMaster(interfaceId);
    }

    /**
     * isWirelessRegulatoryCompliant through the reviewed RPC contract.
     * @param systemDev The systemDev argument
     * @return The delegated result
     */
    @Override
    public boolean isWirelessRegulatoryCompliant(String systemDev)
    {
        return delegate.get().isWirelessRegulatoryCompliant(systemDev);
    }

    /**
     * lookupMacVendorList through the reviewed RPC contract.
     * @param macAddressList The macAddressList argument
     * @return The delegated result
     */
    @Override
    public ConcurrentMap<String, String> lookupMacVendorList(List<String> macAddressList)
    {
        return delegate.get().lookupMacVendorList(macAddressList);
    }

    /**
     * renewDhcpLease through the reviewed RPC contract.
     * @param interfaceId The interfaceId argument
     */
    @Override
    public void renewDhcpLease(int interfaceId)
    {
        delegate.get().renewDhcpLease(interfaceId);
    }

    /**
     * runTroubleshooting through the reviewed RPC contract.
     * @param command The command argument
     * @param arguments The arguments argument
     * @return The delegated result
     */
    @Override
    public SafeExecManagerResultReader runTroubleshooting(NetworkManager.TroubleshootingCommands command, JSONObject arguments)
    {
        ExecManagerResultReader reader = delegate.get().runTroubleshooting(command, arguments);
        return reader == null ? null : new SafeExecManagerResultReaderImpl(reader);
    }

    /**
     * setNetworkSettings through the reviewed RPC contract.
     * @param newSettings The newSettings argument
     */
    @Override
    public void setNetworkSettings(NetworkSettings newSettings)
    {
        delegate.get().setNetworkSettings(newSettings);
    }

    /**
     * setNetworkSettingsV2 through the reviewed RPC contract.
     * @param newSettingsGen The newSettingsGen argument
     */
    @Override
    public void setNetworkSettingsV2(NetworkSettingsGeneric newSettingsGen)
    {
        delegate.get().setNetworkSettingsV2(newSettingsGen);
    }

    /**
     * setWirelessSettings through the reviewed RPC contract.
     * @param ssid The ssid argument
     * @param encryption The encryption argument
     * @param password The password argument
     */
    @Override
    public void setWirelessSettings(String ssid, InterfaceSettings.WirelessEncryption encryption, String password)
    {
        ((NetworkManagerImpl) delegate.get()).setWirelessSettings(ssid, encryption, password);
    }
}
