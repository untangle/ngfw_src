/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.untangle.uvm.network.DeviceStatus;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.network.InterfaceStatus;
import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.generic.InterfaceStatusGeneric;
import com.untangle.uvm.network.generic.NetworkSettingsGeneric;

/**
 * Reviewed callable operations for SafeNetworkManager.
 */
public interface SafeNetworkManager
{
    /**
     * getAllInterfacesStatusV2 through the reviewed RPC contract.
     * @return The delegated result
     */
    List<InterfaceStatusGeneric> getAllInterfacesStatusV2();

    /**
     * getDeviceStatus through the reviewed RPC contract.
     * @return The delegated result
     */
    List<DeviceStatus> getDeviceStatus();

    /**
     * getEnabledInterfaces through the reviewed RPC contract.
     * @return The delegated result
     */
    List<InterfaceSettings> getEnabledInterfaces();

    /**
     * getInterfaceStatus through the reviewed RPC contract.
     * @return The delegated result
     */
    List<InterfaceStatus> getInterfaceStatus();

    /**
     * getInterfaceStatus through the reviewed RPC contract.
     * @param interfaceId The interfaceId argument
     * @return The delegated result
     */
    InterfaceStatus getInterfaceStatus(int interfaceId);

    /**
     * getInterfaceStatusV2 through the reviewed RPC contract.
     * @param device The device argument
     * @return The delegated result
     */
    InterfaceStatusGeneric getInterfaceStatusV2(String device);

    /**
     * getLogFile through the reviewed RPC contract.
     * @param device The device argument
     * @return The delegated result
     */
    String getLogFile(String device);

    /**
     * getNetworkSettings through the reviewed RPC contract.
     * @return The delegated result
     */
    NetworkSettings getNetworkSettings();

    /**
     * getNetworkSettingsV2 through the reviewed RPC contract.
     * @return The delegated result
     */
    NetworkSettingsGeneric getNetworkSettingsV2();

    /**
     * getPublicUrl through the reviewed RPC contract.
     * @return The delegated result
     */
    String getPublicUrl();

    /**
     * getStatus through the reviewed RPC contract.
     * @param command The command argument
     * @param argument The argument argument
     * @return The delegated result
     */
    String getStatus(NetworkManager.StatusCommands command, String argument);

    /**
     * getWirelessChannels through the reviewed RPC contract.
     * @param systemDev The systemDev argument
     * @param region The region argument
     * @return The delegated result
     */
    JSONArray getWirelessChannels(String systemDev, String region);

    /**
     * getWirelessEncryption through the reviewed RPC contract.
     * @return The delegated result
     */
    InterfaceSettings.WirelessEncryption getWirelessEncryption();

    /**
     * getWirelessPassword through the reviewed RPC contract.
     * @return The delegated result
     */
    String getWirelessPassword();

    /**
     * getWirelessRegulatoryCountryCode through the reviewed RPC contract.
     * @param systemDev The systemDev argument
     * @return The delegated result
     */
    String getWirelessRegulatoryCountryCode(String systemDev);

    /**
     * getWirelessSsid through the reviewed RPC contract.
     * @return The delegated result
     */
    String getWirelessSsid();

    /**
     * getWirelessValidRegulatoryCountryCodes through the reviewed RPC contract.
     * @param systemDev The systemDev argument
     * @return The delegated result
     */
    JSONArray getWirelessValidRegulatoryCountryCodes(String systemDev);

    /**
     * isVrrpMaster through the reviewed RPC contract.
     * @param interfaceId The interfaceId argument
     * @return The delegated result
     */
    boolean isVrrpMaster(int interfaceId);

    /**
     * isWirelessRegulatoryCompliant through the reviewed RPC contract.
     * @param systemDev The systemDev argument
     * @return The delegated result
     */
    boolean isWirelessRegulatoryCompliant(String systemDev);

    /**
     * lookupMacVendorList through the reviewed RPC contract.
     * @param macAddressList The macAddressList argument
     * @return The delegated result
     */
    ConcurrentMap<String, String> lookupMacVendorList(List<String> macAddressList);

    /**
     * renewDhcpLease through the reviewed RPC contract.
     * @param interfaceId The interfaceId argument
     */
    void renewDhcpLease(int interfaceId);

    /**
     * runTroubleshooting through the reviewed RPC contract.
     * @param command The command argument
     * @param arguments The arguments argument
     * @return The delegated result
     */
    SafeExecManagerResultReader runTroubleshooting(NetworkManager.TroubleshootingCommands command, JSONObject arguments);

    /**
     * setNetworkSettings through the reviewed RPC contract.
     * @param newSettings The newSettings argument
     */
    void setNetworkSettings(NetworkSettings newSettings);

    /**
     * setNetworkSettingsV2 through the reviewed RPC contract.
     * @param newSettingsGen The newSettingsGen argument
     */
    void setNetworkSettingsV2(NetworkSettingsGeneric newSettingsGen);

    /**
     * setWirelessSettings through the reviewed RPC contract.
     * @param ssid The ssid argument
     * @param encryption The encryption argument
     * @param password The password argument
     */
    void setWirelessSettings(String ssid, InterfaceSettings.WirelessEncryption encryption, String password);
}
