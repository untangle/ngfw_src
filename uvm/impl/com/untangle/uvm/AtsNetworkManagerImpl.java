package com.untangle.uvm;

import java.net.InetAddress;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import org.json.JSONArray;
import org.json.JSONObject;

import com.untangle.uvm.NetworkManager.StatusCommands;
import com.untangle.uvm.NetworkManager.TroubleshootingCommands;
import com.untangle.uvm.network.DeviceStatus;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.network.InterfaceSettings.WirelessEncryption;
import com.untangle.uvm.network.InterfaceStatus;
import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.generic.InterfaceStatusGeneric;
import com.untangle.uvm.network.generic.NetworkSettingsGeneric;

/**
 * Composed ATS facade; internal delegates are never exported.
 */
public final class AtsNetworkManagerImpl implements AtsNetworkManager
{
    private final SafeNetworkManager safe;
    private final Supplier<NetworkManager> delegate;

    /**
     * Create the composed ATS facade.
     * @param safe The normal Safe facade
     * @param delegate The internal delegate
     */
    AtsNetworkManagerImpl(SafeNetworkManager safe, Supplier<NetworkManager> delegate)
    {
        this.safe = Objects.requireNonNull(safe, "safe");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<InterfaceStatusGeneric> getAllInterfacesStatusV2()
    {
        return safe.getAllInterfacesStatusV2();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DeviceStatus> getDeviceStatus()
    {
        return safe.getDeviceStatus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<InterfaceSettings> getEnabledInterfaces()
    {
        return safe.getEnabledInterfaces();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<InterfaceStatus> getInterfaceStatus()
    {
        return safe.getInterfaceStatus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InterfaceStatus getInterfaceStatus(int interfaceId)
    {
        return safe.getInterfaceStatus(interfaceId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InterfaceStatusGeneric getInterfaceStatusV2(String device)
    {
        return safe.getInterfaceStatusV2(device);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLogFile(String device)
    {
        return safe.getLogFile(device);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkSettings getNetworkSettings()
    {
        return safe.getNetworkSettings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkSettingsGeneric getNetworkSettingsV2()
    {
        return safe.getNetworkSettingsV2();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPublicUrl()
    {
        return safe.getPublicUrl();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStatus(StatusCommands command, String argument)
    {
        return safe.getStatus(command, argument);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONArray getWirelessChannels(String systemDev, String region)
    {
        return safe.getWirelessChannels(systemDev, region);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WirelessEncryption getWirelessEncryption()
    {
        return safe.getWirelessEncryption();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getWirelessPassword()
    {
        return safe.getWirelessPassword();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getWirelessRegulatoryCountryCode(String systemDev)
    {
        return safe.getWirelessRegulatoryCountryCode(systemDev);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getWirelessSsid()
    {
        return safe.getWirelessSsid();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONArray getWirelessValidRegulatoryCountryCodes(String systemDev)
    {
        return safe.getWirelessValidRegulatoryCountryCodes(systemDev);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isVrrpMaster(int interfaceId)
    {
        return safe.isVrrpMaster(interfaceId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWirelessRegulatoryCompliant(String systemDev)
    {
        return safe.isWirelessRegulatoryCompliant(systemDev);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConcurrentMap<String, String> lookupMacVendorList(List<String> macAddressList)
    {
        return safe.lookupMacVendorList(macAddressList);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void renewDhcpLease(int interfaceId)
    {
        safe.renewDhcpLease(interfaceId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SafeExecManagerResultReader runTroubleshooting(TroubleshootingCommands command, JSONObject arguments)
    {
        return safe.runTroubleshooting(command, arguments);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNetworkSettings(NetworkSettings newSettings)
    {
        safe.setNetworkSettings(newSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNetworkSettingsV2(NetworkSettingsGeneric newSettingsGen)
    {
        safe.setNetworkSettingsV2(newSettingsGen);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setWirelessSettings(String ssid, WirelessEncryption encryption, String password)
    {
        safe.setWirelessSettings(ssid, encryption, password);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InetAddress getFirstWanAddress()
    {
        return delegate.get().getFirstWanAddress();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFullyQualifiedHostname()
    {
        return delegate.get().getFullyQualifiedHostname();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InetAddress getInterfaceHttpAddress(int clientIntf)
    {
        return delegate.get().getInterfaceHttpAddress(clientIntf);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNextFreeInterfaceId(NetworkSettings netSettings)
    {
        return delegate.get().getNextFreeInterfaceId(netSettings);
    }
}
